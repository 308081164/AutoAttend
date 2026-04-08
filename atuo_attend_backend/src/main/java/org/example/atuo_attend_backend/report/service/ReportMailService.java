package org.example.atuo_attend_backend.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.atuo_attend_backend.ai.domain.ProjectDailySummary;
import org.example.atuo_attend_backend.ai.mapper.ProjectDailySummaryMapper;
import org.example.atuo_attend_backend.ai.service.ProjectDailySummaryService;
import org.example.atuo_attend_backend.commit.CommitRecord;
import org.example.atuo_attend_backend.commit.CommitService;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.example.atuo_attend_backend.report.domain.ProjectReportConfig;
import org.example.atuo_attend_backend.report.domain.ReportConfig;
import org.example.atuo_attend_backend.report.domain.ReportRecipient;
import org.example.atuo_attend_backend.report.mapper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * 报告邮件服务：项目日报（基于 ProjectDailySummary）+ 开发者个人日报（基于 aa_commit 聚合）。
 *
 * 说明：
 * - 发信 SMTP 配置来自 SystemConfigService（由「API 配置与能力测试」维护）。
 * - 项目级配置来自 aa_project_report_config。
 */
@Service
public class ReportMailService {

    private static final Logger log = LoggerFactory.getLogger(ReportMailService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ReportConfigMapper reportConfigMapper;
    private final ProjectReportConfigMapper projectReportConfigMapper;
    private final ReportRecipientMapper recipientMapper;
    private final ReportBlacklistMapper blacklistMapper;
    private final ReportSendLogMapper sendLogMapper;
    private final ProjectDailySummaryMapper projectDailySummaryMapper;
    private final ProjectDailySummaryService projectDailySummaryService;
    private final CommitService commitService;
    private final MailSenderService mailSenderService;
    private final SystemConfigService systemConfigService;

    public ReportMailService(ReportConfigMapper reportConfigMapper,
                             ProjectReportConfigMapper projectReportConfigMapper,
                             ReportRecipientMapper recipientMapper,
                             ReportBlacklistMapper blacklistMapper,
                             ReportSendLogMapper sendLogMapper,
                             ProjectDailySummaryMapper projectDailySummaryMapper,
                             ProjectDailySummaryService projectDailySummaryService,
                             CommitService commitService,
                             MailSenderService mailSenderService,
                             SystemConfigService systemConfigService) {
        this.reportConfigMapper = reportConfigMapper;
        this.projectReportConfigMapper = projectReportConfigMapper;
        this.recipientMapper = recipientMapper;
        this.blacklistMapper = blacklistMapper;
        this.sendLogMapper = sendLogMapper;
        this.projectDailySummaryMapper = projectDailySummaryMapper;
        this.projectDailySummaryService = projectDailySummaryService;
        this.commitService = commitService;
        this.mailSenderService = mailSenderService;
        this.systemConfigService = systemConfigService;
    }

    public boolean isMailConfigured() {
        return mailSenderService.isConfigured();
    }

    public int sendForYesterday(ZoneId zone) {
        LocalDate date = LocalDate.now(zone).minusDays(1);
        return sendForDate(date, zone);
    }

    /**
     * 发送指定业务日的日报邮件。返回成功发送的邮件数（发送失败也会写 log）。
     */
    public int sendForDate(LocalDate reportDate, ZoneId zone) {
        if (reportDate == null) return 0;

        ReportConfig cfg = ensureReportConfigRow();
        if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled())) {
            log.info("Report mail skipped: report_config.enabled=false");
            return 0;
        }
        if (!isMailConfigured()) {
            log.warn("Report mail skipped: SMTP not configured");
            return 0;
        }

        OffsetDateTime start = reportDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = reportDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        // 先确保项目每日总结存在（若 AI 未启用，则服务内部会返回 0；这里不强制）
        try {
            projectDailySummaryService.runSummariesForDate(reportDate, null, true);
        } catch (Exception e) {
            log.warn("Run daily summaries before mail failed: {}", e.getMessage());
        }

        Set<String> blacklist = loadBlacklistEmails();
        int ok = 0;
        List<ProjectReportConfig> projects = projectReportConfigMapper.listEnabled();
        for (ProjectReportConfig p : projects) {
            if (p == null) continue;
            boolean sendManagers = Boolean.TRUE.equals(p.getSendToManagers());
            boolean sendDevs = Boolean.TRUE.equals(p.getSendToDevelopers());
            if (!sendManagers && !sendDevs) continue;

            String repo = p.getRepoFullName() != null ? p.getRepoFullName().trim() : "";
            Long projectId = p.getProjectId();

            Set<String> managerEmails = sendManagers ? parseEmailsJson(p.getManagerEmails()) : Set.of();
            Set<String> managerTargets = filterTargets(managerEmails, blacklist);
            if (!managerTargets.isEmpty()) {
                for (String to : managerTargets) {
                    String token = ensureRecipientAndGetToken(to);
                    String subject = buildProjectSubject(cfg, repo, reportDate);
                    String html = renderProjectReportHtml(cfg, repo, reportDate, token);
                    ok += sendOne(to, subject, html, reportDate, "project", projectId, repo);
                }
            }

            if (sendDevs && repo != null && !repo.isBlank()) {
                List<String> devEmails = commitService.listDistinctAuthorEmailsByRepoBetween(repo, start, end);
                Set<String> devTargets = filterTargets(new HashSet<>(devEmails), blacklist);
                // 个人日报：按人生成内容（不把项目日报一股脑发给开发者）
                for (String to : devTargets) {
                    String token = ensureRecipientAndGetToken(to);
                    String subject = buildDeveloperSubject(cfg, repo, reportDate);
                    String html = renderDeveloperReportHtml(cfg, to, reportDate, zone, start, end, repo, token);
                    ok += sendOne(to, subject, html, reportDate, "developer", projectId, repo);
                }
            }
        }
        return ok;
    }

    /** 仅发送单个项目（用于项目页“发送测试邮件”）。 */
    public int sendForProject(long projectId, LocalDate reportDate, ZoneId zone, boolean includeDevelopers) {
        if (reportDate == null) return 0;
        ReportConfig cfg = ensureReportConfigRow();
        if (cfg == null || !Boolean.TRUE.equals(cfg.getEnabled())) return 0;
        if (!isMailConfigured()) return 0;

        ProjectReportConfig p = projectReportConfigMapper.findByProjectId(projectId);
        if (p == null || !Boolean.TRUE.equals(p.getEnabled())) return 0;

        OffsetDateTime start = reportDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime end = reportDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        Set<String> blacklist = loadBlacklistEmails();

        int ok = 0;
        String repo = p.getRepoFullName() != null ? p.getRepoFullName().trim() : "";
        Set<String> managerEmails = Boolean.TRUE.equals(p.getSendToManagers()) ? parseEmailsJson(p.getManagerEmails()) : Set.of();
        Set<String> managerTargets = filterTargets(managerEmails, blacklist);
        for (String to : managerTargets) {
            String token = ensureRecipientAndGetToken(to);
            String subject = buildProjectSubject(cfg, repo, reportDate);
            String html = renderProjectReportHtml(cfg, repo, reportDate, token);
            ok += sendOne(to, subject, html, reportDate, "project", p.getProjectId(), repo);
        }

        if (includeDevelopers && Boolean.TRUE.equals(p.getSendToDevelopers()) && repo != null && !repo.isBlank()) {
            List<String> devEmails = commitService.listDistinctAuthorEmailsByRepoBetween(repo, start, end);
            Set<String> devTargets = filterTargets(new HashSet<>(devEmails), blacklist);
            for (String to : devTargets) {
                String token = ensureRecipientAndGetToken(to);
                String subject = buildDeveloperSubject(cfg, repo, reportDate);
                String html = renderDeveloperReportHtml(cfg, to, reportDate, zone, start, end, repo, token);
                ok += sendOne(to, subject, html, reportDate, "developer", p.getProjectId(), repo);
            }
        }
        return ok;
    }

    /** 预览项目日报 HTML（不发信）。 */
    public String previewProjectHtml(long projectId, LocalDate reportDate, ZoneId zone) {
        if (reportDate == null) return "";
        ReportConfig cfg = ensureReportConfigRow();
        ProjectReportConfig p = projectReportConfigMapper.findByProjectId(projectId);
        String repo = p != null ? Objects.toString(p.getRepoFullName(), "") : "";
        // 预览时也尽量生成一次总结
        try {
            projectDailySummaryService.runSummariesForDate(reportDate, repo, false);
        } catch (Exception ignore) {}
        return renderProjectReportHtml(cfg, repo, reportDate, "preview");
    }

    private int sendOne(String to, String subject, String html, LocalDate reportDate, String type, Long projectId, String repo) {
        try {
            mailSenderService.sendHtml(to, subject, html);
            sendLogMapper.insert(to, reportDate, "success", null, type, projectId, repo);
            return 1;
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "send failed";
            sendLogMapper.insert(to, reportDate, "failed", truncate(msg, 2000), type, projectId, repo);
            log.warn("Send report mail failed: to={}, type={}, repo={}, err={}", to, type, repo, msg);
            return 0;
        }
    }

    private ReportConfig ensureReportConfigRow() {
        ReportConfig cfg = reportConfigMapper.getLatest();
        if (cfg != null) return cfg;
        ReportConfig init = new ReportConfig();
        init.setEnabled(false);
        init.setCompanyName("");
        init.setDailyExtraMessage("");
        reportConfigMapper.insert(init);
        return reportConfigMapper.getLatest();
    }

    private void ensureRecipientRow(String email) {
        if (email == null || email.isBlank()) return;
        ReportRecipient r = recipientMapper.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (r == null) {
            r = new ReportRecipient();
            r.setEmail(email.trim().toLowerCase(Locale.ROOT));
            r.setDisplayName(null);
            r.setUnsubscribeToken(generateToken());
            recipientMapper.upsert(r);
        } else if (r.getUnsubscribeToken() == null || r.getUnsubscribeToken().isBlank()) {
            r.setUnsubscribeToken(generateToken());
            recipientMapper.upsert(r);
        }
    }

    private String ensureRecipientAndGetToken(String email) {
        if (email == null || email.isBlank()) return null;
        String key = email.trim().toLowerCase(Locale.ROOT);
        ReportRecipient r = recipientMapper.findByEmail(key);
        if (r == null) {
            r = new ReportRecipient();
            r.setEmail(key);
            r.setDisplayName(null);
            r.setUnsubscribeToken(generateToken());
            recipientMapper.upsert(r);
            return r.getUnsubscribeToken();
        }
        if (r.getUnsubscribeToken() == null || r.getUnsubscribeToken().isBlank()) {
            r.setUnsubscribeToken(generateToken());
            recipientMapper.upsert(r);
        }
        return r.getUnsubscribeToken();
    }

    private Set<String> loadBlacklistEmails() {
        Set<String> set = new HashSet<>();
        try {
            var list = blacklistMapper.listAll();
            for (var it : list) {
                if (it != null && it.getEmail() != null && !it.getEmail().isBlank()) {
                    set.add(it.getEmail().trim().toLowerCase(Locale.ROOT));
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return set;
    }

    private Set<String> filterTargets(Set<String> emails, Set<String> blacklist) {
        Set<String> out = new LinkedHashSet<>();
        if (emails == null) return out;
        for (String e : emails) {
            if (e == null) continue;
            String email = e.trim().toLowerCase(Locale.ROOT);
            if (email.isBlank()) continue;
            if (blacklist != null && blacklist.contains(email)) continue;
            ReportRecipient r = recipientMapper.findByEmail(email);
            if (r != null && r.getUnsubscribedAt() != null) continue;
            // 若没入库，也允许发送（首次发送时会补齐 recipient 行）
            out.add(email);
        }
        return out;
    }

    private String renderProjectReportHtml(ReportConfig cfg, String repoFullName, LocalDate date, String unsubscribeToken) {
        String md = "";
        if (repoFullName != null && !repoFullName.isBlank()) {
            ProjectDailySummary s = projectDailySummaryMapper.findByRepoAndDate(tenantId(), repoFullName.trim(), date);
            if (s != null && s.getContent() != null) {
                md = s.getContent();
            }
        }
        String title = (cfg != null && cfg.getCompanyName() != null && !cfg.getCompanyName().isBlank())
                ? cfg.getCompanyName().trim()
                : "项目";
        String extra = cfg != null ? Objects.toString(cfg.getDailyExtraMessage(), "") : "";
        String subjectLine = "项目日报";
        if (repoFullName != null && !repoFullName.isBlank()) subjectLine += " · " + repoFullName.trim();
        subjectLine += " · " + date;

        String unsubUrl = buildUnsubscribeUrl(unsubscribeToken);

        return """
                <div style="font-family: -apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial; line-height:1.55; color:#0f172a;">
                  <h2 style="margin:0 0 10px;">%s</h2>
                  <div style="color:#475569; margin-bottom:12px;">%s</div>
                  %s
                  <div style="margin:14px 0; padding:10px 12px; background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px;">
                    <div style="font-weight:600; margin-bottom:6px;">%s</div>
                    <pre style="white-space:pre-wrap; margin:0; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size:12px; color:#334155;">%s</pre>
                  </div>
                  <div style="color:#94a3b8; font-size:12px;">
                    若不想接收此类邮件，可点击退订：<a href="%s" target="_blank" rel="noopener noreferrer">%s</a>
                  </div>
                </div>
                """.formatted(escapeHtml(title),
                escapeHtml(subjectLine),
                (extra != null && !extra.isBlank()) ? "<p style=\"margin:0 0 10px; color:#334155;\">" + escapeHtml(extra) + "</p>" : "",
                "日报正文（Markdown 原文）",
                escapeHtml(md != null ? md : ""),
                escapeHtml(unsubUrl),
                escapeHtml(unsubUrl));
    }

    private String renderDeveloperReportHtml(ReportConfig cfg, String toEmail, LocalDate date, ZoneId zone,
                                            OffsetDateTime start, OffsetDateTime end, String repoFilter,
                                            String unsubscribeToken) {
        String company = (cfg != null && cfg.getCompanyName() != null && !cfg.getCompanyName().isBlank())
                ? cfg.getCompanyName().trim()
                : "工作日报";
        String extra = cfg != null ? Objects.toString(cfg.getDailyExtraMessage(), "") : "";
        List<CommitRecord> commits = commitService.listCommitsByRepoBetween(repoFilter, start, end);
        List<CommitRecord> mine = new ArrayList<>();
        for (CommitRecord c : commits) {
            if (c != null && c.getAuthorEmail() != null && c.getAuthorEmail().trim().equalsIgnoreCase(toEmail)) {
                mine.add(c);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("邮箱：").append(toEmail).append("\n");
        sb.append("业务日：").append(date).append("（").append(zone.getId()).append("）\n");
        sb.append("仓库：").append(repoFilter).append("\n");
        sb.append("提交数：").append(mine.size()).append("\n\n");
        int i = 0;
        for (CommitRecord c : mine) {
            i++;
            String sha = c.getCommitSha() != null ? c.getCommitSha() : "";
            String shortSha = sha.length() >= 7 ? sha.substring(0, 7) : sha;
            sb.append(i).append(". ").append(shortSha).append(" ").append(Objects.toString(c.getCommittedAt(), "")).append("\n");
            sb.append("   ").append(Objects.toString(c.getMessage(), "")).append("\n");
            sb.append("   规模：文件 ").append(c.getFilesChanged()).append("，+").append(c.getInsertions()).append(" -").append(c.getDeletions()).append("\n");
        }

        String unsubUrl = buildUnsubscribeUrl(unsubscribeToken);
        return """
                <div style="font-family: -apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial; line-height:1.55; color:#0f172a;">
                  <h2 style="margin:0 0 10px;">%s</h2>
                  <div style="color:#475569; margin-bottom:12px;">开发者个人日报 · %s · %s</div>
                  %s
                  <div style="margin:14px 0; padding:10px 12px; background:#f8fafc; border:1px solid #e2e8f0; border-radius:10px;">
                    <div style="font-weight:600; margin-bottom:6px;">%s</div>
                    <pre style="white-space:pre-wrap; margin:0; font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size:12px; color:#334155;">%s</pre>
                  </div>
                  <div style="color:#94a3b8; font-size:12px;">
                    若不想接收此类邮件，可点击退订：<a href="%s" target="_blank" rel="noopener noreferrer">%s</a>
                  </div>
                </div>
                """.formatted(escapeHtml(company),
                escapeHtml(repoFilter),
                escapeHtml(date.toString()),
                (extra != null && !extra.isBlank()) ? "<p style=\"margin:0 0 10px; color:#334155;\">" + escapeHtml(extra) + "</p>" : "",
                "你的提交汇总",
                escapeHtml(sb.toString()),
                escapeHtml(unsubUrl),
                escapeHtml(unsubUrl));
    }

    private String buildProjectSubject(ReportConfig cfg, String repoFullName, LocalDate date) {
        String company = (cfg != null && cfg.getCompanyName() != null && !cfg.getCompanyName().isBlank())
                ? cfg.getCompanyName().trim()
                : "工作日报";
        String repo = (repoFullName != null && !repoFullName.isBlank()) ? repoFullName.trim() : "项目";
        return "【" + company + "】项目日报 - " + repo + " - " + date;
    }

    private String buildDeveloperSubject(ReportConfig cfg, String repoFullName, LocalDate date) {
        String company = (cfg != null && cfg.getCompanyName() != null && !cfg.getCompanyName().isBlank())
                ? cfg.getCompanyName().trim()
                : "工作日报";
        String repo = (repoFullName != null && !repoFullName.isBlank()) ? repoFullName.trim() : "项目";
        return "【" + company + "】开发者日报 - " + repo + " - " + date;
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String buildUnsubscribeUrl(String token) {
        if (token == null || token.isBlank()) return "/api/public/report/unsubscribe";
        String base = systemConfigService.getPublicBaseUrl();
        String path = "/api/public/report/unsubscribe?token=" + token;
        if (base == null || base.isBlank()) return path;
        return base + path;
    }

    private static Set<String> parseEmailsJson(String rawJson) {
        Set<String> out = new LinkedHashSet<>();
        if (rawJson == null || rawJson.isBlank()) return out;
        try {
            List<String> list = objectMapper.readValue(rawJson, new TypeReference<List<String>>() {});
            for (String s : list) {
                if (s == null) continue;
                String e = s.trim().toLowerCase(Locale.ROOT);
                if (!e.isBlank()) out.add(e);
            }
        } catch (Exception ignore) {
            // 兼容：允许用户用逗号/换行输入
            String[] parts = rawJson.split("[,\\n\\r\\t ]+");
            for (String p : parts) {
                String e = p.trim().toLowerCase(Locale.ROOT);
                if (!e.isBlank() && e.contains("@")) out.add(e);
            }
        }
        return out;
    }

    // ===== tenantId: 当前系统尚未对 report_* 表做 tenant 分区，先复用 DEFAULT；后续可迁移到 tenant_id 模式 =====
    private static long tenantId() {
        // 与 ProjectDailySummaryMapper 需要 tenant_id 的接口对齐：当前 summary 表包含 tenant_id
        // 这里先使用 0（DEFAULT_TENANT_ID），保持与其他服务一致
        return org.example.atuo_attend_backend.tenant.context.TenantContext.getTenantIdOrDefault(
                org.example.atuo_attend_backend.tenant.context.TenantConstants.DEFAULT_TENANT_ID);
    }
}


package org.example.atuo_attend_backend.report.controller;

import org.example.atuo_attend_backend.report.domain.ReportRecipient;
import org.example.atuo_attend_backend.report.mapper.ReportRecipientMapper;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 邮件退订入口（无需登录）：/api/public/report/unsubscribe?token=xxx
 */
@RestController
@RequestMapping("/api/public/report")
public class PublicReportUnsubscribeController {

    private final ReportRecipientMapper recipientMapper;

    public PublicReportUnsubscribeController(ReportRecipientMapper recipientMapper) {
        this.recipientMapper = recipientMapper;
    }

    @GetMapping(value = "/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
    public String unsubscribe(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank()) {
            return html("退订失败", "缺少 token 参数。");
        }
        ReportRecipient r = recipientMapper.findByToken(token.trim());
        if (r == null || r.getEmail() == null || r.getEmail().isBlank()) {
            return html("退订失败", "token 无效或已过期。");
        }
        recipientMapper.markUnsubscribed(r.getEmail());
        return html("已退订", "你已成功退订日报邮件。之后将不再收到该系统发送的日报。");
    }

    private static String html(String title, String msg) {
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>%s</title>
                </head>
                <body style="font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial; padding:24px; color:#0f172a;">
                  <h2 style="margin:0 0 12px;">%s</h2>
                  <p style="margin:0; color:#334155;">%s</p>
                </body>
                </html>
                """.formatted(escape(title), escape(title), escape(msg));
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}


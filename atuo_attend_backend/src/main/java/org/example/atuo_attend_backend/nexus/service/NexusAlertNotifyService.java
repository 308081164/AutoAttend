package org.example.atuo_attend_backend.nexus.service;

import org.example.atuo_attend_backend.nexus.domain.NexusAlertRule;
import org.example.atuo_attend_backend.report.service.MailSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NexusAlertNotifyService {

    private static final Logger log = LoggerFactory.getLogger(NexusAlertNotifyService.class);

    private final MailSenderService mailSenderService;
    private final RestTemplate restTemplate = new RestTemplate();

    public NexusAlertNotifyService(MailSenderService mailSenderService) {
        this.mailSenderService = mailSenderService;
    }

    public void notifyAlert(NexusAlertRule rule, String jsonPayload) {
        if (rule.getWebhookUrl() != null && !rule.getWebhookUrl().isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                restTemplate.exchange(
                        rule.getWebhookUrl().trim(),
                        HttpMethod.POST,
                        new HttpEntity<>(jsonPayload, headers),
                        String.class
                );
            } catch (Exception e) {
                log.warn("[nexus] alert webhook failed ruleId={}: {}", rule.getId(), e.getMessage());
            }
        }
        if (rule.getNotifyEmail() != null && !rule.getNotifyEmail().isBlank() && mailSenderService.isConfigured()) {
            try {
                String safe = escapeHtml(jsonPayload);
                mailSenderService.sendHtml(
                        rule.getNotifyEmail().trim(),
                        "[快捷运维] " + (rule.getName() != null ? rule.getName() : "告警"),
                        "<pre style=\"font-family:monospace\">" + safe + "</pre>"
                );
            } catch (Exception e) {
                log.warn("[nexus] alert email failed ruleId={}: {}", rule.getId(), e.getMessage());
            }
        }
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}

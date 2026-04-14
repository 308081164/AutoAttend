package org.example.atuo_attend_backend.report.service;

import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.atuo_attend_backend.config.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 以系统配置表（aa_system_config）为准动态构造 SMTP 发信客户端。
 * 项目级不重复保存 SMTP 配置；统一由「API 配置与能力测试」模块维护。
 */
@Service
public class MailSenderService {

    private static final Logger log = LoggerFactory.getLogger(MailSenderService.class);

    private final SystemConfigService systemConfigService;

    public MailSenderService(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    public boolean isConfigured() {
        return systemConfigService.getMailSmtpHost() != null
                && systemConfigService.getMailSmtpPort() != null
                && systemConfigService.getMailFromAddress() != null;
    }

    /**
     * 仅验证 SMTP 连接（不发信），供能力测试使用。
     */
    public void verifySmtpConnection() throws Exception {
        JavaMailSenderImpl sender = buildSender();
        Session session = sender.getSession();
        Transport transport = session.getTransport("smtp");
        try {
            String host = systemConfigService.getMailSmtpHost();
            Integer port = systemConfigService.getMailSmtpPort();
            String username = systemConfigService.getMailSmtpUsername();
            String password = systemConfigService.getMailSmtpPassword();
            if (username != null && !username.isBlank()) {
                transport.connect(host, port, username, password);
            } else {
                transport.connect();
            }
        } finally {
            transport.close();
        }
    }

    /**
     * 发送一封 HTML 邮件。
     *
     * @throws Exception 发送失败抛出
     */
    public void sendHtml(String toEmail, String subject, String htmlBody) throws Exception {
        if (toEmail == null || toEmail.isBlank()) throw new IllegalArgumentException("toEmail required");
        if (subject == null) subject = "";
        if (htmlBody == null) htmlBody = "";

        JavaMailSenderImpl sender = buildSender();
        String from = systemConfigService.getMailFromAddress();
        String fromName = systemConfigService.getMailFromName();
        if (from == null || from.isBlank()) throw new IllegalStateException("mail.from.address not configured");

        MimeMessage msg = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, false, StandardCharsets.UTF_8.name());
        if (fromName != null && !fromName.isBlank()) {
            helper.setFrom(new InternetAddress(from, fromName, StandardCharsets.UTF_8.name()));
        } else {
            helper.setFrom(from);
        }
        helper.setTo(toEmail.trim());
        helper.setSubject(subject);
        helper.setText(htmlBody, true);
        sender.send(msg);
    }

    private JavaMailSenderImpl buildSender() {
        String host = systemConfigService.getMailSmtpHost();
        Integer port = systemConfigService.getMailSmtpPort();
        String username = systemConfigService.getMailSmtpUsername();
        String password = systemConfigService.getMailSmtpPassword();

        if (host == null || host.isBlank() || port == null) {
            throw new IllegalStateException("SMTP host/port not configured");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host.trim());
        sender.setPort(port);
        if (username != null && !username.isBlank()) {
            sender.setUsername(username.trim());
        }
        if (password != null && !password.isBlank()) {
            sender.setPassword(password);
        }
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());

        Properties props = new Properties();
        // 默认启用认证；若不填 username/password 则认证不会生效
        props.put("mail.smtp.auth", (username != null && !username.isBlank()) ? "true" : "false");
        // 允许 STARTTLS（多数 SMTP 提供商需要）
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "false");
        props.put("mail.smtp.ssl.trust", host.trim());
        // 超时（毫秒）
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "20000");
        props.put("mail.smtp.writetimeout", "20000");
        sender.setJavaMailProperties(props);

        log.debug("SMTP sender built: host={}, port={}, user={}", host, port, username != null && !username.isBlank());
        return sender;
    }
}


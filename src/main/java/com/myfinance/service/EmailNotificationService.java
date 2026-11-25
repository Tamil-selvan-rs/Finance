package com.myfinance.service;

import com.myfinance.entity.FinanceItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final FileStorageService fileStorageService;

    @Value("${spring.mail.username:noreply@myfinance.com}")
    private String fromAddress;

    @Value("${app.notifications.enabled:true}")
    private boolean notificationsEnabled;

    public EmailNotificationService(JavaMailSender mailSender, FileStorageService fileStorageService) {
        this.mailSender = mailSender;
        this.fileStorageService = fileStorageService;
    }

    @Async
    public void sendDueDateReminder(FinanceItem item) {
        if (!notificationsEnabled) {
            log.info("Email notifications disabled; skipping email for item ID={}", item.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromAddress);
            helper.setTo(item.getUser().getEmail());
            String formattedDue = item.getDueDate() != null ? item.getDueDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) : "No Due Date";
            helper.setSubject("[MyFinance Alert] Payment Reminder: " + item.getTitle() + " (" + formattedDue + ")");

            String htmlBody = """
                <div style="background:#12091E;color:#FCE7F3;font-family:Segoe UI,Arial,sans-serif;padding:28px;border-radius:12px;border:1px solid #231438;">
                    <h2 style="color:#F472B6;margin-top:0;">MyFinance Due Date Notification</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Your scheduled financial commitment is due on <strong>%s</strong>:</p>
                    <table style="width:100%%;border-collapse:collapse;background:#231438;border-radius:8px;overflow:hidden;margin:16px 0;">
                        <tr><td style="padding:10px 14px;color:#F472B6;">Title</td><td style="padding:10px 14px;color:#FCE7F3;"><strong>%s</strong></td></tr>
                        <tr><td style="padding:10px 14px;color:#F472B6;">Category</td><td style="padding:10px 14px;color:#FCE7F3;">%s (%s)</td></tr>
                        <tr><td style="padding:10px 14px;color:#F472B6;">Amount Due</td><td style="padding:10px 14px;color:#FCE7F3;font-size:18px;"><strong>₹ %s</strong></td></tr>
                        <tr><td style="padding:10px 14px;color:#F472B6;">Notes</td><td style="padding:10px 14px;color:#FCE7F3;">%s</td></tr>
                    </table>
                    <p style="font-size:13px;color:#F472B6;">Sign in to your MyFinance Enterprise Dashboard to mark this item as PAID.</p>
                </div>
                """.formatted(
                    item.getUser().getFullName(),
                    formattedDue,
                    item.getTitle(),
                    item.getCategory(),
                    item.getItemType(),
                    item.getAmount(),
                    item.getNotes() != null && !item.getNotes().isBlank() ? item.getNotes() : "No notes attached"
            );

            helper.setText(htmlBody, true);

            if (item.getAttachmentPath() != null) {
                File attachmentFile = fileStorageService.resolveFilePath(item.getAttachmentPath()).toFile();
                if (attachmentFile.exists()) {
                    String displayName = item.getAttachmentOriginalName() != null ? item.getAttachmentOriginalName() : attachmentFile.getName();
                    helper.addAttachment(displayName, new FileSystemResource(attachmentFile));
                }
            }

            mailSender.send(message);
            log.info("Due date email sent successfully to {} for item ID={}", item.getUser().getEmail(), item.getId());
        } catch (MessagingException | RuntimeException ex) {
            log.error("Failed to send due date email for item ID={}: {}", item.getId(), ex.getMessage());
        }
    }
}

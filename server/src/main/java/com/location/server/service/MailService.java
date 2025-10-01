package com.location.server.service;

import jakarta.mail.internet.MimeMessage;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {
  private final JavaMailSender mailSender;

  @Value("${app.mail.from:}")
  private String defaultFrom;

  public MailService(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  public void send(
      List<String> to,
      String subject,
      String html,
      List<Map<String, Object>> attachments,
      List<String> cc,
      List<String> bcc,
      String from)
      throws Exception {
    if (to == null || to.isEmpty()) {
      throw new IllegalArgumentException("Destinataire requis");
    }
    MimeMessage message = mailSender.createMimeMessage();
    boolean multipart = attachments != null && !attachments.isEmpty();
    MimeMessageHelper helper = new MimeMessageHelper(message, multipart, "UTF-8");

    String effectiveFrom = (from == null || from.isBlank()) ? defaultFrom : from;
    if (effectiveFrom != null && !effectiveFrom.isBlank()) {
      helper.setFrom(effectiveFrom);
    }

    helper.setTo(to.toArray(String[]::new));

    if (cc != null && !cc.isEmpty()) {
      helper.setCc(cc.toArray(new String[0]));
    }
    if (bcc != null && !bcc.isEmpty()) {
      helper.setBcc(bcc.toArray(new String[0]));
    }

    helper.setSubject(subject == null ? "" : subject);
    helper.setText(html == null ? "" : html, true);

    if (multipart) {
      for (Map<String, Object> attachment : attachments) {
        String name = String.valueOf(attachment.getOrDefault("filename", "attachment"));
        String base64 = String.valueOf(attachment.get("base64"));
        String contentType =
            String.valueOf(attachment.getOrDefault("contentType", "application/octet-stream"));
        byte[] data = Base64.getDecoder().decode(base64);
        ByteArrayResource resource =
            new ByteArrayResource(data) {
              @Override
              public String getFilename() {
                return name;
              }
            };
        helper.addAttachment(name, resource, contentType);
      }
    }

    mailSender.send(message);
  }
}

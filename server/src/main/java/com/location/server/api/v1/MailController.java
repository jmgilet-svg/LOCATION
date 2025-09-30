package com.location.server.api.v1;

import com.location.server.service.MailService;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mail")
public class MailController {
  @Autowired private MailService mailService;

  @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> send(@RequestBody Map<String, Object> payload) {
    try {
      Object to = payload.get("to");
      String subject = (String) payload.getOrDefault("subject", "");
      String html = (String) payload.getOrDefault("html", "");
      String from = (String) payload.getOrDefault("from", "");
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> attachments = (List<Map<String, Object>>) payload.get("attachments");
      @SuppressWarnings("unchecked")
      List<String> cc = (List<String>) payload.get("cc");
      @SuppressWarnings("unchecked")
      List<String> bcc = (List<String>) payload.get("bcc");
      mailService.send(to, subject, html, attachments, cc, bcc, from);
      return ResponseEntity.accepted().build();
    } catch (Exception e) {
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
  }
}

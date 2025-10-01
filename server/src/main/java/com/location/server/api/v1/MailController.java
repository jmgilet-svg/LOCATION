package com.location.server.api.v1;

import com.location.server.api.v1.dto.MailSendRequest;
import com.location.server.service.MailService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mail")
public class MailController {
  private final MailService mailService;

  public MailController(MailService mailService) {
    this.mailService = mailService;
  }

  @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Void> send(@Valid @RequestBody MailSendRequest request) throws Exception {
    List<String> recipients = resolveRecipients(request.getTo(), request.getToList());
    List<Map<String, Object>> attachments = request.getAttachments();
    mailService.send(
        recipients,
        request.getSubject(),
        request.getHtml(),
        attachments,
        sanitizeAddresses(request.getCc()),
        sanitizeAddresses(request.getBcc()),
        request.getFrom());
    return ResponseEntity.accepted().build();
  }

  private List<String> resolveRecipients(String single, List<String> list) {
    List<String> fromList = sanitizeAddresses(list);
    if (fromList != null && !fromList.isEmpty()) {
      return fromList;
    }
    if (single != null && !single.isBlank()) {
      return List.of(single);
    }
    throw new IllegalArgumentException("Destinataire requis");
  }

  private List<String> sanitizeAddresses(List<String> addresses) {
    if (addresses == null) {
      return null;
    }
    List<String> sanitized =
        addresses.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toCollection(ArrayList::new));
    return sanitized.isEmpty() ? null : sanitized;
  }
}

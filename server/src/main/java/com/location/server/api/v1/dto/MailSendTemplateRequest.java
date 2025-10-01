package com.location.server.api.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public class MailSendTemplateRequest {
  @Email(message = "Destinataire invalide")
  private String to;

  private List<@Email(message = "Destinataire invalide") String> toList;

  @NotBlank(message = "Sujet requis")
  @Size(max = 200, message = "Sujet trop long")
  private String subject;

  @NotBlank(message = "Clé de template requise")
  @Size(max = 120, message = "Clé de template trop longue")
  private String key;

  private Map<String, Object> context;
  private boolean attachPdf = true;

  @Size(max = 120, message = "Nom de fichier trop long")
  private String filename = "document.pdf";

  public String getTo() {
    return to;
  }

  public void setTo(String to) {
    this.to = to;
  }

  public List<String> getToList() {
    return toList;
  }

  public void setToList(List<String> toList) {
    this.toList = toList;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Map<String, Object> getContext() {
    return context;
  }

  public void setContext(Map<String, Object> context) {
    this.context = context;
  }

  public boolean isAttachPdf() {
    return attachPdf;
  }

  public void setAttachPdf(boolean attachPdf) {
    this.attachPdf = attachPdf;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }
}

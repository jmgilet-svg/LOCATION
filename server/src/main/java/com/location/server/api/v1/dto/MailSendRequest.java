package com.location.server.api.v1.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

public class MailSendRequest {
  @Email(message = "Destinataire invalide")
  private String to;

  private List<@Email(message = "Destinataire invalide") String> toList;

  @NotBlank(message = "Sujet requis")
  @Size(max = 200, message = "Sujet trop long")
  private String subject;

  @NotBlank(message = "Corps HTML requis")
  private String html;

  private String from;
  private List<@Email(message = "CC invalide") String> cc;
  private List<@Email(message = "BCC invalide") String> bcc;
  private List<Map<String, Object>> attachments;

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

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public List<String> getCc() {
    return cc;
  }

  public void setCc(List<String> cc) {
    this.cc = cc;
  }

  public List<String> getBcc() {
    return bcc;
  }

  public void setBcc(List<String> bcc) {
    this.bcc = bcc;
  }

  public List<Map<String, Object>> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<Map<String, Object>> attachments) {
    this.attachments = attachments;
  }
}

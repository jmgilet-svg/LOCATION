package com.location.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "agency")
public class Agency {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "email_subject_template", length = 255)
  private String emailSubjectTemplate;

  @Column(name = "email_body_template", columnDefinition = "TEXT")
  private String emailBodyTemplate;

  @Column(name = "legal_footer", length = 800)
  private String legalFooter;

  @Column(length = 34)
  private String iban;

  @Lob
  @Column(name = "logo_png")
  private byte[] logoPng;

  protected Agency() {}

  public Agency(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmailSubjectTemplate() {
    return emailSubjectTemplate;
  }

  public void setEmailSubjectTemplate(String emailSubjectTemplate) {
    this.emailSubjectTemplate = emailSubjectTemplate;
  }

  public String getEmailBodyTemplate() {
    return emailBodyTemplate;
  }

  public void setEmailBodyTemplate(String emailBodyTemplate) {
    this.emailBodyTemplate = emailBodyTemplate;
  }

  public String getLegalFooter() {
    return legalFooter;
  }

  public void setLegalFooter(String legalFooter) {
    this.legalFooter = legalFooter;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }

  public byte[] getLogoPng() {
    return logoPng;
  }

  public void setLogoPng(byte[] logoPng) {
    this.logoPng = logoPng;
  }
}

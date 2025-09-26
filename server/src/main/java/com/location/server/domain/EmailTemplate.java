package com.location.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "email_template",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_email_template_agency_type",
            columnNames = {"agency_id", "doc_type"}))
public class EmailTemplate {

  @Id private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "agency_id")
  private Agency agency;

  @Enumerated(EnumType.STRING)
  @Column(name = "doc_type", nullable = false, length = 16)
  private CommercialDocument.DocType documentType;

  @Column(length = 180)
  private String subject;

  @Column(columnDefinition = "text")
  private String body;

  public EmailTemplate() {}

  public EmailTemplate(
      String id,
      Agency agency,
      CommercialDocument.DocType documentType,
      String subject,
      String body) {
    this.id = id;
    this.agency = agency;
    this.documentType = documentType;
    this.subject = subject;
    this.body = body;
  }

  public String getId() {
    return id;
  }

  public Agency getAgency() {
    return agency;
  }

  public CommercialDocument.DocType getDocumentType() {
    return documentType;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public void setBody(String body) {
    this.body = body;
  }
}

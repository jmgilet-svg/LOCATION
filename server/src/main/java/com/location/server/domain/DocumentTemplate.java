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
    name = "document_template",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_document_template_agency_type",
            columnNames = {"agency_id", "doc_type"}))
public class DocumentTemplate {

  @Id private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "agency_id")
  private Agency agency;

  @Enumerated(EnumType.STRING)
  @Column(name = "doc_type", nullable = false, length = 16)
  private CommercialDocument.DocType documentType;

  @Column(columnDefinition = "text", nullable = false)
  private String html;

  public DocumentTemplate() {}

  public DocumentTemplate(
      String id, Agency agency, CommercialDocument.DocType documentType, String html) {
    this.id = id;
    this.agency = agency;
    this.documentType = documentType;
    this.html = html;
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

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }
}

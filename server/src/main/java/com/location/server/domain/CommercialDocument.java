package com.location.server.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "commercial_document",
    indexes = {
      @Index(name = "idx_document_type_date", columnList = "type, doc_date"),
      @Index(name = "idx_document_client", columnList = "client_id"),
      @Index(name = "idx_document_agency", columnList = "agency_id")
    })
public class CommercialDocument {
  public enum DocType {
    QUOTE,
    ORDER,
    DELIVERY,
    INVOICE
  }

  public enum DocStatus {
    DRAFT,
    SENT,
    ACCEPTED,
    CANCELLED,
    ISSUED,
    PAID
  }

  @Id
  private String id;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 16)
  private DocType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private DocStatus status;

  @Column(name = "doc_ref", length = 64)
  private String reference;

  @Column(name = "doc_title", length = 140)
  private String title;

  @Column(name = "doc_date")
  private OffsetDateTime date;

  @ManyToOne(optional = false)
  @JoinColumn(name = "agency_id")
  private Agency agency;

  @ManyToOne(optional = false)
  @JoinColumn(name = "client_id")
  private Client client;

  @OneToMany(
      mappedBy = "document",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private List<CommercialDocumentLine> lines = new ArrayList<>();

  @Column(name = "total_ht", precision = 14, scale = 2, nullable = false)
  private BigDecimal totalHt = BigDecimal.ZERO;

  @Column(name = "total_vat", precision = 14, scale = 2, nullable = false)
  private BigDecimal totalVat = BigDecimal.ZERO;

  @Column(name = "total_ttc", precision = 14, scale = 2, nullable = false)
  private BigDecimal totalTtc = BigDecimal.ZERO;

  @Column(name = "delivered", nullable = false)
  private boolean delivered;

  @Column(name = "paid", nullable = false)
  private boolean paid;

  public CommercialDocument() {}

  public CommercialDocument(
      String id,
      DocType type,
      DocStatus status,
      String reference,
      String title,
      OffsetDateTime date,
      Agency agency,
      Client client) {
    this.id = id;
    this.type = type;
    this.status = status;
    this.reference = reference;
    this.title = title;
    this.date = date;
    this.agency = agency;
    this.client = client;
    this.totalHt = BigDecimal.ZERO;
    this.totalVat = BigDecimal.ZERO;
    this.totalTtc = BigDecimal.ZERO;
    this.delivered = false;
    this.paid = false;
  }

  public String getId() {
    return id;
  }

  public DocType getType() {
    return type;
  }

  public void setType(DocType type) {
    this.type = type;
  }

  public DocStatus getStatus() {
    return status;
  }

  public void setStatus(DocStatus status) {
    this.status = status;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public OffsetDateTime getDate() {
    return date;
  }

  public void setDate(OffsetDateTime date) {
    this.date = date;
  }

  public Agency getAgency() {
    return agency;
  }

  public void setAgency(Agency agency) {
    this.agency = agency;
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client client) {
    this.client = client;
  }

  public List<CommercialDocumentLine> getLines() {
    return lines;
  }

  public BigDecimal getTotalHt() {
    return totalHt;
  }

  public BigDecimal getTotalVat() {
    return totalVat;
  }

  public BigDecimal getTotalTtc() {
    return totalTtc;
  }

  public void setTotals(BigDecimal ht, BigDecimal vat, BigDecimal ttc) {
    this.totalHt = ht;
    this.totalVat = vat;
    this.totalTtc = ttc;
  }

  public boolean isDelivered() {
    return delivered;
  }

  public void setDelivered(boolean delivered) {
    this.delivered = delivered;
  }

  public boolean isPaid() {
    return paid;
  }

  public void setPaid(boolean paid) {
    this.paid = paid;
  }
}

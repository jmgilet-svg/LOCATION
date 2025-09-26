package com.location.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "commercial_document_line")
public class CommercialDocumentLine {
  @Id
  private String id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "document_id")
  private CommercialDocument document;

  @Column(name = "line_no", nullable = false)
  private int lineNo;

  @Column(name = "designation", length = 240, nullable = false)
  private String designation;

  @Column(name = "quantity", precision = 14, scale = 3, nullable = false)
  private BigDecimal quantity;

  @Column(name = "unit_price", precision = 14, scale = 2, nullable = false)
  private BigDecimal unitPrice;

  @Column(name = "vat_rate", precision = 5, scale = 2, nullable = false)
  private BigDecimal vatRate;

  public CommercialDocumentLine() {}

  public CommercialDocumentLine(
      String id,
      CommercialDocument document,
      int lineNo,
      String designation,
      BigDecimal quantity,
      BigDecimal unitPrice,
      BigDecimal vatRate) {
    this.id = id;
    this.document = document;
    this.lineNo = lineNo;
    this.designation = designation;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.vatRate = vatRate;
  }

  public String getId() {
    return id;
  }

  public CommercialDocument getDocument() {
    return document;
  }

  public void setDocument(CommercialDocument document) {
    this.document = document;
  }

  public int getLineNo() {
    return lineNo;
  }

  public void setLineNo(int lineNo) {
    this.lineNo = lineNo;
  }

  public String getDesignation() {
    return designation;
  }

  public void setDesignation(String designation) {
    this.designation = designation;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getVatRate() {
    return vatRate;
  }

  public void setVatRate(BigDecimal vatRate) {
    this.vatRate = vatRate;
  }
}

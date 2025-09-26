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
    name = "doc_sequence",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_doc_sequence_agency_year_type",
            columnNames = {"agency_id", "doc_year", "doc_type"}))
public class DocumentSequence {

  @Id private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "agency_id")
  private Agency agency;

  @Column(name = "doc_year", nullable = false)
  private int year;

  @Enumerated(EnumType.STRING)
  @Column(name = "doc_type", nullable = false, length = 16)
  private CommercialDocument.DocType type;

  @Column(name = "last_no", nullable = false)
  private int lastNumber;

  public DocumentSequence() {}

  public DocumentSequence(
      String id, Agency agency, int year, CommercialDocument.DocType type, int lastNumber) {
    this.id = id;
    this.agency = agency;
    this.year = year;
    this.type = type;
    this.lastNumber = lastNumber;
  }

  public String getId() {
    return id;
  }

  public Agency getAgency() {
    return agency;
  }

  public int getYear() {
    return year;
  }

  public CommercialDocument.DocType getType() {
    return type;
  }

  public int getLastNumber() {
    return lastNumber;
  }

  public void setLastNumber(int lastNumber) {
    this.lastNumber = lastNumber;
  }
}

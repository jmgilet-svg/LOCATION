package com.location.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "intervention",
    indexes = {
      @Index(name = "idx_intervention_resource_start_end", columnList = "resource_id,start_ts,end_ts")
    })
public class Intervention {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 140)
  private String title;

  @Column(name = "start_ts", nullable = false)
  private OffsetDateTime start;

  @Column(name = "end_ts", nullable = false)
  private OffsetDateTime end;

  @ManyToOne(optional = false)
  @JoinColumn(name = "agency_id")
  private Agency agency;

  @ManyToOne(optional = false)
  @JoinColumn(name = "resource_id")
  private Resource resource;

  @ManyToOne(optional = false)
  @JoinColumn(name = "client_id")
  private Client client;

  @Column(columnDefinition = "TEXT")
  private String notes;

  protected Intervention() {}

  public Intervention(
      String id,
      String title,
      OffsetDateTime start,
      OffsetDateTime end,
      Agency agency,
      Resource resource,
      Client client) {
    this(id, title, start, end, agency, resource, client, null);
  }

  public Intervention(
      String id,
      String title,
      OffsetDateTime start,
      OffsetDateTime end,
      Agency agency,
      Resource resource,
      Client client,
      String notes) {
    this.id = id;
    this.title = title;
    this.start = start;
    this.end = end;
    this.agency = agency;
    this.resource = resource;
    this.client = client;
    this.notes = notes;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public OffsetDateTime getStart() {
    return start;
  }

  public void setStart(OffsetDateTime start) {
    this.start = start;
  }

  public OffsetDateTime getEnd() {
    return end;
  }

  public void setEnd(OffsetDateTime end) {
    this.end = end;
  }

  public Agency getAgency() {
    return agency;
  }

  public void setAgency(Agency agency) {
    this.agency = agency;
  }

  public Resource getResource() {
    return resource;
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client client) {
    this.client = client;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}

package com.location.server.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "unavailability",
    indexes = {
        @Index(name = "idx_unav_resource_start_end", columnList = "resource_id,start_ts,end_ts")
    })
public class Unavailability {
  @Id
  private String id;

  @ManyToOne(optional = false) @JoinColumn(name = "resource_id")
  private Resource resource;

  @Column(name = "start_ts", nullable = false)
  private OffsetDateTime start;

  @Column(name = "end_ts", nullable = false)
  private OffsetDateTime end;

  @Column(nullable = false, length = 140)
  private String reason;

  public Unavailability(){}
  public Unavailability(String id, Resource resource, OffsetDateTime start, OffsetDateTime end, String reason) {
    this.id = id; this.resource = resource; this.start = start; this.end = end; this.reason = reason;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Resource getResource() { return resource; }
  public void setResource(Resource resource) { this.resource = resource; }
  public OffsetDateTime getStart() { return start; }
  public void setStart(OffsetDateTime start) { this.start = start; }
  public OffsetDateTime getEnd() { return end; }
  public void setEnd(OffsetDateTime end) { this.end = end; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
}

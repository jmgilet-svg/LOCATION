package com.location.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Index;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(
    name = "recurring_unavailability",
    indexes = {@Index(name = "idx_recurring_unav_resource_dow", columnList = "resource_id,dow")})
public class RecurringUnavailability {

  @Id
  @Column(length = 36)
  private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "resource_id")
  private Resource resource;

  @Enumerated(EnumType.STRING)
  @Column(name = "dow", length = 10, nullable = false)
  private DayOfWeek dayOfWeek;

  @Column(name = "start_t", nullable = false)
  private LocalTime startTime;

  @Column(name = "end_t", nullable = false)
  private LocalTime endTime;

  @Column(nullable = false, length = 140)
  private String reason;

  protected RecurringUnavailability() {}

  public RecurringUnavailability(
      String id,
      Resource resource,
      DayOfWeek dayOfWeek,
      LocalTime startTime,
      LocalTime endTime,
      String reason) {
    this.id = id;
    this.resource = resource;
    this.dayOfWeek = dayOfWeek;
    this.startTime = startTime;
    this.endTime = endTime;
    this.reason = reason;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Resource getResource() {
    return resource;
  }

  public void setResource(Resource resource) {
    this.resource = resource;
  }

  public DayOfWeek getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(DayOfWeek dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  public LocalTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalTime startTime) {
    this.startTime = startTime;
  }

  public LocalTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalTime endTime) {
    this.endTime = endTime;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}


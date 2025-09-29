package com.location.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "resource")
public class Resource {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "license_plate", length = 32)
  private String licensePlate;

  @Column(name = "color_rgb")
  private Integer colorRgb;

  @ManyToOne(optional = false)
  @JoinColumn(name = "agency_id")
  private Agency agency;

  @Column(length = 255)
  private String tags;

  @Column(name = "capacity_tons")
  private Integer capacityTons;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "resource_type_id")
  private ResourceType resourceType;

  protected Resource() {}

  public Resource(String id, String name, String licensePlate, Integer colorRgb, Agency agency) {
    this.id = id;
    this.name = name;
    this.licensePlate = licensePlate;
    this.colorRgb = colorRgb;
    this.agency = agency;
  }

  public Resource(
      String id,
      String name,
      String licensePlate,
      Integer colorRgb,
      Agency agency,
      String tags,
      Integer capacityTons) {
    this(id, name, licensePlate, colorRgb, agency);
    this.tags = tags;
    this.capacityTons = capacityTons;
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

  public String getLicensePlate() {
    return licensePlate;
  }

  public void setLicensePlate(String licensePlate) {
    this.licensePlate = licensePlate;
  }

  public Integer getColorRgb() {
    return colorRgb;
  }

  public void setColorRgb(Integer colorRgb) {
    this.colorRgb = colorRgb;
  }

  public Agency getAgency() {
    return agency;
  }

  public void setAgency(Agency agency) {
    this.agency = agency;
  }

  public String getTags() {
    return tags;
  }

  public void setTags(String tags) {
    this.tags = tags;
  }

  public Integer getCapacityTons() {
    return capacityTons;
  }

  public void setCapacityTons(Integer capacityTons) {
    this.capacityTons = capacityTons;
  }

  public ResourceType getResourceType() {
    return resourceType;
  }

  public void setResourceType(ResourceType resourceType) {
    this.resourceType = resourceType;
  }
}

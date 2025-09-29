package com.location.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "resource_type")
public class ResourceType {

  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 80, unique = true)
  private String name;

  @Column(name = "icon_name", nullable = false, length = 120)
  private String iconName;

  protected ResourceType() {}

  public ResourceType(String id, String name, String iconName) {
    this.id = id;
    this.name = name;
    this.iconName = iconName;
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

  public String getIconName() {
    return iconName;
  }

  public void setIconName(String iconName) {
    this.iconName = iconName;
  }
}

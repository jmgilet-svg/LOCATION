package com.location.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "client")
public class Client {
  @Id
  @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 128)
  private String name;

  @Column(name = "billing_email", nullable = false, length = 160)
  private String billingEmail;

  protected Client() {}

  public Client(String id, String name, String billingEmail) {
    this.id = id;
    this.name = name;
    this.billingEmail = billingEmail;
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

  public String getBillingEmail() {
    return billingEmail;
  }

  public void setBillingEmail(String billingEmail) {
    this.billingEmail = billingEmail;
  }
}

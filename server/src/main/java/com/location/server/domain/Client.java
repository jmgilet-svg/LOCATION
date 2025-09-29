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

  @Column(name = "billing_address", length = 200)
  private String billingAddress;

  @Column(name = "billing_zip", length = 16)
  private String billingZip;

  @Column(name = "billing_city", length = 120)
  private String billingCity;

  @Column(name = "vat_number", length = 32)
  private String vatNumber;

  @Column(length = 34)
  private String iban;

  protected Client() {}

  public Client(String id, String name, String billingEmail) {
    this(id, name, billingEmail, null, null, null, null, null);
  }

  public Client(
      String id,
      String name,
      String billingEmail,
      String billingAddress,
      String billingZip,
      String billingCity,
      String vatNumber,
      String iban) {
    this.id = id;
    this.name = name;
    this.billingEmail = billingEmail;
    this.billingAddress = billingAddress;
    this.billingZip = billingZip;
    this.billingCity = billingCity;
    this.vatNumber = vatNumber;
    this.iban = iban;
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

  public String getBillingAddress() {
    return billingAddress;
  }

  public void setBillingAddress(String billingAddress) {
    this.billingAddress = billingAddress;
  }

  public String getBillingZip() {
    return billingZip;
  }

  public void setBillingZip(String billingZip) {
    this.billingZip = billingZip;
  }

  public String getBillingCity() {
    return billingCity;
  }

  public void setBillingCity(String billingCity) {
    this.billingCity = billingCity;
  }

  public String getVatNumber() {
    return vatNumber;
  }

  public void setVatNumber(String vatNumber) {
    this.vatNumber = vatNumber;
  }

  public String getIban() {
    return iban;
  }

  public void setIban(String iban) {
    this.iban = iban;
  }
}

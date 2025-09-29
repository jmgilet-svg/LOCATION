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
  private String email;

  @Column(length = 50)
  private String phone;

  @Column(name = "billing_address", length = 200)
  private String address;

  @Column(name = "billing_zip", length = 16)
  private String zip;

  @Column(name = "billing_city", length = 120)
  private String city;

  @Column(name = "vat_number", length = 32)
  private String vatNumber;

  @Column(length = 34)
  private String iban;

  protected Client() {}

  public Client(String id, String name, String email) {
    this(id, name, email, null, null, null, null, null, null);
  }

  public Client(
      String id,
      String name,
      String email,
      String phone,
      String address,
      String zip,
      String city,
      String vatNumber,
      String iban) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.phone = phone;
    this.address = address;
    this.zip = zip;
    this.city = city;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
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

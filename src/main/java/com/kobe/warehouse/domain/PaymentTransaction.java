package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;

/** A Payment. */
@Getter
@Entity
@Table(
    name = "payment_transaction",
    indexes = {@Index(columnList = "organisme_id", name = "organisme_id_index")})
public class PaymentTransaction implements Serializable {
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "amount", nullable = false)
  private int amount;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @ManyToOne(optional = false)
  @JsonIgnoreProperties(value = "payments", allowSetters = true)
  private PaymentMode paymentMode;

  @NotNull
  @ManyToOne(optional = false)
  private User user;

  @NotNull
  @ManyToOne(optional = false)
  private CashRegister cashRegister;

  @Column(name = "organisme_id")
  private String organismeId;

  @Size(max = 50)
  @Column(name = "ticket_code", length = 50)
  private String ticketCode;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "categorie_ca", nullable = false)
  private CategorieChiffreAffaire categorieChiffreAffaire = CategorieChiffreAffaire.CA;

  public PaymentTransaction createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public int getAmount() {
    return amount;
  }

  public PaymentTransaction setAmount(int amount) {
    this.amount = amount;
    return this;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public PaymentMode getPaymentMode() {
    return paymentMode;
  }

  public void setPaymentMode(PaymentMode paymentMode) {
    this.paymentMode = paymentMode;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public CashRegister getCashRegister() {
    return cashRegister;
  }

  public PaymentTransaction setCashRegister(CashRegister cashRegister) {
    this.cashRegister = cashRegister;
    return this;
  }

  public String getOrganismeId() {
    return organismeId;
  }

  public PaymentTransaction setOrganismeId(String organismeId) {
    this.organismeId = organismeId;
    return this;
  }

  public String getTicketCode() {
    return ticketCode;
  }

  public PaymentTransaction setTicketCode(String ticketCode) {
    this.ticketCode = ticketCode;
    return this;
  }

  public CategorieChiffreAffaire getCategorieChiffreAffaire() {
    return categorieChiffreAffaire;
  }

  public PaymentTransaction setCategorieChiffreAffaire(
      CategorieChiffreAffaire categorieChiffreAffaire) {
    this.categorieChiffreAffaire = categorieChiffreAffaire;
    return this;
  }

  public PaymentTransaction paymentMode(PaymentMode paymentMode) {
    this.paymentMode = paymentMode;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof PaymentTransaction)) {
      return false;
    }
    return id != null && id.equals(((PaymentTransaction) o).id);
  }

  @Override
  public int hashCode() {
    return 31;
  }
}

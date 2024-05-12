package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(
    name = "cash_register_item",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"cash_register_id", "payment_mode_id"})})
public class CashRegisterItem implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @NotNull
  private CashRegister cashRegister;

  private long amount;

  @ManyToOne(optional = false)
  @NotNull
  private PaymentMode paymentMode;

  public Long getId() {
    return id;
  }

  public CashRegisterItem setId(Long id) {
    this.id = id;
    return this;
  }

  public CashRegister getCashRegister() {
    return cashRegister;
  }

  public CashRegisterItem setCashRegister(CashRegister cashRegister) {
    this.cashRegister = cashRegister;
    return this;
  }

  public long getAmount() {
    return amount;
  }

  public CashRegisterItem setAmount(long amount) {
    this.amount = amount;
    return this;
  }

  public PaymentMode getPaymentMode() {
    return paymentMode;
  }

  public CashRegisterItem setPaymentMode(PaymentMode paymentMode) {
    this.paymentMode = paymentMode;
    return this;
  }
}

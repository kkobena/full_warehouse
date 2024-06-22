package com.kobe.warehouse.service.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DeliveryReceiptLiteDTO {

  private Long id;
  @NotNull private Integer receiptAmount;
  private String sequenceBon;
  private String receiptRefernce;
  @NotNull private Integer taxAmount;

  private LocalDate receiptDate;
  private String orderReference;

  private Long commandeId;
  private LocalDateTime receiptFullDate;

  public Long getId() {
    return id;
  }

  public DeliveryReceiptLiteDTO setId(Long id) {
    this.id = id;
    return this;
  }

  public @NotNull Integer getReceiptAmount() {
    return receiptAmount;
  }

  public DeliveryReceiptLiteDTO setReceiptAmount(@NotNull Integer receiptAmount) {
    this.receiptAmount = receiptAmount;
    return this;
  }

  public String getSequenceBon() {
    return sequenceBon;
  }

  public DeliveryReceiptLiteDTO setSequenceBon(String sequenceBon) {
    this.sequenceBon = sequenceBon;
    return this;
  }

  public String getReceiptRefernce() {
    return receiptRefernce;
  }

  public DeliveryReceiptLiteDTO setReceiptRefernce(String receiptRefernce) {
    this.receiptRefernce = receiptRefernce;
    return this;
  }

  public @NotNull Integer getTaxAmount() {
    return taxAmount;
  }

  public DeliveryReceiptLiteDTO setTaxAmount(@NotNull Integer taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  public LocalDate getReceiptDate() {
    return receiptDate;
  }

  public DeliveryReceiptLiteDTO setReceiptDate(LocalDate receiptDate) {
    this.receiptDate = receiptDate;
    return this;
  }

  public String getOrderReference() {
    return orderReference;
  }

  public DeliveryReceiptLiteDTO setOrderReference(String orderReference) {
    this.orderReference = orderReference;
    return this;
  }

  public Long getCommandeId() {
    return commandeId;
  }

  public DeliveryReceiptLiteDTO setCommandeId(Long commandeId) {
    this.commandeId = commandeId;
    return this;
  }

  public LocalDateTime getReceiptFullDate() {
    return receiptFullDate;
  }

  public DeliveryReceiptLiteDTO setReceiptFullDate(LocalDateTime receiptFullDate) {
    this.receiptFullDate = receiptFullDate;
    return this;
  }
}

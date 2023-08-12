package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Payment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.User;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class PaymentDTO {
  private Long id;
  private Integer netAmount;
  private Integer paidAmount;
  private Integer restToPay;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private PaymentModeDTO paymentMode;
  private Long userId;
  private Customer customer;
  private String saleNumberTransaction;
  private Long customerId;
  private Long salesId;
  private Integer salesAmount;
  private Integer salesNetAmount;
  private String userFullName;
  private Integer reelPaidAmount;
  private String paymentCode;
  private Integer montantVerse = 0;

  public PaymentDTO(Payment payment) {
    super();
    this.id = payment.getId();
    this.netAmount = payment.getNetAmount();
    this.paidAmount = payment.getPaidAmount();
    this.createdAt = payment.getCreatedAt();
    this.updatedAt = payment.getUpdatedAt();
    this.paymentMode = new PaymentModeDTO(payment.getPaymentMode());
    User user = payment.getUser();
    this.userId = user.getId();
    this.userFullName = user.getFirstName() + " " + user.getLastName();
    Customer customer = payment.getCustomer();
    this.customer = customer;
    Sales sales = payment.getSales();
    this.saleNumberTransaction = sales.getNumberTransaction();
    this.salesId = sales.getId();
    this.salesAmount = sales.getSalesAmount();
    this.salesNetAmount = sales.getNetAmount();
    this.montantVerse = payment.getMontantVerse();
  }

  public PaymentDTO() {}

  public void setSalesAmount(Integer salesAmount) {
    this.salesAmount = salesAmount;
  }

  public void setSalesNetAmount(Integer salesNetAmount) {
    this.salesNetAmount = salesNetAmount;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
  }

  public void setPaidAmount(Integer paidAmount) {
    this.paidAmount = paidAmount;
  }

  public void setRestToPay(Integer restToPay) {
    this.restToPay = restToPay;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public PaymentDTO setPaymentMode(PaymentModeDTO paymentMode) {
    this.paymentMode = paymentMode;
    return this;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public void setUserFullName(String userFullName) {
    this.userFullName = userFullName;
  }

  public void setSaleNumberTransaction(String saleNumberTransaction) {
    this.saleNumberTransaction = saleNumberTransaction;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public void setSalesId(Long salesId) {
    this.salesId = salesId;
  }

  public PaymentDTO setReelPaidAmount(Integer reelPaidAmount) {
    this.reelPaidAmount = reelPaidAmount;
    return this;
  }

  public PaymentDTO setPaymentCode(String paymentCode) {
    this.paymentCode = paymentCode;
    return this;
  }

  public PaymentDTO setMontantVerse(Integer montantVerse) {
    this.montantVerse = montantVerse;
    return this;
  }
}

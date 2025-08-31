package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.AppUser;
import java.time.LocalDateTime;

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
    private boolean differe;

    public PaymentDTO(SalePayment payment) {
        super();
        this.id = payment.getId();
        this.netAmount = payment.getReelAmount();
        this.paidAmount = payment.getPaidAmount();
        this.createdAt = payment.getCreatedAt();
        this.updatedAt = payment.getCreatedAt();
        this.paymentMode = new PaymentModeDTO(payment.getPaymentMode());
        AppUser user = payment.getCashRegister().getUser();
        this.userId = user.getId();
        this.userFullName = user.getFirstName() + " " + user.getLastName();
        Sales sales = payment.getSale();
        this.customer = sales.getCustomer();
        this.saleNumberTransaction = sales.getNumberTransaction();
        this.salesId = sales.getId().getId();
        this.salesAmount = sales.getSalesAmount();
        this.salesNetAmount = sales.getNetAmount();
        this.montantVerse = payment.getMontantVerse();
    }

    public PaymentDTO() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Integer getRestToPay() {
        return restToPay;
    }

    public void setRestToPay(Integer restToPay) {
        this.restToPay = restToPay;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public PaymentModeDTO getPaymentMode() {
        return paymentMode;
    }

    public PaymentDTO setPaymentMode(PaymentModeDTO paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getSaleNumberTransaction() {
        return saleNumberTransaction;
    }

    public void setSaleNumberTransaction(String saleNumberTransaction) {
        this.saleNumberTransaction = saleNumberTransaction;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getSalesId() {
        return salesId;
    }

    public void setSalesId(Long salesId) {
        this.salesId = salesId;
    }

    public Integer getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Integer getSalesNetAmount() {
        return salesNetAmount;
    }

    public void setSalesNetAmount(Integer salesNetAmount) {
        this.salesNetAmount = salesNetAmount;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public Integer getReelPaidAmount() {
        return reelPaidAmount;
    }

    public PaymentDTO setReelPaidAmount(Integer reelPaidAmount) {
        this.reelPaidAmount = reelPaidAmount;
        return this;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public PaymentDTO setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
        return this;
    }

    public boolean isDiffere() {
        return differe;
    }

    public void setDiffere(boolean differe) {
        this.differe = differe;
    }

    public Integer getMontantVerse() {
        return montantVerse;
    }

    public PaymentDTO setMontantVerse(Integer montantVerse) {
        this.montantVerse = montantVerse;
        return this;
    }
}

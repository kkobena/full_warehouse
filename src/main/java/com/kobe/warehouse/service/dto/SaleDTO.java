package com.kobe.warehouse.service.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.SalesStatut;

import javax.persistence.Column;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = CashSaleDTO.class, name = "VNO"),
    @JsonSubTypes.Type(value = ThirdPartySaleDTO.class, name = "VO")
})
public class SaleDTO implements Serializable {
    private Long id;
    private Integer discountAmount;
    private Customer customer;
    private String numberTransaction;
    private Long customerId;
    private Integer salesAmount;
    private String userFullName;
    private Integer grossAmount;
    private Integer netAmount;
    private Integer taxAmount;
    private Integer costAmount;
    private SalesStatut statut;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SaleLineDTO> salesLines = new ArrayList<>();
    private List<PaymentDTO> payments = new ArrayList<>();
    private Integer dateDimensionId;
    private String sellerUserName;
    private SaleDTO canceledSale;
    private Instant effectiveUpdateDate;
    private boolean toIgnore;
    private String ticketNumber;
    private Integer payrollAmount;
    private Integer amountToBePaid;
    private Integer amountToBeTakenIntoAccount;
    private Remise remise;
    private Integer restToPay;
    private String customerNum;
    private Boolean copy = false;
    private boolean imported = false;
    private Integer margeUg = 0;
    private Integer montantttcUg = 0;
    private Integer montantnetUg = 0;
    private Integer montantTvaUg = 0;
    private Integer marge = 0;


    public Boolean getCopy() {
        return copy;
    }

    public SaleDTO setCopy(Boolean copy) {
        this.copy = copy;
        return this;
    }

    public boolean isImported() {
        return imported;
    }

    public SaleDTO setImported(boolean imported) {
        this.imported = imported;
        return this;
    }

    public Integer getMargeUg() {
        return margeUg;
    }

    public SaleDTO setMargeUg(Integer margeUg) {
        this.margeUg = margeUg;
        return this;
    }

    public Integer getMontantttcUg() {
        return montantttcUg;
    }

    public SaleDTO setMontantttcUg(Integer montantttcUg) {
        this.montantttcUg = montantttcUg;
        return this;
    }

    public Integer getMontantnetUg() {
        return montantnetUg;
    }

    public SaleDTO setMontantnetUg(Integer montantnetUg) {
        this.montantnetUg = montantnetUg;
        return this;
    }

    public Integer getMontantTvaUg() {
        return montantTvaUg;
    }

    public SaleDTO setMontantTvaUg(Integer montantTvaUg) {
        this.montantTvaUg = montantTvaUg;
        return this;
    }

    public Integer getMarge() {
        return marge;
    }

    public SaleDTO setMarge(Integer marge) {
        this.marge = marge;
        return this;
    }

    public String getCustomerNum() {
        return customerNum;
    }

    public SaleDTO setCustomerNum(String customerNum) {
        this.customerNum = customerNum;
        return this;
    }

    public Long getId() {
        return id;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Integer getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Integer getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public SalesStatut getStatut() {
        return statut;
    }

    public void setStatut(SalesStatut statut) {
        this.statut = statut;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNumberTransaction() {
        return numberTransaction;
    }

    public void setNumberTransaction(String numberTransaction) {
        this.numberTransaction = numberTransaction;
    }

    public SaleDTO() {

    }

    public Integer getDateDimensionId() {
        return dateDimensionId;
    }

    public SaleDTO setDateDimensionId(Integer dateDimensionId) {
        this.dateDimensionId = dateDimensionId;
        return this;
    }

    public String getSellerUserName() {
        return sellerUserName;
    }

    public SaleDTO setSellerUserName(String sellerUserName) {
        this.sellerUserName = sellerUserName;
        return this;
    }

    public SaleDTO getCanceledSale() {
        return canceledSale;
    }

    public SaleDTO setCanceledSale(SaleDTO canceledSale) {
        this.canceledSale = canceledSale;
        return this;
    }

    public Instant getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public SaleDTO setEffectiveUpdateDate(Instant effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    public boolean isToIgnore() {
        return toIgnore;
    }

    public SaleDTO setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
        return this;
    }

    public String getTicketNumber() {
        return ticketNumber;
    }

    public SaleDTO setTicketNumber(String ticketNumber) {
        this.ticketNumber = ticketNumber;
        return this;
    }

    public Integer getPayrollAmount() {
        return payrollAmount;
    }

    public SaleDTO setPayrollAmount(Integer payrollAmount) {
        this.payrollAmount = payrollAmount;
        return this;
    }

    public Integer getAmountToBePaid() {
        return amountToBePaid;
    }

    public SaleDTO setAmountToBePaid(Integer amountToBePaid) {
        this.amountToBePaid = amountToBePaid;
        return this;
    }

    public Integer getAmountToBeTakenIntoAccount() {
        return amountToBeTakenIntoAccount;
    }

    public SaleDTO setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }

    public Remise getRemise() {
        return remise;
    }

    public SaleDTO setRemise(Remise remise) {
        this.remise = remise;
        return this;
    }

    public Integer getRestToPay() {
        return restToPay;
    }

    public SaleDTO setRestToPay(Integer restToPay) {
        this.restToPay = restToPay;
        return this;
    }

    public SaleDTO(Sales sale) {
        super();
        this.id = sale.getId();
        this.discountAmount = sale.getDiscountAmount();
        if (sale instanceof ThirdPartySales) {
            ThirdPartySales thirdPartySales = (ThirdPartySales) sale;
            this.customer = thirdPartySales.getAssuredCustomer();
        } else if (sale instanceof CashSale) {
            CashSale cashSale = (CashSale) sale;
            this.customer = cashSale.getUninsuredCustomer();
        }

        this.salesAmount = sale.getSalesAmount();
        this.grossAmount = sale.getGrossAmount();
        this.netAmount = sale.getNetAmount();
        this.taxAmount = sale.getTaxAmount();
        this.costAmount = sale.getCostAmount();
        this.statut = sale.getStatut();
        this.createdAt = sale.getCreatedAt();
        this.updatedAt = sale.getUpdatedAt();
        this.salesLines = sale.getSalesLines().stream().map(SaleLineDTO::new).collect(Collectors.toList());
        this.payments = sale.getPayments().stream().map(PaymentDTO::new).collect(Collectors.toList());
        User user = sale.getUser();
        this.userFullName = user.getFirstName() + " " + user.getLastName();
        this.numberTransaction = sale.getNumberTransaction();
    }

    public List<SaleLineDTO> getSalesLines() {
        return salesLines;
    }

    public void setSalesLines(List<SaleLineDTO> salesLines) {
        this.salesLines = salesLines;
    }

    public List<PaymentDTO> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentDTO> payments) {
        this.payments = payments;
    }

}

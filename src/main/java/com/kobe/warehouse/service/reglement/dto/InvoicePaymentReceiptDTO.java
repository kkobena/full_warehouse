package com.kobe.warehouse.service.reglement.dto;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.service.utils.DateUtil;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InvoicePaymentReceiptDTO {

    private String montantAttendu;

    private String paidAmount;

    private String created;

    private String codeFacture;
    private String organisme;

    private String paymentMode;
    private String paymentModeCode;
    private String change;
    private String transactionNumber;

    private String user;

    private String montantVerse;
    private String montantRestant;
    private String invoicePaymentItemsCount;
    private boolean grouped;
    private List<InvoicePaymentReceiptDTO> invoicePayments = new ArrayList<>();

    public InvoicePaymentReceiptDTO(InvoicePayment invoicePayment) {
        this.montantAttendu = NumberUtil.formatToString(invoicePayment.getExpectedAmount());
        FactureTiersPayant factureTiersPayant = invoicePayment.getFactureTiersPayant();
        this.codeFacture = factureTiersPayant.getNumFacture();
        this.created = DateUtil.format(invoicePayment.getCreatedAt());
        this.grouped = invoicePayment.isGrouped();
        this.invoicePaymentItemsCount = String.valueOf(invoicePayment.getInvoicePaymentItems().size());
        this.montantVerse = NumberUtil.formatToStringIfNotNull(invoicePayment.getMontantVerse());
        this.paidAmount = NumberUtil.formatToString(invoicePayment.getPaidAmount());

        if (invoicePayment.isGrouped()) {
            GroupeTiersPayant groupeTiersPayant = factureTiersPayant.getGroupeTiersPayant();
            this.organisme = groupeTiersPayant.getName();
        } else {
            TiersPayant tiersPayant = factureTiersPayant.getTiersPayant();
            this.organisme = tiersPayant.getName();
        }
this.transactionNumber= invoicePayment.getTransactionNumber();
        PaymentMode mode = invoicePayment.getPaymentMode();
        this.paymentMode = mode.getLibelle();
        this.paymentModeCode = mode.getCode();
        AppUser u = invoicePayment.getCashRegister().getUser();
        this.user = u.getFirstName() + " " + u.getLastName();
        var montantR = invoicePayment.getExpectedAmount() - invoicePayment.getPaidAmount();
        if (montantR > 0) {
            this.montantRestant = NumberUtil.formatToString(montantR);
        } else {
            this.montantRestant = "0";
        }
        var change0 = Objects.requireNonNullElse(invoicePayment.getMontantVerse(), 0) - invoicePayment.getExpectedAmount();
        if (change0 > 0) {
            this.change = NumberUtil.formatToString(change0);
        } else {
            this.change = "0";
        }
    }

    public String getChange() {
        return change;
    }

    public void setChange(String change) {
        this.change = change;
    }

    public String getMontantRestant() {
        return montantRestant;
    }

    public void setMontantRestant(String montantRestant) {
        this.montantRestant = montantRestant;
    }

    public boolean getGrouped() {
        return grouped;
    }

    public String getMontantAttendu() {
        return montantAttendu;
    }

    public void setMontantAttendu(String montantAttendu) {
        this.montantAttendu = montantAttendu;
    }

    public String getCodeFacture() {
        return codeFacture;
    }

    public void setCodeFacture(String codeFacture) {
        this.codeFacture = codeFacture;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public boolean isGrouped() {
        return grouped;
    }

    public void setGrouped(boolean grouped) {
        this.grouped = grouped;
    }

    public String getInvoicePaymentItemsCount() {
        return invoicePaymentItemsCount;
    }

    public void setInvoicePaymentItemsCount(String invoicePaymentItemsCount) {
        this.invoicePaymentItemsCount = invoicePaymentItemsCount;
    }

    public List<InvoicePaymentReceiptDTO> getInvoicePayments() {
        return invoicePayments;
    }

    public void setInvoicePayments(List<InvoicePaymentReceiptDTO> invoicePayments) {
        this.invoicePayments = invoicePayments;
    }

    public String getMontantVerse() {
        return montantVerse;
    }

    public void setMontantVerse(String montantVerse) {
        this.montantVerse = montantVerse;
    }

    public String getOrganisme() {
        return organisme;
    }

    public void setOrganisme(String organisme) {
        this.organisme = organisme;
    }

    public String getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(String paidAmount) {
        this.paidAmount = paidAmount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPaymentModeCode() {
        return paymentModeCode;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public InvoicePaymentReceiptDTO setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
        return this;
    }

    public void setPaymentModeCode(String paymentModeCode) {
        this.paymentModeCode = paymentModeCode;
    }
}

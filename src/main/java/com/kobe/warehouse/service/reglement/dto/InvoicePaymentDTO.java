package com.kobe.warehouse.service.reglement.dto;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.InvoicePayment;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.service.utils.DateUtil;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.util.List;
import java.util.Objects;

public class InvoicePaymentDTO {

    private final long id;
    private final String organisme;
    private final long organismeId;
    private final String codeFacture;
    private final String montantAttendu;
    private final String montantVerse;
    private final String montantRestant;
    private final String paymentMode;
    private final String change;
    private final String user;
    private final String created;
    private final String paidAmount;
    private final boolean grouped;
    private final int invoicePaymentItemsCount;
    private final int totalAmount;
    private List<InvoicePaymentDTO> invoicePayments;
    private List<InvoicePaymentItemDTO> invoicePaymentItems;

    public InvoicePaymentDTO(InvoicePayment invoicePayment) {
        this.id = invoicePayment.getId().getId();
        this.montantAttendu = NumberUtil.formatToString(invoicePayment.getExpectedAmount());
        FactureTiersPayant factureTiersPayant = invoicePayment.getFactureTiersPayant();
        this.codeFacture = factureTiersPayant.getNumFacture();
        this.created = DateUtil.format(invoicePayment.getCreatedAt());
        this.grouped = invoicePayment.isGrouped();

        this.montantVerse = NumberUtil.formatToStringIfNotNull(invoicePayment.getMontantVerse());
        this.paidAmount = NumberUtil.formatToString(invoicePayment.getPaidAmount());
        this.totalAmount = invoicePayment.getPaidAmount();
        TiersPayant tiersPayant = factureTiersPayant.getTiersPayant();
        if (Objects.isNull(tiersPayant)) {
            GroupeTiersPayant groupeTiersPayant = factureTiersPayant.getGroupeTiersPayant();
            this.organisme = groupeTiersPayant.getName();
            this.organismeId = groupeTiersPayant.getId();
        } else {
            this.organisme = tiersPayant.getName();
            this.organismeId = tiersPayant.getId();
        }

        PaymentMode mode = invoicePayment.getPaymentMode();
        this.paymentMode = mode.getLibelle();
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

        this.invoicePaymentItemsCount = invoicePayment.getInvoicePaymentItems().size();
    }

    public String getChange() {
        return change;
    }

    public String getCodeFacture() {
        return codeFacture;
    }

    public int getInvoicePaymentItemsCount() {
        return invoicePaymentItemsCount;
    }

    public String getCreated() {
        return created;
    }

    public int getTotalAmount() {
        return totalAmount;
    }

    public boolean isGrouped() {
        return grouped;
    }

    public long getId() {
        return id;
    }

    public List<InvoicePaymentItemDTO> getInvoicePaymentItems() {
        return invoicePaymentItems;
    }

    public void setInvoicePaymentItems(List<InvoicePaymentItemDTO> invoicePaymentItems) {
        this.invoicePaymentItems = invoicePaymentItems;
    }

    public List<InvoicePaymentDTO> getInvoicePayments() {
        return invoicePayments;
    }

    public void setInvoicePayments(List<InvoicePaymentDTO> invoicePayments) {
        this.invoicePayments = invoicePayments;
    }

    public String getMontantAttendu() {
        return montantAttendu;
    }

    public String getMontantRestant() {
        return montantRestant;
    }

    public String getMontantVerse() {
        return montantVerse;
    }

    public String getOrganisme() {
        return organisme;
    }

    public String getPaidAmount() {
        return paidAmount;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public String getUser() {
        return user;
    }

    public long getOrganismeId() {
        return organismeId;
    }
}

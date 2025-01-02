package com.kobe.warehouse.service.reglement.dto;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.time.format.DateTimeFormatter;

public class InvoicePaymentItemDTO {

    private final String numBon;
    private final String montant;
    private final String montantVente;
    private final String montantAttendu;
    private final String customer;
    private final String created;
    private final String heure;
    private final String customerMatricule;

    public InvoicePaymentItemDTO(InvoicePaymentItem invoicePaymentItem) {
        ThirdPartySaleLine thirdPartySaleLine = invoicePaymentItem.getThirdPartySaleLine();
        this.created = thirdPartySaleLine.getCreated().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        this.heure = thirdPartySaleLine.getCreated().format(DateTimeFormatter.ofPattern("HH:mm"));
        this.numBon = thirdPartySaleLine.getNumBon();
        this.montant = NumberUtil.formatToString(invoicePaymentItem.getPaidAmount());
        this.montantVente = NumberUtil.formatToString(thirdPartySaleLine.getMontant());
        this.montantAttendu = NumberUtil.formatToString(invoicePaymentItem.getAmount());
        ClientTiersPayant clientTiersPayant = thirdPartySaleLine.getClientTiersPayant();
        AssuredCustomer assuredCustomer = clientTiersPayant.getAssuredCustomer();
        this.customer = assuredCustomer.getFirstName() + " " + assuredCustomer.getLastName();
        this.customerMatricule = clientTiersPayant.getNum();
    }

    public String getCreated() {
        return created;
    }

    public String getMontantAttendu() {
        return montantAttendu;
    }

    public String getMontantVente() {
        return montantVente;
    }

    public String getCustomer() {
        return customer;
    }

    public String getCustomerMatricule() {
        return customerMatricule;
    }

    public String getHeure() {
        return heure;
    }

    public String getMontant() {
        return montant;
    }

    public String getNumBon() {
        return numBon;
    }
}

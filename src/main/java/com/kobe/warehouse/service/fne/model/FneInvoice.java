package com.kobe.warehouse.service.fne.model;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public class FneInvoice {

    private  List<FneInvoiceItem> items ;
    @NotNull
    private String clientNcc;
    @NotNull
    private String clientCompanyName;
    private String clientPhone;
    private String clientEmail;
    @NotNull
    private String pointOfSale;
    @NotNull
    private String establishment;
    private String commercialMessage;
    private String footer;


    public String getClientCompanyName() {
        return clientCompanyName;
    }

    public void setClientCompanyName(String clientCompanyName) {
        this.clientCompanyName = clientCompanyName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getClientNcc() {
        return clientNcc;
    }

    public void setClientNcc(String clientNcc) {
        this.clientNcc = clientNcc;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public String getCommercialMessage() {
        return commercialMessage;
    }

    public void setCommercialMessage(String commercialMessage) {
        this.commercialMessage = commercialMessage;
    }

    public String getEstablishment() {
        return establishment;
    }

    public void setEstablishment(String establishment) {
        this.establishment = establishment;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public String getInvoiceType() {

        return "sale";
    }

    public List<FneInvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<FneInvoiceItem> items) {
        this.items = items;
    }

    public String getPaymentMethod() {

        return "check";
    }

    public String getPointOfSale() {
        return pointOfSale;
    }

    public void setPointOfSale(String pointOfSale) {
        this.pointOfSale = pointOfSale;
    }

    public String getTemplate() {

        return "B2B";
    }
}


/*
fneUrl=http://54.247.95.108/ws/external/invoices/sign
fnePkey=ftjep8vmlDKXCgcAkc9Plur7rYTrV4GT
fnepointOfSale=PHARMACIE

1428351F
 */

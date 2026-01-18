package com.kobe.warehouse.service.fne.model;

public class FneResponseInvoice {

    private String id;
    private int quantity;
    private String reference;
    private String description;
    private Double amount;
    private List<FneResponseTaxe> taxes;
}

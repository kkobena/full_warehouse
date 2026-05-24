package com.kobe.warehouse.service.errors;

public class InvoiceEmptyDataException extends BadRequestAlertException {

    public InvoiceEmptyDataException() {
        super("Aucun bon à facturer sur cette période");
    }
}

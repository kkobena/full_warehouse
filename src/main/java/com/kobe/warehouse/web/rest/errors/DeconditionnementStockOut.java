package com.kobe.warehouse.web.rest.errors;

public class DeconditionnementStockOut extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public DeconditionnementStockOut(String produitId) {
        super(String.format("Stock insuffisant [%s]", produitId), "stockChInsufisant");
    }
}

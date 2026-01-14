package com.kobe.warehouse.service.errors;

import java.io.Serial;

public class DeconditionnementStockOut extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DeconditionnementStockOut(String produitId) {
        super(String.format("Stock insuffisant [%s]", produitId), "stockChInsufisant", produitId);
        //  BadRequestAlertException(String defaultMessage, String errorKey, Object payload)
    }
}

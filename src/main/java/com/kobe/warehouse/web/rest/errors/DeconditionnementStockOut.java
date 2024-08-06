package com.kobe.warehouse.web.rest.errors;

import java.io.Serial;
import org.springframework.http.HttpStatus;

public class DeconditionnementStockOut extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DeconditionnementStockOut(String produitId) {
        super(HttpStatus.BAD_REQUEST, String.format("Stock insuffisant [%s]", produitId), "stockChInsufisant", produitId);
        //  BadRequestAlertException(String defaultMessage, String errorKey, Object payload)
    }
}

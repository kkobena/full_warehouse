package com.kobe.warehouse.web.rest.errors;

public class StockException extends BadRequestAlertException {

    private static final long serialVersionUID = 1L;

    public StockException() {
        super("Stock insuffisant", "stock");
    }
}

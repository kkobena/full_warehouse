package com.kobe.warehouse.service.errors;

import java.io.Serial;

public class StockException extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public StockException() {
        super("Stock insuffisant", "stock");
    }
}

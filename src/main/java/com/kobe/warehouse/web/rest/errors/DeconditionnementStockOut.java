package com.kobe.warehouse.web.rest.errors;

public class DeconditionnementStockOut extends   BadRequestAlertException {
    private static final long serialVersionUID = 1L;
    public DeconditionnementStockOut(String prouduitId) {
        super(ErrorConstants.ERR_STOCK_INSUFFISANT,prouduitId, "sales", "stockChInsufisant");
    }
}

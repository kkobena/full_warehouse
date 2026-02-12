package com.kobe.warehouse.service.errors;

import java.io.Serial;

public class QuantitySoldException extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public QuantitySoldException() {
        super("La quantité vendue ne peut excéder la quantité commandée", "quantitySold");
    }
}

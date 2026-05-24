package com.kobe.warehouse.service.errors;

import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;

public class PlafondVenteException extends BadRequestAlertException {

    public PlafondVenteException(ThirdPartySaleDTO payload, String message) {
        super(message, "customerInsuranceCreditLimit", payload);
    }
}

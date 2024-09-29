package com.kobe.warehouse.service.errors;

import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import org.springframework.http.HttpStatus;

public class PlafondVenteException extends BadRequestAlertException {

    public PlafondVenteException(ThirdPartySaleDTO payload, String message) {
        super(HttpStatus.PRECONDITION_FAILED, message, payload);
    }
}

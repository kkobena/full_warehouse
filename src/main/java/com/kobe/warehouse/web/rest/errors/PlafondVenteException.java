package com.kobe.warehouse.web.rest.errors;

import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import org.springframework.http.HttpStatus;

public class PlafondVenteException extends BadRequestAlertException {

    private final ThirdPartySaleDTO payload;

    public PlafondVenteException(ThirdPartySaleDTO payload, String message) {
        super(HttpStatus.PRECONDITION_FAILED, message);
        this.payload = payload;
    }

    public ThirdPartySaleDTO getPayload() {
        return payload;
    }
}

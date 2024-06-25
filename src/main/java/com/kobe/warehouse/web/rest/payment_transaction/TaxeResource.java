package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TaxeResource extends TaxeProxy {


    public TaxeResource(TaxeService taxeService) {
        super(taxeService);
    }
}

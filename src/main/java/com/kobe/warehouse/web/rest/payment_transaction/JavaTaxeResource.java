package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client")
public class JavaTaxeResource extends TaxeProxy {

  public JavaTaxeResource(TaxeService taxeService) {
    super(taxeService);
  }
}

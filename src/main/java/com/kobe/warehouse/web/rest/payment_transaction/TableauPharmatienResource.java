package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TableauPharmatienResource extends TableauPharmacienProxy {

  public TableauPharmatienResource(TableauPharmacienService tableauPharmacienService) {
    super(tableauPharmacienService);
  }
}

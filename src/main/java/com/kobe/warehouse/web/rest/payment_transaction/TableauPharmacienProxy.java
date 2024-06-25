package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienService;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class TableauPharmacienProxy {
  private final TableauPharmacienService tableauPharmacienService;

  public TableauPharmacienProxy(TableauPharmacienService tableauPharmacienService) {
    this.tableauPharmacienService = tableauPharmacienService;
  }

  @GetMapping("/tableau-pharmacien")
  TableauPharmacienWrapper getTableauPharmacien(
      @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
      @RequestParam(value = "toDate", required = false) LocalDate toDate,
      @RequestParam(value = "categorieChiffreAffaires", required = false)
          Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
      @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes,
      @RequestParam(value = "groupBy", required = false, defaultValue = "daily") String groupeBy) {
    return tableauPharmacienService.getTableauPharmacien(
        fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy);
  }
}

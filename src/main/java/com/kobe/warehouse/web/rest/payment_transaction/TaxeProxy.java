package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class TaxeProxy {
  private final TaxeService taxeService;

  public TaxeProxy(TaxeService taxeService) {
    this.taxeService = taxeService;
  }

  @GetMapping("/taxe-report")
  public ResponseEntity<TaxeWrapperDTO> getTaxe(
      @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
      @RequestParam(value = "toDate", required = false) LocalDate toDate,
      @RequestParam(value = "categorieChiffreAffaires", required = false)
          Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
      @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes,
      @RequestParam(value = "groupBy", required = false, defaultValue = "codeTva")
          String groupeBy) {
    return ResponseEntity.ok(
        taxeService.fetchTaxe(
            new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy)
                .build(),
            false,
            false));
  }

  @GetMapping("/taxe-report/pdf")
  public ResponseEntity<TaxeWrapperDTO> getExportTaxes(
      @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
      @RequestParam(value = "toDate", required = false) LocalDate toDate,
      @RequestParam(value = "categorieChiffreAffaires", required = false)
          Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
      @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes,
      @RequestParam(value = "groupBy", required = false, defaultValue = "codeTva")
          String groupeBy) {
    return ResponseEntity.ok(
        taxeService.fetchTaxe(
            new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy)
                .build(),
            false,
            true));
  }
}

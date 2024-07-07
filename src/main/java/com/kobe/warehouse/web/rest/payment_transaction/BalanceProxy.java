package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseService;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.http.ResponseEntity;

public class BalanceProxy {
  private final BalanceCaisseService balanceCaisseService;

  public BalanceProxy(BalanceCaisseService balanceCaisseService) {
    this.balanceCaisseService = balanceCaisseService;
  }

  public ResponseEntity<BalanceCaisseWrapper> getBalance(
      LocalDate fromDate,
      LocalDate toDate,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes) {

    return ResponseEntity.ok()
        .body(
            this.balanceCaisseService.getBalanceCaisse(
                new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, null)
                    .build()));
  }
}

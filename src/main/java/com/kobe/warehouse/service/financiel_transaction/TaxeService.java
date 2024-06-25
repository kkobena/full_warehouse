package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.dto.TaxeWrapperDTO;
import java.time.LocalDate;
import java.util.Set;

public interface TaxeService {
  TaxeWrapperDTO fetchTaxe(
      LocalDate fromDate,
      LocalDate toDate,
      Set<CategorieChiffreAffaire> categorieChiffreAffaires,
      Set<SalesStatut> statuts,
      Set<TypeVente> typeVentes,
      String groupeBy,
      boolean ignoreSomeTaxe);
}

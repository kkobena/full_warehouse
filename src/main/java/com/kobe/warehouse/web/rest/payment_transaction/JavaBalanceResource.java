package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseService;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client")
public class JavaBalanceResource extends BalanceProxy {

    public JavaBalanceResource(BalanceCaisseService balanceCaisseService) {
        super(balanceCaisseService);
    }

    @GetMapping("/balances")
    public ResponseEntity<BalanceCaisseWrapper> fetchBalanceCaisse(
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
        @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes
    ) {
        return super.getBalance(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes);
    }
}

package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseService;
import com.kobe.warehouse.service.financiel_transaction.dto.BalanceCaisseWrapper;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        Set<TypeVente> typeVentes
    ) {
        return ResponseEntity.ok()
            .body(
                this.balanceCaisseService.getBalanceCaisse(
                        new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, null).build()
                    )
            );
    }

    @GetMapping("/balances/pdf")
    public ResponseEntity<Resource> getExportTaxes(
        HttpServletRequest request,
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
        @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes
    ) throws MalformedURLException {
        Resource resource = balanceCaisseService.exportToPdf(
            new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, null).build()
        );
        return Utils.printPDF(resource, request);
    }
}

package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public class TableauPharmacienProxy {

    private final TableauPharmacienService tableauPharmacienService;

    public TableauPharmacienProxy(TableauPharmacienService tableauPharmacienService) {
        this.tableauPharmacienService = tableauPharmacienService;
    }

    @GetMapping("/tableau-pharmacien")
    public ResponseEntity<TableauPharmacienWrapper> getTableauPharmacien(
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
        @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes,
        @RequestParam(value = "groupBy", required = false, defaultValue = "daily") String groupeBy
    ) {
        return ResponseEntity.ok(
            tableauPharmacienService.getTableauPharmacien(
                new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy).build()
            )
        );
    }

    @GetMapping("/top-groupe-fournisseurs")
    public ResponseEntity<List<GroupeFournisseurDTO>> fetchGroupGrossisteToDisplay() {
        return ResponseEntity.ok(tableauPharmacienService.fetchGroupGrossisteToDisplay());
    }

    @GetMapping("/tableau-pharmacien/pdf")
    public ResponseEntity<Resource> exportToPdf(
        HttpServletRequest request,
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
        @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes,
        @RequestParam(value = "groupBy", required = false, defaultValue = "daily") String groupeBy
    ) throws MalformedURLException {
        Resource resource = tableauPharmacienService.exportToPdf(
            new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy).build()
        );
        return Utils.printPDF(resource, request);
    }

    @GetMapping("/tableau-pharmacien/excel")
    public ResponseEntity<Resource> exportToExcel(
        HttpServletRequest request,
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
        @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes,
        @RequestParam(value = "groupBy", required = false, defaultValue = "daily") String groupeBy
    ) throws IOException {
        return Utils.exportExcel(
            tableauPharmacienService.exportToExcel(
                new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy).build()
            ),
            request
        );
    }
}

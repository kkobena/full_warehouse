package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.service.cash_register.dto.TypeVente;
import com.kobe.warehouse.service.dto.GroupeFournisseurDTO;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienService;
import com.kobe.warehouse.service.financiel_transaction.dto.ModeChiffreAffaire;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.financiel_transaction.dto.TableauPharmacienWrapper;
import com.kobe.warehouse.web.rest.Utils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class TableauPharmatienResource {
    private final TableauPharmacienService tableauPharmacienService;

    public TableauPharmatienResource(TableauPharmacienService tableauPharmacienService) {
        this.tableauPharmacienService = tableauPharmacienService;
    }

    @GetMapping("/tableau-pharmacien")
    public ResponseEntity<TableauPharmacienWrapper> getTableauPharmacien(
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
        @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes,
        @RequestParam(value = "groupBy", required = false, defaultValue = "daily") String groupeBy,
        @RequestParam(value = "mode", required = false) ModeChiffreAffaire mode
    ) {
        return ResponseEntity.ok(
            tableauPharmacienService.getTableauPharmacien(
                new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy).setMode(mode).build()
            )
        );
    }

    @GetMapping("/top-groupe-fournisseurs")
    public ResponseEntity<List<GroupeFournisseurDTO>> fetchGroupGrossisteToDisplay() {
        return ResponseEntity.ok(tableauPharmacienService.fetchGroupGrossisteToDisplay());
    }

    @GetMapping("/tableau-pharmacien/pdf")
    public ResponseEntity<byte[]> exportToPdf(
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
        @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes,
        @RequestParam(value = "groupBy", required = false, defaultValue = "daily") String groupeBy,
        @RequestParam(value = "mode", required = false) ModeChiffreAffaire mode
    ) {

        return Utils.printPDF(tableauPharmacienService.exportToPdf(
            new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy).setMode(mode).build()
        ), nomFichier("tableau_pharmacien", mode, "pdf"));
    }

    @GetMapping("/tableau-pharmacien/excel")
    public ResponseEntity<byte[]> exportToExcel(
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "statuts", required = false) Set<SalesStatut> statuts,
        @RequestParam(value = "typeVentes", required = false) Set<TypeVente> typeVentes,
        @RequestParam(value = "groupBy", required = false, defaultValue = "daily") String groupeBy,
        @RequestParam(value = "mode", required = false) ModeChiffreAffaire mode
    ) {

        return Utils.exportExcel(tableauPharmacienService.exportToExcel(
            new MvtParam(fromDate, toDate, categorieChiffreAffaires, statuts, typeVentes, groupeBy).setMode(mode).build()
        ), nomFichier("tableau_pharmacien", mode, "xlsx"));
    }

    /**
     * Le mode figure dans le nom du fichier : deux exports d'apparence identique aux totaux
     * differents sont une source d'erreur garantie une fois sur le bureau de quelqu'un.
     */
    private static String nomFichier(String base, ModeChiffreAffaire mode, String extension) {
        return base + (mode == ModeChiffreAffaire.REEL ? "_ca_reel." : "_ca_declare.") + extension;
    }
}

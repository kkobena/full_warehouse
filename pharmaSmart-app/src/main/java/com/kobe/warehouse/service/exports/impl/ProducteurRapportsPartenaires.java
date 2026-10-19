package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import com.kobe.warehouse.service.ap.AccountsPayableService;
import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.facturation.dto.RapprochementParams;
import com.kobe.warehouse.service.facturation.dto.RecapitulatifMensuelParams;
import com.kobe.warehouse.service.facturation.service.RapprochementService;
import com.kobe.warehouse.service.facturation.service.RecapitulatifMensuelService;
import com.kobe.warehouse.service.report.pdf.SupplierPerformancePdfReportService;
import com.kobe.warehouse.service.report.pdf.TiersPayantPdfReportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Éditions des écrans Fournisseurs et Tiers payants. */
@Component
class ProducteurRapportsPartenaires {

    private final AccountsPayableService accountsPayableService;
    private final SupplierPerformancePdfReportService supplierPerformancePdfReportService;
    private final TiersPayantPdfReportService tiersPayantPdfReportService;
    private final RecapitulatifMensuelService recapitulatifMensuelService;
    private final RapprochementService rapprochementService;

    ProducteurRapportsPartenaires(
        AccountsPayableService accountsPayableService,
        SupplierPerformancePdfReportService supplierPerformancePdfReportService,
        TiersPayantPdfReportService tiersPayantPdfReportService,
        RecapitulatifMensuelService recapitulatifMensuelService,
        RapprochementService rapprochementService
    ) {
        this.accountsPayableService = accountsPayableService;
        this.supplierPerformancePdfReportService = supplierPerformancePdfReportService;
        this.tiersPayantPdfReportService = tiersPayantPdfReportService;
        this.recapitulatifMensuelService = recapitulatifMensuelService;
        this.rapprochementService = rapprochementService;
    }

    byte[] produire(RapportExport rapport, FormatExport format, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        return switch (rapport) {
            case COMPTES_FOURNISSEURS -> accountsPayableService.exportComptesAsPdf(du, au);
            case PERFORMANCE_FOURNISSEURS -> supplierPerformancePdfReportService.export();
            case CREANCES_TIERS_PAYANTS -> tiersPayantPdfReportService.export();
            case RECAPITULATIF_FACTURES -> produireRecapitulatif(format, du, filtres);
            // Aucun tiers payant ni statut imposé : l'état porte sur toutes les factures de la période.
            case RAPPROCHEMENT_TIERS_PAYANTS -> format == FormatExport.XLSX
                ? rapprochementService.exportExcel(new RapprochementParams(null, du, au, null))
                : rapprochementService.exportPdf(new RapprochementParams(null, du, au, null));
            default -> throw new IllegalArgumentException("Édition hors des éditions partenaires : " + rapport);
        };
    }
    /** Le récapitulatif est mensuel : c'est le mois du début de la période qui le désigne. */
    private byte[] produireRecapitulatif(FormatExport format, LocalDate du, Map<FiltreExport, List<String>> filtres) {
        RecapitulatifMensuelParams params = new RecapitulatifMensuelParams(
            du.getYear(),
            du.getMonthValue(),
            null,
            null,
            ReglagesRapports.lireUnique(filtres, FiltreExport.TYPE_FACTURE, TypeFacture.class)
        );
        return format == FormatExport.XLSX
            ? recapitulatifMensuelService.exportExcel(params)
            : recapitulatifMensuelService.exportPdf(params);
    }
}

package com.kobe.warehouse.service.exports.impl;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import com.kobe.warehouse.domain.enumeration.SectionExportComptable;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.activity_summary.ActivitySummaryService;
import com.kobe.warehouse.service.ap.ExportComptableService;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseService;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienService;
import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import com.kobe.warehouse.service.financiel_transaction.dto.ModeChiffreAffaire;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import com.kobe.warehouse.service.report.pdf.CashRegisterPdfReportService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Éditions des écrans Comptabilité, Mouvements de caisse et Retraitement du CA. Les trois états comptables se lisent sur deux
 * chiffres d'affaires, l'encaissé et le déclaré : c'est le réglage {@link FiltreExport#MODE_CA}, le paramètre {@code mode} que
 * leurs écrans passent déjà à leur service.
 */
@Component
class ProducteurRapportsComptables {

    /** Les défauts des écrans d'origine, repris de la valeur par défaut de leurs contrôleurs. */
    private static final String DECOUPAGE_TABLEAU_PAR_DEFAUT = "daily";
    private static final String GROUPEMENT_TVA_PAR_DEFAUT = "codeTva";

    private final ExportComptableService exportComptableService;
    private final BalanceCaisseService balanceCaisseService;
    private final TableauPharmacienService tableauPharmacienService;
    private final TaxeService taxeService;
    private final CashRegisterPdfReportService cashRegisterPdfReportService;
    private final ActivitySummaryService activitySummaryService;
    private final FinancialTransactionService financialTransactionService;
    private final ReglementDiffereService reglementDiffereService;

    ProducteurRapportsComptables(
        ExportComptableService exportComptableService,
        BalanceCaisseService balanceCaisseService,
        TableauPharmacienService tableauPharmacienService,
        TaxeService taxeService,
        CashRegisterPdfReportService cashRegisterPdfReportService,
        ActivitySummaryService activitySummaryService,
        FinancialTransactionService financialTransactionService,
        ReglementDiffereService reglementDiffereService
    ) {
        this.exportComptableService = exportComptableService;
        this.balanceCaisseService = balanceCaisseService;
        this.tableauPharmacienService = tableauPharmacienService;
        this.taxeService = taxeService;
        this.cashRegisterPdfReportService = cashRegisterPdfReportService;
        this.activitySummaryService = activitySummaryService;
        this.financialTransactionService = financialTransactionService;
        this.reglementDiffereService = reglementDiffereService;
    }

    byte[] produire(RapportExport rapport, FormatExport format, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        return switch (rapport) {
            case EXPORT_COMPTABLE -> produireComptable(format, du, au, filtres);
            case BALANCE_CAISSE -> balanceCaisseService.exportToPdf(ecrireMvtParam(du, au, filtres, null, null));
            case TABLEAU_PHARMACIEN -> produireTableauPharmacien(format, du, au, filtres);
            case RAPPORT_TVA -> taxeService.exportToPdf(
                ecrireMvtParam(du, au, filtres, FiltreExport.GROUPEMENT_TVA, GROUPEMENT_TVA_PAR_DEFAUT)
            );
            // La déclaration ne connaît que sa période : son service n'attend ni mode ni regroupement.
            case DECLARATION_TVA -> taxeService.exportDeclarationToPdf(new MvtParam().setFromDate(du).setToDate(au).build());
            // Le récapitulatif porte sur une journée : c'est le début de la période qui la désigne.
            case RECAPITULATIF_CAISSE -> cashRegisterPdfReportService.export(du);
            case RAPPORT_ACTIVITE -> activitySummaryService.printToPdf(du, au, null, null);
            case MOUVEMENTS_CAISSE_ETAT -> financialTransactionService.exportToPdf(ecrireFiltreTransactions(du, au, filtres));
            // Tous les clients, et seuls les différés qui restent dus : c'est ce que l'écran imprime.
            case DIFFERES_IMPAYES -> format == FormatExport.XLSX
                ? reglementDiffereService.exportDifferesToExcel(null, Set.of(PaymentStatus.IMPAYE))
                : reglementDiffereService.printListToPdf(null, Set.of(PaymentStatus.IMPAYE));
            default -> throw new IllegalArgumentException("Édition hors des éditions comptables : " + rapport);
        };
    }

    /** Aucune feuille retenue : les six, comme le défaut de l'écran Mouvements de caisse. */
    private byte[] produireComptable(FormatExport format, LocalDate du, LocalDate au, Map<FiltreExport, List<String>> filtres) {
        List<SectionExportComptable> sections = ReglagesRapports.lireListe(
            filtres,
            FiltreExport.SECTIONS_COMPTABLES,
            SectionExportComptable.class
        );
        boolean toutes = sections.isEmpty();
        try {
            return exportComptableService.export(
                du,
                au,
                nommerFormat(format),
                toutes || sections.contains(SectionExportComptable.VENTES),
                toutes || sections.contains(SectionExportComptable.ACHATS),
                toutes || sections.contains(SectionExportComptable.MVT_CAISSE),
                toutes || sections.contains(SectionExportComptable.TIERS_PAYANT),
                toutes || sections.contains(SectionExportComptable.DIFFERES),
                toutes || sections.contains(SectionExportComptable.TVA)
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private byte[] produireTableauPharmacien(
        FormatExport format,
        LocalDate du,
        LocalDate au,
        Map<FiltreExport, List<String>> filtres
    ) {
        MvtParam param = ecrireMvtParam(du, au, filtres, FiltreExport.DECOUPAGE_TABLEAU, DECOUPAGE_TABLEAU_PAR_DEFAUT);
        return format == FormatExport.XLSX ? tableauPharmacienService.exportToExcel(param) : tableauPharmacienService.exportToPdf(param);
    }

    /**
     * La requête des états comptables. Ce qui n'a pas été réglé prend le défaut de l'écran : {@code build()} pose la période,
     * les ventes closes et annulées et la catégorie CA, {@code setMode(null)} vaut le CA encaissé, et le regroupement reprend
     * la valeur par défaut du contrôleur de l'écran — un regroupement nul ne voudrait rien dire pour son service.
     */
    private static MvtParam ecrireMvtParam(
        LocalDate du,
        LocalDate au,
        Map<FiltreExport, List<String>> filtres,
        FiltreExport regroupement,
        String regroupementParDefaut
    ) {
        String groupeBy = regroupement == null
            ? null
            : Objects.requireNonNullElse(ReglagesRapports.lireTexte(filtres, regroupement), regroupementParDefaut);
        return new MvtParam()
            .setFromDate(du)
            .setToDate(au)
            .setGroupeBy(groupeBy)
            .setMode(ReglagesRapports.lireUnique(filtres, FiltreExport.MODE_CA, ModeChiffreAffaire.class))
            .build();
    }

    /** Les mouvements de la période ; ce qui n'est pas retenu ne filtre rien, comme à l'écran. */
    private static TransactionFilterDTO ecrireFiltreTransactions(
        LocalDate du,
        LocalDate au,
        Map<FiltreExport, List<String>> filtres
    ) {
        List<TypeFinancialTransaction> types = ReglagesRapports.lireListe(
            filtres,
            FiltreExport.TYPE_TRANSACTION,
            TypeFinancialTransaction.class
        );
        List<String> modes = ReglagesRapports.lireListe(filtres, FiltreExport.MODE_PAIEMENT, String.class);
        return new TransactionFilterDTO(
            du,
            au,
            null,
            null,
            types.isEmpty() ? null : Set.copyOf(types),
            null,
            modes.isEmpty() ? null : Set.copyOf(modes),
            null,
            null,
            null
        );
    }

    /** Les formats que {@code ExportComptableService} connait, en toutes lettres. */
    private static String nommerFormat(FormatExport format) {
        return switch (format) {
            case XLSX -> "excel";
            case CSV -> "csv";
            case PDF -> "pdf";
        };
    }
}

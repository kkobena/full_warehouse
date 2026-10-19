package com.kobe.warehouse.service.exports.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import com.kobe.warehouse.domain.enumeration.RapportExport;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.activity_summary.ActivitySummaryService;
import com.kobe.warehouse.service.ap.ExportComptableService;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import com.kobe.warehouse.service.report.pdf.CashRegisterPdfReportService;
import com.kobe.warehouse.service.financiel_transaction.BalanceCaisseService;
import com.kobe.warehouse.service.financiel_transaction.TableauPharmacienService;
import com.kobe.warehouse.service.financiel_transaction.TaxeService;
import com.kobe.warehouse.service.financiel_transaction.dto.ModeChiffreAffaire;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.pilotage.ExportPilotageService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Les réglages du catalogue deviennent la requête qu'attend le service de l'écran ; rien n'est recalculé ici. */
@ExtendWith(MockitoExtension.class)
@DisplayName("Exports — éditions bâties par le service de leur écran")
class ProducteurRapportsTest {

    private static final LocalDate DU = LocalDate.of(2026, 10, 1);
    private static final LocalDate AU = LocalDate.of(2026, 10, 31);

    @Mock
    private ExportComptableService exportComptableService;

    @Mock
    private ExportPilotageService exportPilotageService;

    @Mock
    private BalanceCaisseService balanceCaisseService;

    @Mock
    private TableauPharmacienService tableauPharmacienService;

    @Mock
    private TaxeService taxeService;

    @Mock
    private CashRegisterPdfReportService cashRegisterPdfReportService;

    @Mock
    private ActivitySummaryService activitySummaryService;

    @Mock
    private FinancialTransactionService financialTransactionService;

    @Mock
    private ReglementDiffereService reglementDiffereService;

    @Captor
    private ArgumentCaptor<TransactionFilterDTO> filtreTransactions;

    @InjectMocks
    private ProducteurRapportsComptables comptables;

    @InjectMocks
    private ProducteurRapportsPilotage pilotage;

    @Captor
    private ArgumentCaptor<RequetePilotageDTO> requetePilotage;

    @Captor
    private ArgumentCaptor<RequeteAnalyseDTO> requeteAnalyse;

    @Captor
    private ArgumentCaptor<RequeteAnneesDTO> requeteAnnees;

    @Captor
    private ArgumentCaptor<MvtParam> mvtParam;

    @Test
    @DisplayName("export comptable : sans feuille retenue, les six ; le format part en toutes lettres")
    void comptableSansFeuille() throws Exception {
        comptables.produire(RapportExport.EXPORT_COMPTABLE, FormatExport.XLSX, DU, AU, Map.of());

        verify(exportComptableService).export(DU, AU, "excel", true, true, true, true, true, true);
    }

    @Test
    @DisplayName("export comptable : seules les feuilles retenues, et le PDF reste le PDF")
    void comptableFeuillesRetenues() throws Exception {
        comptables.produire(
            RapportExport.EXPORT_COMPTABLE,
            FormatExport.PDF,
            DU,
            AU,
            Map.of(FiltreExport.SECTIONS_COMPTABLES, List.of("VENTES", "TVA"))
        );

        verify(exportComptableService).export(DU, AU, "pdf", true, false, false, false, false, true);
    }

    @Test
    @DisplayName("tableau de bord : découpage, comparaison et indicateurs retenus passent dans la requête")
    void tableauDeBordRegle() {
        pilotage.produire(
            RapportExport.PILOTAGE_TABLEAU_DE_BORD,
            DU,
            AU,
            Map.of(
                FiltreExport.GRANULARITE,
                List.of("JOUR"),
                FiltreExport.COMPARAISON,
                List.of("AUCUNE"),
                FiltreExport.INDICATEURS,
                List.of("CA_TTC", "MARGE_BRUTE")
            )
        );

        verify(exportPilotageService).exporterSeries(requetePilotage.capture());
        RequetePilotageDTO requete = requetePilotage.getValue();
        assertThat(requete.du()).isEqualTo(DU);
        assertThat(requete.au()).isEqualTo(AU);
        assertThat(requete.granularite()).isEqualTo(Granularite.JOUR);
        assertThat(requete.comparaison()).isEqualTo(TypeComparaison.AUCUNE);
        assertThat(requete.indicateurs()).containsExactly(IndicateurPilotage.CA_TTC, IndicateurPilotage.MARGE_BRUTE);
    }

    @Test
    @DisplayName("tableau de bord : sans réglage, les défauts de l'écran — mois, même période l'an passé, tous les indicateurs")
    void tableauDeBordParDefaut() {
        pilotage.produire(RapportExport.PILOTAGE_TABLEAU_DE_BORD, DU, AU, Map.of());

        verify(exportPilotageService).exporterSeries(requetePilotage.capture());
        RequetePilotageDTO requete = requetePilotage.getValue();
        assertThat(requete.granularite()).isEqualTo(Granularite.MOIS);
        assertThat(requete.comparaison()).isEqualTo(TypeComparaison.MEME_PERIODE_N_1);
        assertThat(requete.indicateurs()).hasSameSizeAs(IndicateurPilotage.values());
    }

    @Test
    @DisplayName("ventilation : sans réglage, les défauts de l'écran — famille, vingt premiers, tri par valeur")
    void ventilationParDefaut() {
        pilotage.produire(RapportExport.PILOTAGE_ANALYSER, DU, AU, Map.of());

        verify(exportPilotageService).exporterAnalyse(any(), requeteAnalyse.capture());
        RequeteAnalyseDTO requete = requeteAnalyse.getValue();
        assertThat(requete.axe()).isEqualTo(AxeAnalyse.FAMILLE);
        assertThat(requete.top()).isEqualTo(20);
        assertThat(requete.tri()).isEqualTo(TriAnalyse.VALEUR);
    }

    @Test
    @DisplayName("ventilation : axe, nombre d'éléments et tri retenus passent dans la requête ; « Tout » vaut zéro")
    void ventilationReglee() {
        pilotage.produire(
            RapportExport.PILOTAGE_ANALYSER,
            DU,
            AU,
            Map.of(
                FiltreExport.AXE_ANALYSE,
                List.of("LABORATOIRE"),
                FiltreExport.NOMBRE_ELEMENTS,
                List.of("0"),
                FiltreExport.TRI_ANALYSE,
                List.of("ECART_BAISSE")
            )
        );

        verify(exportPilotageService).exporterAnalyse(any(), requeteAnalyse.capture());
        RequeteAnalyseDTO requete = requeteAnalyse.getValue();
        assertThat(requete.axe()).isEqualTo(AxeAnalyse.LABORATOIRE);
        assertThat(requete.top()).isZero();
        assertThat(requete.tri()).isEqualTo(TriAnalyse.ECART_BAISSE);
    }

    @Test
    @DisplayName("comparaison des années : indicateur, nombre d'années et lecture du mois")
    void annees() {
        pilotage.produire(
            RapportExport.PILOTAGE_ANNEES,
            null,
            null,
            Map.of(
                FiltreExport.INDICATEUR_COMPARE,
                List.of("MARGE_BRUTE"),
                FiltreExport.NOMBRE_ANNEES,
                List.of("5"),
                FiltreExport.MODE_ANNEES,
                List.of("CUMULE")
            )
        );

        verify(exportPilotageService).exporterAnnees(requeteAnnees.capture());
        RequeteAnneesDTO requete = requeteAnnees.getValue();
        assertThat(requete.indicateur()).isEqualTo(IndicateurPilotage.MARGE_BRUTE);
        assertThat(requete.annees()).isEqualTo(5);
        assertThat(requete.mode()).isEqualTo(ModeAnnees.CUMULE);
    }
    @Test
    @DisplayName("balance : sans réglage, le CA encaissé et la période demandée")
    void balanceParDefaut() {
        comptables.produire(RapportExport.BALANCE_CAISSE, FormatExport.PDF, DU, AU, Map.of());

        verify(balanceCaisseService).exportToPdf(mvtParam.capture());
        MvtParam param = mvtParam.getValue();
        assertThat(param.getFromDate()).isEqualTo(DU);
        assertThat(param.getToDate()).isEqualTo(AU);
        assertThat(param.getMode()).isEqualTo(ModeChiffreAffaire.REEL);
        assertThat(param.getGroupeBy()).isNull();
    }

    @Test
    @DisplayName("balance : le CA déclaré se demande, comme sur l'écran du retraitement")
    void balanceCaDeclare() {
        comptables.produire(
            RapportExport.BALANCE_CAISSE,
            FormatExport.PDF,
            DU,
            AU,
            Map.of(FiltreExport.MODE_CA, List.of("DECLARE"))
        );

        verify(balanceCaisseService).exportToPdf(mvtParam.capture());
        assertThat(mvtParam.getValue().getMode()).isEqualTo(ModeChiffreAffaire.DECLARE);
    }

    @Test
    @DisplayName("tableau du pharmacien : le PDF et l'Excel sortent du même service, le découpage par défaut est journalier")
    void tableauPharmacien() {
        comptables.produire(RapportExport.TABLEAU_PHARMACIEN, FormatExport.PDF, DU, AU, Map.of());
        comptables.produire(
            RapportExport.TABLEAU_PHARMACIEN,
            FormatExport.XLSX,
            DU,
            AU,
            Map.of(FiltreExport.DECOUPAGE_TABLEAU, List.of("month"))
        );

        verify(tableauPharmacienService).exportToPdf(mvtParam.capture());
        assertThat(mvtParam.getValue().getGroupeBy()).isEqualTo("daily");
        verify(tableauPharmacienService).exportToExcel(mvtParam.capture());
        assertThat(mvtParam.getValue().getGroupeBy()).isEqualTo("month");
    }

    @Test
    @DisplayName("rapport de TVA : regroupé par code de TVA sans réglage")
    void rapportTva() {
        comptables.produire(RapportExport.RAPPORT_TVA, FormatExport.PDF, DU, AU, Map.of());

        verify(taxeService).exportToPdf(mvtParam.capture());
        assertThat(mvtParam.getValue().getGroupeBy()).isEqualTo("codeTva");
    }

    @Test
    @DisplayName("déclaration de TVA : sa période seule, aucun réglage ne la décrit")
    void declarationTva() {
        comptables.produire(RapportExport.DECLARATION_TVA, FormatExport.PDF, DU, AU, Map.of(FiltreExport.MODE_CA, List.of("DECLARE")));

        verify(taxeService).exportDeclarationToPdf(mvtParam.capture());
        MvtParam param = mvtParam.getValue();
        assertThat(param.getFromDate()).isEqualTo(DU);
        assertThat(param.getToDate()).isEqualTo(AU);
        assertThat(param.getGroupeBy()).isNull();
    }
    @Test
    @DisplayName("récapitulatif de caisse : la journée est le début de la période")
    void recapitulatifCaisse() {
        comptables.produire(RapportExport.RECAPITULATIF_CAISSE, FormatExport.PDF, DU, AU, Map.of());

        verify(cashRegisterPdfReportService).export(DU);
    }

    @Test
    @DisplayName("rapport d'activité : la période, sans recherche d'achat ni de règlement")
    void rapportActivite() {
        comptables.produire(RapportExport.RAPPORT_ACTIVITE, FormatExport.PDF, DU, AU, Map.of());

        verify(activitySummaryService).printToPdf(DU, AU, null, null);
    }
    @Test
    @DisplayName("mouvements de caisse : la période, et seuls les réglages retenus filtrent")
    void mouvementsDeCaisse() {
        comptables.produire(
            RapportExport.MOUVEMENTS_CAISSE_ETAT,
            FormatExport.PDF,
            DU,
            AU,
            Map.of(FiltreExport.TYPE_TRANSACTION, List.of("CASH_SALE"), FiltreExport.MODE_PAIEMENT, List.of("CASH"))
        );

        verify(financialTransactionService).exportToPdf(filtreTransactions.capture());
        TransactionFilterDTO filtre = filtreTransactions.getValue();
        assertThat(filtre.fromDate()).isEqualTo(DU);
        assertThat(filtre.toDate()).isEqualTo(AU);
        assertThat(filtre.typeFinancialTransactions()).containsExactly(TypeFinancialTransaction.CASH_SALE);
        assertThat(filtre.paymentModes()).containsExactly("CASH");
        assertThat(filtre.categorieChiffreAffaires()).isNull();
    }

    @Test
    @DisplayName("mouvements de caisse : sans réglage, aucun filtre imposé")
    void mouvementsDeCaisseSansReglage() {
        comptables.produire(RapportExport.MOUVEMENTS_CAISSE_ETAT, FormatExport.PDF, DU, AU, Map.of());

        verify(financialTransactionService).exportToPdf(filtreTransactions.capture());
        assertThat(filtreTransactions.getValue().typeFinancialTransactions()).isNull();
        assertThat(filtreTransactions.getValue().paymentModes()).isNull();
    }

    @Test
    @DisplayName("différés impayés : tous les clients, les seuls impayés, en PDF comme en tableur")
    void differesImpayes() {
        comptables.produire(RapportExport.DIFFERES_IMPAYES, FormatExport.PDF, null, null, Map.of());
        comptables.produire(RapportExport.DIFFERES_IMPAYES, FormatExport.XLSX, null, null, Map.of());

        verify(reglementDiffereService).printListToPdf(null, Set.of(PaymentStatus.IMPAYE));
        verify(reglementDiffereService).exportDifferesToExcel(null, Set.of(PaymentStatus.IMPAYE));
    }
}

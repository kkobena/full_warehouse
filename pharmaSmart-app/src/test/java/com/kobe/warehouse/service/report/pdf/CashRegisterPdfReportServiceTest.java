package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.service.dto.report.DailyCashRegisterReportDTO;
import com.kobe.warehouse.service.report.CashRegisterReportService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le rapport de caisse quotidien réunit toutes les caisses de la journée sur un document. Il porte
 * deux totaux que personne ne fournit au service : le <b>chiffre encaissé</b> de la journée, et
 * surtout la <b>somme des écarts</b> — ce qui manque ou déborde, toutes caisses confondues.
 *
 * <p>Cette somme d'écarts est la ligne que le pharmacien regarde en premier. Elle obéit à deux
 * règles faciles à manquer : elle ne compte que les caisses <b>fermées</b>, une caisse encore
 * ouverte n'ayant pas été comptée ; et elle additionne des <b>valeurs absolues</b>, sans quoi un
 * manquant de 10 000 dans une caisse et un excédent de 10 000 dans une autre s'annuleraient en un
 * rassurant zéro — alors que deux caisses sont fausses.
 */
@DisplayName("CashRegisterPdfReportService — rapport de caisse quotidien")
class CashRegisterPdfReportServiceTest {

    private CashRegisterReportService cashRegisterReportService;
    private CashRegisterPdfReportService service;

    @BeforeEach
    void setUp() {
        cashRegisterReportService = mock(CashRegisterReportService.class);
        service = new CashRegisterPdfReportService(
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits(),
            cashRegisterReportService
        );
    }

    @Test
    @DisplayName("le chiffre encaissé totalise toutes les caisses de la journée")
    void totalEncaisse() {
        caisses(caisse(300_000, 0, true), caisse(150_000, 0, true), caisse(50_000, 0, false));

        assertThat(modele().get("totalSales")).isEqualTo(500_000);
    }

    /**
     * Un manquant et un excédent de même montant ne s'annulent pas : deux caisses fausses restent
     * deux caisses fausses, et le total doit le dire.
     */
    @Test
    @DisplayName("les écarts s'additionnent en valeur absolue, sans se compenser")
    void ecartsEnValeurAbsolue() {
        caisses(caisse(100_000, -10_000, true), caisse(100_000, 10_000, true));

        assertThat(modele().get("totalDiscrepancy")).isEqualTo(20_000);
    }

    /** Une caisse encore ouverte n'a pas été comptée : son écart n'existe pas encore. */
    @Test
    @DisplayName("une caisse encore ouverte ne pèse pas dans les écarts")
    void caisseOuverteHorsEcarts() {
        caisses(caisse(100_000, -10_000, true), caisse(100_000, 999_000, false));

        assertThat(modele().get("totalDiscrepancy")).isEqualTo(10_000);
    }

    @Test
    @DisplayName("des montants absents sont traités comme zéro")
    void montantsAbsents() {
        caisses(caisse(null, null, true));

        assertThat(modele().get("totalSales")).isEqualTo(0);
        assertThat(modele().get("totalDiscrepancy")).isEqualTo(0);
    }

    @Test
    @DisplayName("la date, le titre et les mentions légales accompagnent le document")
    void modeleDuDocument() {
        caisses(caisse(100_000, 0, true));

        Map<String, Object> modele = modele();

        assertThat(modele.get("reportDate")).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(modele.get("reportTitle")).isEqualTo("Rapport de Caisse Quotidien");
        assertThat((List<?>) modele.get("dailyReports")).hasSize(1);
        assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
    }

    @Test
    @DisplayName("une journée sans caisse s'imprime avec des totaux nuls")
    void journeeSansCaisse() {
        caisses();

        assertThat(modele().get("totalSales")).isEqualTo(0);
        assertThat(modele().get("totalDiscrepancy")).isEqualTo(0);
        assertThat((List<?>) modele().get("dailyReports")).isEmpty();
    }

    // ===== fabriques =====

    private void caisses(DailyCashRegisterReportDTO... caisses) {
        when(cashRegisterReportService.getDailyReport(any())).thenReturn(List.of(caisses));
    }

    private Map<String, Object> modele() {
        service.export(LocalDate.of(2026, 3, 31));
        return service.getParameters();
    }

    private static DailyCashRegisterReportDTO caisse(Integer totalVentes, Integer ecart, boolean fermee) {
        return new DailyCashRegisterReportDTO(
            1,
            "Caisse 1",
            LocalDate.of(2026, 3, 31),
            null,
            null,
            0,
            0,
            0,
            ecart,
            totalVentes,
            0,
            List.of(),
            "System System",
            fermee
        );
    }
}

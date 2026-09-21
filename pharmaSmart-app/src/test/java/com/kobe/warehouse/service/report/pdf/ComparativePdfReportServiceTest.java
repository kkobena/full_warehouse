package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.service.dto.report.ComparativeByTypeDTO;
import com.kobe.warehouse.service.dto.report.ComparativeCADTO;
import com.kobe.warehouse.service.dto.report.ComparativeSummaryDTO;
import com.kobe.warehouse.service.report.ComparativeReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le rapport comparatif confronte une année à la précédente. Il s'imprime à trois mailles — par
 * mois, par trimestre, ou année contre année — et c'est le seul générateur PDF du logiciel qui
 * <b>choisit sa source de données</b> selon un paramètre venu de l'écran.
 *
 * <p>Ce choix est ce qu'on éprouve ici. Une maille mal aiguillée produit un document qui a l'air
 * juste — les mêmes colonnes, la même mise en page — mais compare des trimestres en les annonçant
 * comme des mois. La comparaison pluriannuelle a en outre sa propre fenêtre, cinq ans en arrière,
 * qui n'apparaît nulle part ailleurs.
 */
@DisplayName("ComparativePdfReportService — rapport comparatif des ventes")
class ComparativePdfReportServiceTest {

    private ComparativeReportService comparativeReportService;
    private ComparativePdfReportService service;

    @BeforeEach
    void setUp() {
        comparativeReportService = mock(ComparativeReportService.class);
        when(comparativeReportService.getComparativeSummary()).thenReturn(resume());
        when(comparativeReportService.getMonthlyComparison(any())).thenReturn(List.of(periode("2026-01")));
        when(comparativeReportService.getQuarterlyComparison(any())).thenReturn(List.of(periode("2026-Q1")));
        when(comparativeReportService.getYearlyComparison(any(), any())).thenReturn(List.of(periode("2026")));
        when(comparativeReportService.getComparisonBySalesType(any(), any())).thenReturn(List.of(parType()));

        service = new ComparativePdfReportService(
            PdfReportTestSupport.proprietes(),
            PdfReportTestSupport.storageService(),
            PdfReportTestSupport.moteurDeGabarits(),
            comparativeReportService
        );
    }

    @Test
    @DisplayName("la maille mensuelle interroge la comparaison par mois")
    void mailleMensuelle() {
        Map<String, Object> modele = modele("MONTHLY", 2026);

        verify(comparativeReportService).getMonthlyComparison(2026);
        verify(comparativeReportService, never()).getQuarterlyComparison(any());
        assertThat(libelles(modele)).containsExactly("2026-01");
    }

    @Test
    @DisplayName("la maille trimestrielle interroge la comparaison par trimestre")
    void mailleTrimestrielle() {
        Map<String, Object> modele = modele("QUARTERLY", 2026);

        verify(comparativeReportService).getQuarterlyComparison(2026);
        assertThat(libelles(modele)).containsExactly("2026-Q1");
    }

    /** La comparaison pluriannuelle remonte cinq ans en arrière — une fenêtre propre à cette maille. */
    @Test
    @DisplayName("la maille annuelle couvre les six dernières années")
    void mailleAnnuelle() {
        modele("YEARLY", 2026);

        verify(comparativeReportService).getYearlyComparison(LocalDate.of(2021, 1, 1), LocalDate.of(2026, 12, 31));
    }

    /** La casse vient de l'écran : elle ne doit pas décider de la maille. */
    @Test
    @DisplayName("la maille est reconnue quelle que soit la casse")
    void casseIndifferente() {
        modele("quarterly", 2026);

        verify(comparativeReportService).getQuarterlyComparison(2026);
    }

    /** Une maille inconnue ne doit pas produire un document vide : elle retombe sur le mois. */
    @Test
    @DisplayName("une maille inconnue retombe sur la comparaison mensuelle")
    void mailleInconnue() {
        Map<String, Object> modele = modele("HEBDOMADAIRE", 2026);

        verify(comparativeReportService).getMonthlyComparison(2026);
        assertThat(libelles(modele)).containsExactly("2026-01");
    }

    @Test
    @DisplayName("la comparaison par type de vente confronte l'année à la précédente")
    void comparaisonParType() {
        Map<String, Object> modele = modele("MONTHLY", 2026);

        verify(comparativeReportService).getComparisonBySalesType(2026, 2025);
        assertThat((List<?>) modele.get("byType")).hasSize(1);
    }

    @Test
    @DisplayName("la maille, l'année et les mentions légales accompagnent le document")
    void modeleDuDocument() {
        Map<String, Object> modele = modele("MONTHLY", 2026);

        assertThat(modele.get("comparisonType")).isEqualTo("MONTHLY");
        assertThat(modele.get("year")).isEqualTo(2026);
        assertThat(modele.get("reportTitle")).isEqualTo("Rapport Comparatif des Ventes");
        assertThat(modele).containsKey("summary");
        assertThat(modele.get("footer").toString()).contains("RC N° " + PdfReportTestSupport.REGISTRE);
    }

    /**
     * La maille arrive d'un paramètre de requête : absente, elle fait échouer l'impression sur une
     * comparaison de chaîne. Le test fige le comportement d'aujourd'hui — un appel sans maille ne
     * produit pas de document silencieusement faux.
     */
    @Test
    @DisplayName("une maille absente interrompt l'impression plutôt que de deviner")
    void mailleAbsente() {
        assertThatThrownBy(() -> service.export(null, 2026)).isInstanceOf(NullPointerException.class);
    }

    // ===== fabriques =====

    private Map<String, Object> modele(String maille, int annee) {
        service.export(maille, annee);
        return service.getParameters();
    }

    private static List<String> libelles(Map<String, Object> modele) {
        return ((List<?>) modele.get("comparisons")).stream().map(d -> ((ComparativeCADTO) d).periodLabel()).toList();
    }

    private static ComparativeCADTO periode(String libelle) {
        return new ComparativeCADTO(
            LocalDate.of(2026, 1, 1), libelle, 100_000L, 90_000L, BigDecimal.TEN, BigDecimal.TEN, 10, 9, "MONTHLY"
        );
    }

    private static ComparativeByTypeDTO parType() {
        return new ComparativeByTypeDTO("VNO", "Vente Normale Officine", 100_000L, 90_000L, BigDecimal.TEN, 10, 9);
    }

    private static ComparativeSummaryDTO resume() {
        return new ComparativeSummaryDTO(
            0L, 0L, BigDecimal.ZERO, 0L, 0L, BigDecimal.ZERO, "Janvier", 0L, "Février", 0L, BigDecimal.ZERO, BigDecimal.ZERO
        );
    }
}

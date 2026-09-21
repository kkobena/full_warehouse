package com.kobe.warehouse.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.StockAlertType;
import com.kobe.warehouse.service.dashboard.impl.ResponsableCommandeDashboardServiceImpl;
import com.kobe.warehouse.service.dashboard.mapper.DashboardDTOMapper;
import com.kobe.warehouse.service.dto.dashboard.AnalyseABCDTO;
import com.kobe.warehouse.service.dto.dashboard.PerformanceFournisseurDTO;
import com.kobe.warehouse.service.dto.dashboard.StockAlertsDTO;
import com.kobe.warehouse.service.dto.report.ABCParetoSummaryDTO;
import com.kobe.warehouse.service.dto.report.StockAlertDTO;
import com.kobe.warehouse.service.dto.report.SupplierPerformanceDTO;
import com.kobe.warehouse.service.report.ABCParetoReportService;
import com.kobe.warehouse.service.report.StockAlertReportService;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Le tableau de bord du responsable des commandes est en partie un assembleur : trois de ses tuiles
 * ne calculent rien, elles reformulent ce que les services de rapport savent déjà. Ce sont ces
 * trois-là qu'on éprouve ici — les quatre autres tiennent dans du SQL natif et sont couvertes par
 * {@code ResponsableCommandeDashboardServiceIntegrationTest}, où une vraie base peut répondre.
 *
 * <p>Deux choses s'y jouent malgré tout. La <b>pagination des alertes</b> : le tableau de bord
 * compte des alertes, il lui faut donc toutes les lignes et pas la première page — une page
 * tronquée afficherait un stock plus sain qu'il n'est. Et le <b>nombre de fournisseurs demandé</b>,
 * dont l'absence doit retomber sur un défaut plutôt que de descendre {@code null} jusqu'au
 * {@code LIMIT} de la requête.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResponsableCommandeDashboardService — tuiles déléguées aux services de rapport")
class ResponsableCommandeDashboardServiceImplTest {

    @Mock
    private StockAlertReportService stockAlertReportService;

    @Mock
    private ABCParetoReportService abcParetoReportService;

    @Mock
    private SupplierPerformanceReportService supplierPerformanceReportService;

    private ResponsableCommandeDashboardServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ResponsableCommandeDashboardServiceImpl(
            stockAlertReportService,
            abcParetoReportService,
            supplierPerformanceReportService,
            new DashboardDTOMapper()
        );
    }

    // ===== alertes de stock =====

    @Test
    @DisplayName("les alertes sont comptées par type")
    void alertesCompteesParType() {
        when(stockAlertReportService.getStockAlerts(isNull(), any(Pageable.class))).thenReturn(
            page(alerte(1, StockAlertType.RUPTURE), alerte(2, StockAlertType.ALERTE), alerte(3, StockAlertType.ALERTE))
        );

        StockAlertsDTO resultat = service.getStockAlerts();

        assertThat(resultat.rupture()).isEqualTo(1);
        assertThat(resultat.stockCritique()).isEqualTo(2);
    }

    /**
     * Le service de rapport pagine par défaut. Demander une page bornée ferait compter les alertes
     * d'une seule page : la tuile annoncerait vingt ruptures là où l'officine en a deux cents.
     */
    @Test
    @DisplayName("demande toutes les alertes, sans pagination, et sans filtrer par type")
    void demandeToutesLesAlertes() {
        when(stockAlertReportService.getStockAlerts(isNull(), any(Pageable.class))).thenReturn(page());

        service.getStockAlerts();

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(stockAlertReportService).getStockAlerts(isNull(), pageable.capture());
        assertThat(pageable.getValue().isUnpaged()).isTrue();
    }

    // ===== analyse ABC =====

    @Test
    @DisplayName("l'analyse ABC est reprise du service de rapport")
    void analyseABCRepriseDuRapport() {
        when(abcParetoReportService.getABCParetoSummary()).thenReturn(resumeABC());

        AnalyseABCDTO resultat = service.getAnalyseABC();

        assertThat(resultat.classeA().nombreProduits()).isEqualTo(10);
        assertThat(resultat.classeA().valeur()).isEqualTo(8_000_000L);
        assertThat(resultat.classeC().nombreProduits()).isEqualTo(60);
    }

    @Test
    @DisplayName("sans analyse disponible, la tuile reçoit null")
    void analyseABCAbsente() {
        when(abcParetoReportService.getABCParetoSummary()).thenReturn(null);

        assertThat(service.getAnalyseABC()).isNull();
    }

    // ===== performance fournisseurs =====

    @Test
    @DisplayName("le classement fournisseurs est converti en notes sur cinq")
    void classementFournisseursConverti() {
        when(supplierPerformanceReportService.getTopSuppliersByVolume(3)).thenReturn(
            List.of(fournisseur(1, "LABOREX", BigDecimal.valueOf(95)), fournisseur(2, "DPCI", BigDecimal.valueOf(45)))
        );

        List<PerformanceFournisseurDTO> resultat = service.getPerformanceFournisseurs(3);

        assertThat(resultat).extracting(PerformanceFournisseurDTO::fournisseurName).containsExactly("LABOREX", "DPCI");
        assertThat(resultat).extracting(PerformanceFournisseurDTO::note).containsExactly(5, 2);
    }

    @Test
    @DisplayName("sans nombre demandé, cinq fournisseurs")
    void topParDefautACinq() {
        when(supplierPerformanceReportService.getTopSuppliersByVolume(5)).thenReturn(List.of());

        service.getPerformanceFournisseurs(null);

        verify(supplierPerformanceReportService).getTopSuppliersByVolume(5);
    }

    // ===== fabriques =====

    private static Page<StockAlertDTO> page(StockAlertDTO... alertes) {
        return new PageImpl<>(List.of(alertes));
    }

    private static StockAlertDTO alerte(int produitId, StockAlertType type) {
        return new StockAlertDTO(produitId, "PRODUIT " + produitId, "CIP" + produitId, 0, 5, LocalDate.now().plusMonths(1), type);
    }

    private static ABCParetoSummaryDTO resumeABC() {
        return new ABCParetoSummaryDTO(
            100,
            10_000_000L,
            5,
            4_000_000L,
            BigDecimal.valueOf(40),
            10,
            8_000_000L,
            BigDecimal.valueOf(78.5),
            30,
            1_500_000L,
            BigDecimal.valueOf(16.2),
            60,
            500_000L,
            BigDecimal.valueOf(5.3),
            0,
            0L,
            BigDecimal.ZERO
        );
    }

    private static SupplierPerformanceDTO fournisseur(int id, String nom, BigDecimal score) {
        return new SupplierPerformanceDTO(
            id,
            nom,
            "F" + id,
            "0100000000",
            "0700000000",
            2,
            10_000_000L,
            24,
            120_000_000L,
            3,
            1,
            10,
            BigDecimal.valueOf(95),
            score
        );
    }
}

package com.kobe.warehouse.service.dashboard.integration;

import static org.mockito.Mockito.mock;

import com.kobe.warehouse.repository.CashRegisterItemRepository;
import com.kobe.warehouse.repository.CashRegisterRepository;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.dashboard.CaissierDashboardService;
import com.kobe.warehouse.service.dashboard.ResponsableCommandeDashboardService;
import com.kobe.warehouse.service.dashboard.impl.CaissierDashboardServiceImpl;
import com.kobe.warehouse.service.dashboard.impl.ResponsableCommandeDashboardServiceImpl;
import com.kobe.warehouse.service.dashboard.mapper.DashboardDTOMapper;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.id_generator.OrderLineIdGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.report.ABCParetoReportService;
import com.kobe.warehouse.service.report.StockAlertReportService;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Les deux services de tableau de bord, câblés à la main sur le PostgreSQL du conteneur.
 *
 * <p>Tout ce qui compte ici est <b>réel</b> : les repositories, donc les requêtes natives qu'ils
 * portent, et l'{@link EntityManager} par lequel le tableau de bord du responsable envoie son
 * propre SQL. C'est le seul montage où ces requêtes sont réellement exécutées — un nom de colonne
 * erroné, une valeur d'énumération absente de la contrainte {@code CHECK} ou une jointure sur une
 * table partitionnée ne se voient nulle part ailleurs.
 *
 * <p>Sont <b>simulés</b> les trois services de rapport dont dépend le tableau de bord du
 * responsable : ils lisent des vues matérialisées rafraîchies par des jobs, et ce que le tableau de
 * bord en fait — compter, noter, reformuler — est déjà éprouvé sans base par les tests unitaires.
 */
final class DashboardServicesUnderTest {

    // --- collaborateurs simulés ---
    final StockAlertReportService stockAlertReportService = mock(StockAlertReportService.class);
    final ABCParetoReportService abcParetoReportService = mock(ABCParetoReportService.class);
    final SupplierPerformanceReportService supplierPerformanceReportService = mock(SupplierPerformanceReportService.class);

    // --- repositories réels ---
    final CashRegisterRepository cashRegisterRepository;
    final CashRegisterItemRepository cashRegisterItemRepository;
    final SalesRepository salesRepository;
    final CommandeRepository commandeRepository;

    // --- générateurs d'identifiants composites ---
    final SaleIdGeneratorService saleIdGeneratorService;
    final SaleLineIdGeneratorService saleLineIdGeneratorService;
    final CommandeIdGeneratorService commandeIdGeneratorService;
    final OrderLineIdGeneratorService orderLineIdGeneratorService;

    // --- services sous test ---
    final CaissierDashboardService caissierDashboardService;
    final ResponsableCommandeDashboardService responsableCommandeDashboardService;

    DashboardServicesUnderTest(EntityManager entityManager) {
        this.cashRegisterRepository = IntegrationPostgresDatabase.bean(CashRegisterRepository.class);
        this.cashRegisterItemRepository = IntegrationPostgresDatabase.bean(CashRegisterItemRepository.class);
        this.salesRepository = IntegrationPostgresDatabase.bean(SalesRepository.class);
        this.commandeRepository = IntegrationPostgresDatabase.bean(CommandeRepository.class);

        this.saleIdGeneratorService = new SaleIdGeneratorService(entityManager);
        this.saleLineIdGeneratorService = new SaleLineIdGeneratorService(entityManager);
        this.commandeIdGeneratorService = new CommandeIdGeneratorService(entityManager);
        this.orderLineIdGeneratorService = new OrderLineIdGeneratorService(entityManager);

        this.caissierDashboardService = new CaissierDashboardServiceImpl(
            cashRegisterRepository,
            cashRegisterItemRepository,
            salesRepository,
            commandeRepository
        );

        ResponsableCommandeDashboardServiceImpl responsable = new ResponsableCommandeDashboardServiceImpl(
            stockAlertReportService,
            abcParetoReportService,
            supplierPerformanceReportService,
            new DashboardDTOMapper()
        );
        // L'EntityManager est injecté par @PersistenceContext en production ; hors conteneur Spring
        // il faut le poser soi-même, sans quoi toutes les tuiles en SQL natif tomberaient sur null.
        ReflectionTestUtils.setField(responsable, "entityManager", entityManager);
        this.responsableCommandeDashboardService = responsable;
    }
}

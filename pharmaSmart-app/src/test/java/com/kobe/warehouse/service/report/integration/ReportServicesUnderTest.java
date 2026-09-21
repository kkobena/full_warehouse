package com.kobe.warehouse.service.report.integration;

import static org.mockito.Mockito.mock;

import com.kobe.warehouse.repository.MvStockAlertRepository;
import com.kobe.warehouse.repository.MvStockValuationRayonViewRepository;
import com.kobe.warehouse.repository.MvStockValuationViewRepository;
import com.kobe.warehouse.repository.SupplierEvolutionRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.id_generator.FactureIdGeneratorService;
import com.kobe.warehouse.service.id_generator.OrderLineIdGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.report.ABCParetoReportService;
import com.kobe.warehouse.service.report.ABCParetoReportServiceImpl;
import com.kobe.warehouse.service.report.StockAlertReportService;
import com.kobe.warehouse.service.report.StockAlertReportServiceImpl;
import com.kobe.warehouse.service.report.MargeReportService;
import com.kobe.warehouse.service.report.MargeReportServiceImpl;
import com.kobe.warehouse.service.report.CashRegisterReportService;
import com.kobe.warehouse.service.report.CashRegisterReportServiceImpl;
import com.kobe.warehouse.service.report.ClientRetentionReportService;
import com.kobe.warehouse.service.report.ClientRetentionReportServiceImpl;
import com.kobe.warehouse.service.report.CustomerSegmentationReportService;
import com.kobe.warehouse.service.report.DemarqueReportService;
import com.kobe.warehouse.service.report.DemarqueReportServiceImpl;
import com.kobe.warehouse.service.report.CustomerSegmentationReportServiceImpl;
import com.kobe.warehouse.service.report.MarketBasketAnalysisService;
import com.kobe.warehouse.service.report.MarketBasketAnalysisServiceImpl;
import com.kobe.warehouse.service.report.RecapProduitVenduService;
import com.kobe.warehouse.service.report.RecapProduitVenduServiceImpl;
import com.kobe.warehouse.service.report.SalesForecastService;
import com.kobe.warehouse.service.report.SalesForecastServiceImpl;
import com.kobe.warehouse.service.report.SalesSummaryReportService;
import com.kobe.warehouse.service.report.SalesSummaryReportServiceImpl;
import com.kobe.warehouse.service.report.StockRotationReportService;
import com.kobe.warehouse.service.report.StockRotationReportServiceImpl;
import com.kobe.warehouse.service.report.StockValuationReportService;
import com.kobe.warehouse.service.report.TiersPayantReportService;
import com.kobe.warehouse.service.report.TiersPayantReportServiceImpl;
import com.kobe.warehouse.service.report.StockValuationReportServiceImpl;
import com.kobe.warehouse.service.report.SupplierPerformanceReportService;
import com.kobe.warehouse.service.report.TopProductsReportService;
import com.kobe.warehouse.service.report.TopProductsReportServiceImpl;
import com.kobe.warehouse.service.report.SupplierPerformanceReportServiceImpl;
import com.kobe.warehouse.service.inventaire.InventaireService;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import com.kobe.warehouse.service.report.pdf.ProfitabilityPdfReportService;
import com.kobe.warehouse.service.report.pdf.RecapProduitInvenduPdfService;
import com.kobe.warehouse.service.report.pdf.RecapProduitVenduPdfService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Les services de rapport, câblés à la main sur le PostgreSQL du conteneur.
 *
 * <p>Tout y est <b>réel</b> : l'{@link EntityManager} par lequel partent les requêtes natives, les
 * repositories, et les vues que les migrations ont créées. Rien n'est simulé — un rapport n'a pas
 * de collaborateur à simuler, il n'a qu'une base à interroger, et c'est précisément l'accord entre
 * ses requêtes et le schéma qu'on vient éprouver ici.
 *
 * <p>Le cache de Spring est absent de ce montage : les annotations {@code @Cacheable} portées par
 * les services ne s'appliquent qu'à travers un proxy. C'est voulu — le cache masquerait la seconde
 * lecture et, avec elle, l'effet de tout changement de jeu d'essai au sein d'un même test.
 */
final class ReportServicesUnderTest {

    // --- générateurs d'identifiants composites ---
    final SaleIdGeneratorService saleIdGeneratorService;
    final SaleLineIdGeneratorService saleLineIdGeneratorService;
    final CommandeIdGeneratorService commandeIdGeneratorService;
    final OrderLineIdGeneratorService orderLineIdGeneratorService;
    final FactureIdGeneratorService factureIdGeneratorService;
    final AssuranceItemIdGeneratorService assuranceItemIdGeneratorService;

    // --- repositories réels ---
    final MvStockAlertRepository mvStockAlertRepository;
    final SupplierEvolutionRepository supplierEvolutionRepository;
    final MvStockValuationViewRepository mvStockValuationViewRepository;
    final MvStockValuationRayonViewRepository mvStockValuationRayonViewRepository;

    /**
     * Seul collaborateur simulé du paquet : la valorisation du stock se cloisonne par magasin, et
     * le magasin se lit sur l'utilisateur connecté. Hors contexte de sécurité, c'est le test qui
     * dit de quel magasin il parle.
     */
    final UserService userService = mock(UserService.class);

    /**
     * Le rendu PDF est simulé : ce qui compte n'est pas le document produit mais ce que l'export
     * lui transmet — le résumé et les lignes filtrées, là où il ne passait autrefois rien.
     */
    final ProfitabilityPdfReportService profitabilityPdfReportService = mock(ProfitabilityPdfReportService.class);

    // --- services sous test ---
    final ABCParetoReportService abcParetoReportService;
    final SupplierPerformanceReportService supplierPerformanceReportService;
    final StockAlertReportService stockAlertReportService;
    final StockRotationReportService stockRotationReportService;
    final StockValuationReportService stockValuationReportService;
    final SalesSummaryReportService salesSummaryReportService;
    final TopProductsReportService topProductsReportService;
    final MargeReportService margeReportService;
    final SalesForecastService salesForecastService;
    final RecapProduitVenduService recapProduitVenduService;
    final ClientRetentionReportService clientRetentionReportService;
    final CustomerSegmentationReportService customerSegmentationReportService;
    final CashRegisterReportService cashRegisterReportService;
    final DemarqueReportService demarqueReportService;
    final MarketBasketAnalysisService marketBasketAnalysisService;
    final TiersPayantReportService tiersPayantReportService;

    ReportServicesUnderTest(EntityManager entityManager) {
        this.saleIdGeneratorService = new SaleIdGeneratorService(entityManager);
        this.saleLineIdGeneratorService = new SaleLineIdGeneratorService(entityManager);
        this.commandeIdGeneratorService = new CommandeIdGeneratorService(entityManager);
        this.orderLineIdGeneratorService = new OrderLineIdGeneratorService(entityManager);
        this.factureIdGeneratorService = new FactureIdGeneratorService(entityManager);
        this.assuranceItemIdGeneratorService = new AssuranceItemIdGeneratorService(entityManager);

        this.mvStockAlertRepository = IntegrationPostgresDatabase.bean(MvStockAlertRepository.class);
        this.supplierEvolutionRepository = new SupplierEvolutionRepository(entityManager);

        ABCParetoReportServiceImpl abcPareto = new ABCParetoReportServiceImpl();
        // L'EntityManager est injecté par @PersistenceContext en production ; hors conteneur Spring
        // il faut le poser soi-même.
        ReflectionTestUtils.setField(abcPareto, "entityManager", entityManager);
        this.abcParetoReportService = abcPareto;

        this.supplierPerformanceReportService = new SupplierPerformanceReportServiceImpl(entityManager, supplierEvolutionRepository);
        this.stockAlertReportService = new StockAlertReportServiceImpl(mvStockAlertRepository);

        StockRotationReportServiceImpl stockRotation = new StockRotationReportServiceImpl();
        ReflectionTestUtils.setField(stockRotation, "entityManager", entityManager);
        this.stockRotationReportService = stockRotation;

        this.mvStockValuationViewRepository = IntegrationPostgresDatabase.bean(MvStockValuationViewRepository.class);
        this.mvStockValuationRayonViewRepository = IntegrationPostgresDatabase.bean(MvStockValuationRayonViewRepository.class);
        this.stockValuationReportService = new StockValuationReportServiceImpl(
            entityManager,
            mvStockValuationViewRepository,
            mvStockValuationRayonViewRepository,
            userService
        );

        SalesSummaryReportServiceImpl salesSummary = new SalesSummaryReportServiceImpl();
        ReflectionTestUtils.setField(salesSummary, "entityManager", entityManager);
        this.salesSummaryReportService = salesSummary;

        TopProductsReportServiceImpl topProducts = new TopProductsReportServiceImpl();
        ReflectionTestUtils.setField(topProducts, "entityManager", entityManager);
        this.topProductsReportService = topProducts;

        this.margeReportService = new MargeReportServiceImpl(profitabilityPdfReportService, entityManager);
        this.salesForecastService = new SalesForecastServiceImpl(entityManager);

        // Exports et création d'inventaire simulés : ce paquet éprouve les requêtes, pas le rendu
        // des documents ni les écritures que déclenchent les actions de l'écran.
        this.recapProduitVenduService = new RecapProduitVenduServiceImpl(
            entityManager,
            mock(ReportExcelExportService.class),
            mock(CsvExportService.class),
            mock(InventaireService.class),
            mock(RecapProduitVenduPdfService.class),
            mock(RecapProduitInvenduPdfService.class),
            mock(SuggestionProduitService.class)
        );

        ClientRetentionReportServiceImpl clientRetention = new ClientRetentionReportServiceImpl();
        ReflectionTestUtils.setField(clientRetention, "entityManager", entityManager);
        this.clientRetentionReportService = clientRetention;

        this.customerSegmentationReportService = new CustomerSegmentationReportServiceImpl(entityManager);
        this.cashRegisterReportService = new CashRegisterReportServiceImpl(entityManager);

        DemarqueReportServiceImpl demarque = new DemarqueReportServiceImpl();
        ReflectionTestUtils.setField(demarque, "entityManager", entityManager);
        this.demarqueReportService = demarque;

        MarketBasketAnalysisServiceImpl marketBasket = new MarketBasketAnalysisServiceImpl();
        ReflectionTestUtils.setField(marketBasket, "entityManager", entityManager);
        this.marketBasketAnalysisService = marketBasket;

        this.tiersPayantReportService = new TiersPayantReportServiceImpl(entityManager);
    }
}

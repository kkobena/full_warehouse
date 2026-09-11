package com.kobe.warehouse.service.ajustement.integration;

import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.repository.AjustRepository;
import com.kobe.warehouse.repository.AjustementRepository;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.LotStockLocationRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.AjustementService;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.criteria.InventoryTransactionSpec;
import com.kobe.warehouse.service.impl.LotServiceImpl;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionServiceIml;
import com.kobe.warehouse.service.id_generator.MvtProduitIdGeneratorService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotServiceReportService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import com.kobe.warehouse.service.stock.impl.LotStockLocationServiceImpl;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;

/**
 * Le graphe de l'ajustement, câblé à la main sur les vrais repositories du conteneur.
 *
 * <p>Sont <b>réels</b> les repositories et toute la chaîne que la validation d'un ajustement
 * déclenche : {@link LotServiceImpl} pour la quantité courante des lots, {@link
 * LotStockLocationServiceImpl} pour leur répartition par emplacement, et le service de journal —
 * c'est leur cohérence mutuelle avec {@code stock_produit} qu'on veut observer, et elle n'existe
 * que dans les requêtes FEFO et les contraintes de la base.
 *
 * <p>Sont <b>simulés</b> les collaborateurs qui dépendent du contexte de sécurité
 * ({@link StorageService}), d'un paramétrage applicatif ({@link AppConfigurationService}), du
 * système de fichiers ({@link LotServiceReportService}) ou d'un autre domaine — journal
 * applicatif et suggestions de réassort, dont l'ajustement n'attend rien en retour.
 */
final class AjustementServicesUnderTest {

    // --- collaborateurs simulés, exposés pour être paramétrés par les tests ---
    final StorageService storageService = mock(StorageService.class);
    final LogsService logsService = mock(LogsService.class);
    final SuggestionReassortService suggestionReassortService = mock(SuggestionReassortService.class);
    final AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);
    final LotServiceReportService lotServiceReportService = mock(LotServiceReportService.class);
    final OrderLineService orderLineService = mock(OrderLineService.class);

    // --- repositories réels ---
    final AjustRepository ajustRepository;
    final AjustementRepository ajustementRepository;
    final ProduitRepository produitRepository;
    final StockProduitRepository stockProduitRepository;
    final LotRepository lotRepository;
    final LotStockLocationRepository lotStockLocationRepository;
    final InventoryTransactionRepository inventoryTransactionRepository;

    // --- services réels ---
    final LotStockLocationService lotStockLocationService;
    final LotService lotService;
    final InventoryTransactionService inventoryTransactionService;

    // --- service sous test ---
    final AjustementService ajustementService;

    AjustementServicesUnderTest(EntityManager entityManager) {
        this.ajustRepository = IntegrationPostgresDatabase.bean(AjustRepository.class);
        this.ajustementRepository = IntegrationPostgresDatabase.bean(AjustementRepository.class);
        this.produitRepository = IntegrationPostgresDatabase.bean(ProduitRepository.class);
        this.stockProduitRepository = IntegrationPostgresDatabase.bean(StockProduitRepository.class);
        this.lotRepository = IntegrationPostgresDatabase.bean(LotRepository.class);
        this.lotStockLocationRepository = IntegrationPostgresDatabase.bean(LotStockLocationRepository.class);
        this.inventoryTransactionRepository = IntegrationPostgresDatabase.bean(InventoryTransactionRepository.class);

        this.lotStockLocationService = new LotStockLocationServiceImpl(lotStockLocationRepository, lotRepository);

        this.lotService = new LotServiceImpl(
            lotRepository,
            lotStockLocationRepository,
            appConfigurationService,
            produitRepository,
            lotServiceReportService,
            orderLineService,
            lotStockLocationService,
            storageService,
            IntegrationPostgresDatabase.bean(RetourBonRepository.class)
        );

        this.inventoryTransactionService = new InventoryTransactionServiceIml(
            inventoryTransactionRepository,
            new InventoryTransactionSpec(),
            new MvtProduitIdGeneratorService(entityManager),
            new ObjectMapper(),
            storageService
        );

        this.ajustementService = new AjustementService(
            ajustementRepository,
            produitRepository,
            ajustRepository,
            storageService,
            stockProduitRepository,
            logsService,
            inventoryTransactionService,
            suggestionReassortService,
            lotStockLocationService,
            lotService
        );
    }
}

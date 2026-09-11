package com.kobe.warehouse.service.reassort.integration;

import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.LigneReassortRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.LotStockLocationRepository;
import com.kobe.warehouse.repository.ReferenceRepository;
import com.kobe.warehouse.repository.RepartitionStockProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.SuggestionReassortRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.criteria.InventoryTransactionSpec;
import com.kobe.warehouse.service.id_generator.MvtProduitIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionServiceIml;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.reassort.impl.RepartitionStockServiceImpl;
import com.kobe.warehouse.service.reassort.impl.SuggestionReassortServiceImpl;
import com.kobe.warehouse.service.report.pdf.RepartitionStockPdfReportService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import com.kobe.warehouse.service.stock.impl.LotStockLocationServiceImpl;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;

/**
 * Le graphe du réassort, câblé à la main sur les vrais repositories du conteneur.
 *
 * <p>Sont <b>réels</b> les repositories et toute la chaîne qu'une répartition déclenche :
 * {@link LotStockLocationServiceImpl} pour le déplacement FEFO des lots, le service de journal
 * pour les deux mouvements de stock, et {@link ReferenceService} pour la numérotation des
 * suggestions — trois écritures dont la cohérence avec {@code stock_produit} ne se vérifie que
 * dans les requêtes et les contraintes de la base. Les deux services de réassort sont eux-mêmes
 * réels et se voient l'un l'autre : valider une suggestion doit réellement bouger du stock.
 *
 * <p>Sont <b>simulés</b> les collaborateurs qui dépendent du contexte de sécurité
 * ({@link StorageService}) ou du système de fichiers ({@link RepartitionStockPdfReportService},
 * qui rend un PDF dont le contenu n'apprend rien sur le réassort).
 */
final class ReassortServicesUnderTest {

    // --- collaborateurs simulés, exposés pour être paramétrés par les tests ---
    final StorageService storageService = mock(StorageService.class);
    final RepartitionStockPdfReportService repartitionStockPdfReportService = mock(RepartitionStockPdfReportService.class);

    // --- repositories réels ---
    final SuggestionReassortRepository suggestionReassortRepository;
    final LigneReassortRepository ligneReassortRepository;
    final RepartitionStockProduitRepository repartitionStockProduitRepository;
    final StockProduitRepository stockProduitRepository;
    final LotRepository lotRepository;
    final LotStockLocationRepository lotStockLocationRepository;
    final InventoryTransactionRepository inventoryTransactionRepository;

    // --- services réels ---
    final LotStockLocationService lotStockLocationService;
    final InventoryTransactionService inventoryTransactionService;
    final ReferenceService referenceService;

    // --- services sous test ---
    final RepartitionStockService repartitionStockService;
    final SuggestionReassortService suggestionReassortService;

    ReassortServicesUnderTest(EntityManager entityManager) {
        this.suggestionReassortRepository = IntegrationPostgresDatabase.bean(SuggestionReassortRepository.class);
        this.ligneReassortRepository = IntegrationPostgresDatabase.bean(LigneReassortRepository.class);
        this.repartitionStockProduitRepository = IntegrationPostgresDatabase.bean(RepartitionStockProduitRepository.class);
        this.stockProduitRepository = IntegrationPostgresDatabase.bean(StockProduitRepository.class);
        this.lotRepository = IntegrationPostgresDatabase.bean(LotRepository.class);
        this.lotStockLocationRepository = IntegrationPostgresDatabase.bean(LotStockLocationRepository.class);
        this.inventoryTransactionRepository = IntegrationPostgresDatabase.bean(InventoryTransactionRepository.class);

        this.lotStockLocationService = new LotStockLocationServiceImpl(lotStockLocationRepository, lotRepository);

        this.inventoryTransactionService = new InventoryTransactionServiceIml(
            inventoryTransactionRepository,
            new InventoryTransactionSpec(),
            new MvtProduitIdGeneratorService(entityManager),
            new ObjectMapper(),
            storageService
        );

        this.referenceService = new ReferenceService(IntegrationPostgresDatabase.bean(ReferenceRepository.class));

        this.repartitionStockService = new RepartitionStockServiceImpl(
            storageService,
            repartitionStockProduitRepository,
            stockProduitRepository,
            repartitionStockPdfReportService,
            inventoryTransactionService,
            lotStockLocationService
        );

        this.suggestionReassortService = new SuggestionReassortServiceImpl(
            suggestionReassortRepository,
            ligneReassortRepository,
            repartitionStockService,
            storageService,
            referenceService
        );
    }
}

package com.kobe.warehouse.service.stock.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.config.JacksonConfiguration;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.repository.AvoirFournisseurRepository;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.repository.FournisseurProduitPriceHistoryRepository;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.FournisseurRepository;
import com.kobe.warehouse.repository.InventoryGapAnalysisRepository;
import com.kobe.warehouse.repository.LotReceptionRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.LotStockLocationRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ParetoAnalysisRepository;
import com.kobe.warehouse.repository.PrixReferenceRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.ReferenceRepository;
import com.kobe.warehouse.repository.RetourBonItemRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.repository.RetourDepotItemRepository;
import com.kobe.warehouse.repository.RetourDepotRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.SubstitutRepository;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import com.kobe.warehouse.service.EtatProduitService;
import com.kobe.warehouse.service.FournisseurProduitService;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.id_generator.CommandeIdGeneratorService;
import com.kobe.warehouse.service.id_generator.OrderLineIdGeneratorService;
import com.kobe.warehouse.service.impl.LotServiceImpl;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.produit_prix.service.PrixRererenceService;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import com.kobe.warehouse.service.stock.DeliveryReceiptReportReportService;
import com.kobe.warehouse.service.report.pdf.RetourBonPdfReportService;
import com.kobe.warehouse.service.report.pdf.SuggestionPdfReportService;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.rupture.service.RuptureService;
import com.kobe.warehouse.service.sale.AvoirClientDocumentService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.DataMatrixParserService;
import com.kobe.warehouse.service.stock.GapAnalysisService;
import com.kobe.warehouse.service.stock.ImportationEchoueService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotServiceReportService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import com.kobe.warehouse.service.stock.AvoirFournisseurService;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.BedService;
import com.kobe.warehouse.service.stock.ProduitIndicateursService;
import com.kobe.warehouse.service.stock.ProduitMergeService;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.ReconciliationFournisseurService;
import com.kobe.warehouse.service.stock.RetourBonService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import com.kobe.warehouse.service.stock.RetourDepotService;
import com.kobe.warehouse.service.stock.StockEntryDataService;
import com.kobe.warehouse.service.stock.StockEntryService;
import com.kobe.warehouse.service.stock.StockProduitService;
import com.kobe.warehouse.service.stock.impl.DataMatrixParserServiceImpl;
import com.kobe.warehouse.service.stock.impl.GapAnalysisServiceImpl;
import com.kobe.warehouse.service.stock.impl.LotStockLocationServiceImpl;
import com.kobe.warehouse.service.stock.impl.AvoirFournisseurServiceImpl;
import com.kobe.warehouse.service.stock.impl.BedServiceImpl;
import com.kobe.warehouse.service.stock.impl.ProduitIndicateursServiceImpl;
import com.kobe.warehouse.service.stock.impl.ProduitMergeServiceImpl;
import com.kobe.warehouse.service.stock.impl.ProduitServiceImpl;
import com.kobe.warehouse.service.stock.impl.ReconciliationFournisseurServiceImpl;
import com.kobe.warehouse.service.stock.impl.RetourBonServiceImpl;
import com.kobe.warehouse.service.stock.impl.SuggestionProduitServiceImpl;
import com.kobe.warehouse.service.stock.impl.RetourDepotServiceImpl;
import com.kobe.warehouse.service.stock.impl.EtiquetteExportReportServiceImpl;
import com.kobe.warehouse.service.stock.impl.StockEntryDataServiceImpl;
import com.kobe.warehouse.service.stock.impl.StockEntryServiceImpl;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Le graphe des services de stock, câblé à la main sur les vrais repositories du conteneur.
 *
 * <p>Sont <b>réels</b> les repositories, les générateurs d'identifiants (qui lisent des séquences
 * Postgres) et les services sous test : c'est leur dialogue avec Postgres qu'on veut voir — ordre
 * FEFO, upsert sur {@code (lot, emplacement)}, suppression des lignes épuisées, stock recalculé à
 * la réception. {@link ProduitServiceImpl} est réel lui aussi : la réception lui délègue l'écriture
 * du stock et le prix moyen pondéré, et c'est précisément ce qu'on veut éprouver.
 *
 * <p>Sont <b>simulés</b> les collaborateurs qui dépendent du contexte de sécurité
 * ({@link StorageService}), d'un paramétrage applicatif ({@link AppConfigurationService}), du
 * système de fichiers ({@link LotServiceReportService}) ou d'un autre domaine — réassort, ruptures,
 * mouvements de stock, avoirs client. {@link OrderLineService} est simulé mais rebranché sur son
 * vrai repository : sa version de production ne fait que déléguer à celui-ci, et ces lectures et
 * écritures doivent bien passer par la base.
 */
final class StockServicesUnderTest {

    /**
     * Le mapper de l'application, et non un {@code new ObjectMapper()} nu : la recherche produit
     * desérialise le JSON rendu par Postgres, qui porte des champs de travail (le score de
     * pertinence) absents du DTO. Un mapper strict les refuse, et le service avale l'erreur en
     * rendant une liste vide — la recherche paraîtrait ne rien trouver.
     */
    final ObjectMapper objectMapper = new JacksonConfiguration().objectMapper();

    // --- collaborateurs simulés, exposés pour être paramétrés par les tests ---
    final StorageService storageService = mock(StorageService.class);
    final AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);
    final RepartitionStockService repartitionStockService = mock(RepartitionStockService.class);
    final LotServiceReportService lotServiceReportService = mock(LotServiceReportService.class);
    final OrderLineService orderLineService = mock(OrderLineService.class);
    final LogsService logsService = mock(LogsService.class);
    final SuggestionReassortService suggestionReassortService = mock(SuggestionReassortService.class);
    final InventoryTransactionService inventoryTransactionService = mock(InventoryTransactionService.class);
    final RuptureService ruptureService = mock(RuptureService.class);
    final AvoirClientDocumentService avoirClientDocumentService = mock(AvoirClientDocumentService.class);
    final ImportationEchoueService importationEchoueService = mock(ImportationEchoueService.class);
    final CustomizedProductService customizedProductService = mock(CustomizedProductService.class);
    final UserService userService = mock(UserService.class);
    final RetourBonPdfReportService retourBonPdfReportService = mock(RetourBonPdfReportService.class);
    final SuggestionPdfReportService suggestionPdfReportService = mock(SuggestionPdfReportService.class);
    final DeliveryReceiptReportReportService deliveryReceiptReportService = mock(DeliveryReceiptReportReportService.class);
    final EtiquetteExportReportServiceImpl etiquetteExportService = mock(EtiquetteExportReportServiceImpl.class);
    final EtatProduitService etatProduitService = mock(EtatProduitService.class);
    final CommandService commandService = mock(CommandService.class);
    final CsvExportService csvExportService = mock(CsvExportService.class);
    final PrixRererenceService prixRererenceService = mock(PrixRererenceService.class);

    // --- repositories réels ---
    final LotRepository lotRepository;
    final LotStockLocationRepository lotStockLocationRepository;
    final LotReceptionRepository lotReceptionRepository;
    final StockProduitRepository stockProduitRepository;
    final ProduitRepository produitRepository;
    final OrderLineRepository orderLineRepository;
    final RetourBonRepository retourBonRepository;
    final CommandeRepository commandeRepository;
    final InventoryGapAnalysisRepository inventoryGapAnalysisRepository;
    final StoreInventoryLineRepository storeInventoryLineRepository;
    final FournisseurRepository fournisseurRepository;
    final FournisseurProduitRepository fournisseurProduitRepository;
    final FournisseurProduitPriceHistoryRepository priceHistoryRepository;
    final RayonProduitRepository rayonProduitRepository;
    final SubstitutRepository substitutRepository;
    final PrixReferenceRepository prixReferenceRepository;
    final SemoisConfigurationRepository semoisConfigurationRepository;
    final VentesMensuellesAgregeesRepository ventesMensuellesAgregeesRepository;
    final SalesLineRepository salesLineRepository;
    final AvoirFournisseurRepository avoirFournisseurRepository;
    final RetourBonItemRepository retourBonItemRepository;
    final RetourDepotRepository retourDepotRepository;
    final RetourDepotItemRepository retourDepotItemRepository;
    final MagasinRepository magasinRepository;
    final ParetoAnalysisRepository paretoAnalysisRepository;
    final SuggestionRepository suggestionRepository;
    final SuggestionLineRepository suggestionLineRepository;

    // --- services sous test ---
    final LotStockLocationService lotStockLocationService;
    final LotService lotService;
    final StockProduitService stockProduitService;
    final ReconciliationFournisseurService reconciliationFournisseurService;
    final GapAnalysisService gapAnalysisService;
    final StockEntryService stockEntryService;
    final ProduitMergeService produitMergeService;
    final AvoirFournisseurService avoirFournisseurService;
    final RetourDepotService retourDepotService;
    final BedService bedService;
    final RetourBonService retourBonService;
    final SuggestionProduitService suggestionProduitService;
    final StockEntryDataService stockEntryDataService;
    final ProduitIndicateursService produitIndicateursService;

    // --- rouages réels utiles aux fabriques du jeu d'essai et aux assertions ---
    final CommandeIdGeneratorService commandeIdGeneratorService;
    final OrderLineIdGeneratorService orderLineIdGeneratorService;
    final ProduitService produitService;
    final ReferenceService referenceService;
    final DataMatrixParserService dataMatrixParserService;

    StockServicesUnderTest(EntityManager entityManager) {
        this.lotRepository = IntegrationPostgresDatabase.bean(LotRepository.class);
        this.lotStockLocationRepository = IntegrationPostgresDatabase.bean(LotStockLocationRepository.class);
        this.lotReceptionRepository = IntegrationPostgresDatabase.bean(LotReceptionRepository.class);
        this.stockProduitRepository = IntegrationPostgresDatabase.bean(StockProduitRepository.class);
        this.produitRepository = IntegrationPostgresDatabase.bean(ProduitRepository.class);
        this.orderLineRepository = IntegrationPostgresDatabase.bean(OrderLineRepository.class);
        this.retourBonRepository = IntegrationPostgresDatabase.bean(RetourBonRepository.class);
        this.commandeRepository = IntegrationPostgresDatabase.bean(CommandeRepository.class);
        this.inventoryGapAnalysisRepository = IntegrationPostgresDatabase.bean(InventoryGapAnalysisRepository.class);
        this.storeInventoryLineRepository = IntegrationPostgresDatabase.bean(StoreInventoryLineRepository.class);
        this.fournisseurRepository = IntegrationPostgresDatabase.bean(FournisseurRepository.class);
        this.fournisseurProduitRepository = IntegrationPostgresDatabase.bean(FournisseurProduitRepository.class);
        this.priceHistoryRepository = IntegrationPostgresDatabase.bean(FournisseurProduitPriceHistoryRepository.class);
        this.rayonProduitRepository = IntegrationPostgresDatabase.bean(RayonProduitRepository.class);
        this.substitutRepository = IntegrationPostgresDatabase.bean(SubstitutRepository.class);
        this.prixReferenceRepository = IntegrationPostgresDatabase.bean(PrixReferenceRepository.class);
        this.semoisConfigurationRepository = IntegrationPostgresDatabase.bean(SemoisConfigurationRepository.class);
        this.ventesMensuellesAgregeesRepository = IntegrationPostgresDatabase.bean(VentesMensuellesAgregeesRepository.class);
        this.salesLineRepository = IntegrationPostgresDatabase.bean(SalesLineRepository.class);
        this.avoirFournisseurRepository = IntegrationPostgresDatabase.bean(AvoirFournisseurRepository.class);
        this.retourBonItemRepository = IntegrationPostgresDatabase.bean(RetourBonItemRepository.class);
        this.retourDepotRepository = IntegrationPostgresDatabase.bean(RetourDepotRepository.class);
        this.retourDepotItemRepository = IntegrationPostgresDatabase.bean(RetourDepotItemRepository.class);
        this.magasinRepository = IntegrationPostgresDatabase.bean(MagasinRepository.class);
        // ParetoAnalysisRepository n'est pas un repository Spring Data mais une classe @Repository
        // qui lit la vue v_abc_pareto_analysis en SQL natif : le contexte de test ne scanne que les
        // interfaces JPA, il faut donc la construire et lui donner l'EntityManager a la main.
        this.suggestionRepository = IntegrationPostgresDatabase.bean(SuggestionRepository.class);
        this.suggestionLineRepository = IntegrationPostgresDatabase.bean(SuggestionLineRepository.class);
        this.paretoAnalysisRepository = new ParetoAnalysisRepository();
        ReflectionTestUtils.setField(paretoAnalysisRepository, "entityManager", entityManager);

        this.commandeIdGeneratorService = new CommandeIdGeneratorService(entityManager);
        this.orderLineIdGeneratorService = new OrderLineIdGeneratorService(entityManager);
        this.referenceService = new ReferenceService(IntegrationPostgresDatabase.bean(ReferenceRepository.class));
        this.dataMatrixParserService = new DataMatrixParserServiceImpl();

        brancherOrderLineServiceSurSonRepository();

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
            retourBonRepository
        );

        this.stockProduitService = new StockProduitService(
            stockProduitRepository,
            produitRepository,
            storageService,
            repartitionStockService
        );

        this.reconciliationFournisseurService = new ReconciliationFournisseurServiceImpl(commandeRepository);

        this.gapAnalysisService = new GapAnalysisServiceImpl(inventoryGapAnalysisRepository, storeInventoryLineRepository);

        this.produitService = new ProduitServiceImpl(
            produitRepository,
            customizedProductService,
            IntegrationPostgresDatabase.bean(RayonRepository.class),
            objectMapper,
            storageService,
            logsService,
            stockProduitRepository,
            rayonProduitRepository,
            suggestionReassortService,
            substitutRepository,
            fournisseurProduitRepository,
            prixRererenceService,
            dataMatrixParserService,
            salesLineRepository,
            orderLineRepository,
            inventoryTransactionService
        );

        // ProduitServiceImpl reçoit son EntityManager par @PersistenceContext, donc par le conteneur
        // Spring — ici le graphe est monté à la main et le champ resterait nul. La suppression en
        // cascade d'un produit passe entièrement par des requêtes JPQL et natives sur ce champ.
        ReflectionTestUtils.setField(produitService, "entityManager", entityManager);

        this.produitMergeService = new ProduitMergeServiceImpl(
            produitRepository,
            stockProduitRepository,
            lotRepository,
            prixReferenceRepository,
            fournisseurProduitRepository,
            rayonProduitRepository,
            salesLineRepository,
            semoisConfigurationRepository,
            ventesMensuellesAgregeesRepository,
            storeInventoryLineRepository,
            substitutRepository,
            logsService
        );
        // Meme raison que pour ProduitServiceImpl : la fusion reaffecte une dizaine de tables en
        // JPQL bulk sur ce champ, injecte par @PersistenceContext en production.
        ReflectionTestUtils.setField(produitMergeService, "entityManager", entityManager);

        this.avoirFournisseurService = new AvoirFournisseurServiceImpl(
            avoirFournisseurRepository,
            retourBonRepository,
            retourBonItemRepository,
            userService
        );

        this.retourDepotService = new RetourDepotServiceImpl(
            retourDepotRepository,
            retourDepotItemRepository,
            magasinRepository,
            produitRepository,
            userService,
            stockProduitRepository,
            inventoryTransactionService
        );

        this.bedService = new BedServiceImpl(
            commandeRepository,
            orderLineRepository,
            fournisseurProduitRepository,
            fournisseurRepository,
            storageService,
            logsService,
            produitService,
            commandeIdGeneratorService,
            orderLineIdGeneratorService,
            referenceService,
            inventoryTransactionService
        );

        this.produitIndicateursService = new ProduitIndicateursServiceImpl(
            paretoAnalysisRepository,
            ventesMensuellesAgregeesRepository
        );
        // Les indicateurs lisent deux vues Postgres en SQL natif sur cet EntityManager.
        ReflectionTestUtils.setField(produitIndicateursService, "entityManager", entityManager);

        this.retourBonService = new RetourBonServiceImpl(
            retourBonRepository,
            retourBonItemRepository,
            commandeRepository,
            orderLineRepository,
            userService,
            stockProduitRepository,
            inventoryTransactionService,
            lotRepository,
            retourBonPdfReportService,
            fournisseurProduitRepository,
            fournisseurRepository,
            appConfigurationService,
            avoirFournisseurService
        );

        this.suggestionProduitService = new SuggestionProduitServiceImpl(
            suggestionRepository,
            suggestionLineRepository,
            fournisseurProduitRepository,
            storageService,
            referenceService,
            appConfigurationService,
            etatProduitService,
            commandService,
            csvExportService,
            entityManager,
            suggestionPdfReportService,
            produitRepository,
            orderLineRepository
        );

        this.stockEntryDataService = new StockEntryDataServiceImpl(
            entityManager,
            commandeRepository,
            deliveryReceiptReportService,
            orderLineRepository,
            etiquetteExportService
        );

        FournisseurProduitService fournisseurProduitService = new FournisseurProduitService(
            fournisseurProduitRepository,
            fournisseurRepository,
            produitService
        );

        this.stockEntryService = new StockEntryServiceImpl(
            commandeRepository,
            produitService,
            referenceService,
            storageService,
            fournisseurProduitService,
            logsService,
            fournisseurRepository,
            orderLineService,
            commandeIdGeneratorService,
            orderLineIdGeneratorService,
            importationEchoueService,
            inventoryTransactionService,
            lotRepository,
            lotReceptionRepository,
            appConfigurationService,
            priceHistoryRepository,
            orderLineRepository,
            lotStockLocationService,
            suggestionReassortService,
            ruptureService,
            dataMatrixParserService,
            avoirClientDocumentService
        );
    }

    /**
     * En production, ces quatre méthodes ne font que passer la main au repository. Les rebrancher
     * dessus garde la base dans la boucle sans avoir à instancier tout le graphe produit dont
     * {@code OrderLineServiceImpl} dépend par ailleurs.
     */
    @SuppressWarnings("unchecked")
    private void brancherOrderLineServiceSurSonRepository() {
        lenient()
            .when(orderLineService.findOneById(any(OrderLineId.class)))
            .thenAnswer(invocation -> orderLineRepository.findById(invocation.getArgument(0)));
        lenient()
            .when(orderLineService.save(any(OrderLine.class)))
            .thenAnswer(invocation -> orderLineRepository.save(invocation.getArgument(0)));
        lenient()
            .when(orderLineService.findAllByOrderLineIdIn(anySet(), any()))
            .thenAnswer(invocation -> orderLineRepository.findAllByIdInAndOrderDate(invocation.getArgument(0), invocation.getArgument(1)));
        lenient()
            .doAnswer(invocation -> orderLineRepository.saveAll((Collection<OrderLine>) invocation.getArgument(0)))
            .when(orderLineService)
            .saveAll(any(List.class));
        lenient()
            .doAnswer(invocation -> orderLineRepository.saveAll((Collection<OrderLine>) invocation.getArgument(0)))
            .when(orderLineService)
            .saveAll(any(Set.class));
    }
}

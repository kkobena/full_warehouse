package com.kobe.warehouse.service.inventaire.integration;

import static org.mockito.Mockito.mock;

import com.kobe.warehouse.repository.FamilleProduitRepository;
import com.kobe.warehouse.repository.InventoryLotRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.PlanningInventaireTournantRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.StorageRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.historique_inventaire.HistoriqueInventaireService;
import com.kobe.warehouse.service.inventaire.InventaireCreationService;
import com.kobe.warehouse.service.inventaire.InventaireImportService;
import com.kobe.warehouse.service.inventaire.InventaireProgressService;
import com.kobe.warehouse.service.inventaire.InventaireQueryService;
import com.kobe.warehouse.service.inventaire.InventaireSyncService;
import com.kobe.warehouse.service.inventaire.InventoryCloseService;
import com.kobe.warehouse.service.inventaire.InventoryLotService;
import com.kobe.warehouse.service.inventaire.InventoryStockService;
import com.kobe.warehouse.service.inventaire.InventoryValuationService;
import com.kobe.warehouse.service.inventaire.PlanningInventaireTournantService;
import com.kobe.warehouse.service.inventaire.impl.InventaireCreationServiceImpl;
import com.kobe.warehouse.service.inventaire.impl.InventaireImportServiceImpl;
import com.kobe.warehouse.service.inventaire.impl.InventaireProgressServiceImpl;
import com.kobe.warehouse.service.inventaire.impl.InventaireQueryServiceImpl;
import com.kobe.warehouse.service.inventaire.impl.InventaireSyncServiceImpl;
import com.kobe.warehouse.service.inventaire.impl.InventoryCloseServiceImpl;
import com.kobe.warehouse.service.inventaire.impl.InventoryLotServiceImpl;
import com.kobe.warehouse.service.inventaire.impl.InventoryStockServiceImpl;
import com.kobe.warehouse.service.inventaire.impl.InventoryValuationServiceImpl;
import com.kobe.warehouse.service.inventaire.impl.PlanningInventaireTournantServiceImpl;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Le graphe des services d'inventaire, câblé à la main sur les vrais repositories du conteneur.
 *
 * <p>Sont <b>réels</b> les repositories, l'{@link EntityManager} et tous les services sous test :
 * ce qu'on veut éprouver — la constitution du périmètre, la valorisation, la progression, la
 * clôture — est écrit en SQL et n'existe que dans la base.
 *
 * <p>Sont <b>simulés</b> les collaborateurs qui dépendent du contexte de sécurité
 * ({@link UserService}, {@link StorageService}), d'un paramétrage applicatif
 * ({@link AppConfigurationService}), ou d'un autre domaine — l'historique des inventaires et la
 * publication d'événements, dont le traitement asynchrone n'a pas sa place dans une transaction
 * annulée à la fin du test.
 */
final class InventaireServicesUnderTest {

    // --- collaborateurs simulés, exposés pour être paramétrés par les tests ---
    final UserService userService = mock(UserService.class);
    final StorageService storageService = mock(StorageService.class);
    final AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);
    final HistoriqueInventaireService historiqueInventaireService = mock(HistoriqueInventaireService.class);
    final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    // --- repositories réels ---
    final StoreInventoryRepository storeInventoryRepository;
    final StoreInventoryLineRepository storeInventoryLineRepository;
    final InventoryLotRepository inventoryLotRepository;
    final StockProduitRepository stockProduitRepository;
    final LotRepository lotRepository;
    final RayonRepository rayonRepository;
    final StorageRepository storageRepository;
    final UserRepository userRepository;
    final FamilleProduitRepository familleProduitRepository;
    final PlanningInventaireTournantRepository planningRepository;

    // --- services sous test ---
    final InventoryStockService inventoryStockService;
    final InventaireCreationService inventaireCreationService;
    final InventaireQueryService inventaireQueryService;
    final InventaireProgressService inventaireProgressService;
    final InventoryValuationService inventoryValuationService;
    final InventaireSyncService inventaireSyncService;
    final InventaireImportService inventaireImportService;
    final InventoryCloseService inventoryCloseService;
    final InventoryLotService inventoryLotService;
    final PlanningInventaireTournantService planningInventaireTournantService;

    InventaireServicesUnderTest(EntityManager entityManager) {
        this.storeInventoryRepository = IntegrationPostgresDatabase.bean(StoreInventoryRepository.class);
        this.storeInventoryLineRepository = IntegrationPostgresDatabase.bean(StoreInventoryLineRepository.class);
        this.inventoryLotRepository = IntegrationPostgresDatabase.bean(InventoryLotRepository.class);
        this.stockProduitRepository = IntegrationPostgresDatabase.bean(StockProduitRepository.class);
        this.lotRepository = IntegrationPostgresDatabase.bean(LotRepository.class);
        this.rayonRepository = IntegrationPostgresDatabase.bean(RayonRepository.class);
        this.storageRepository = IntegrationPostgresDatabase.bean(StorageRepository.class);
        this.userRepository = IntegrationPostgresDatabase.bean(UserRepository.class);
        this.familleProduitRepository = IntegrationPostgresDatabase.bean(FamilleProduitRepository.class);
        this.planningRepository = IntegrationPostgresDatabase.bean(PlanningInventaireTournantRepository.class);

        this.inventoryStockService = new InventoryStockServiceImpl(stockProduitRepository);

        this.inventaireCreationService = new InventaireCreationServiceImpl(
            storeInventoryRepository,
            userService,
            userRepository,
            storageService,
            rayonRepository,
            entityManager,
            appConfigurationService
        );

        this.inventaireQueryService = new InventaireQueryServiceImpl(
            storeInventoryRepository,
            inventoryStockService,
            appConfigurationService,
            entityManager
        );

        this.inventaireProgressService = new InventaireProgressServiceImpl(entityManager);

        this.inventoryValuationService = new InventoryValuationServiceImpl(entityManager);

        this.inventaireSyncService = new InventaireSyncServiceImpl(storeInventoryLineRepository, userService);

        this.inventaireImportService = new InventaireImportServiceImpl(
            storeInventoryLineRepository,
            inventoryStockService,
            userService
        );

        this.inventoryCloseService = new InventoryCloseServiceImpl(
            storeInventoryLineRepository,
            storeInventoryRepository,
            historiqueInventaireService,
            appConfigurationService,
            entityManager,
            eventPublisher
        );

        this.inventoryLotService = new InventoryLotServiceImpl(
            inventoryLotRepository,
            storeInventoryLineRepository,
            storeInventoryRepository,
            lotRepository,
            userService,
            inventoryStockService,
            entityManager
        );

        this.planningInventaireTournantService = new PlanningInventaireTournantServiceImpl(
            planningRepository,
            rayonRepository,
            familleProduitRepository,
            storageRepository,
            userRepository,
            inventaireCreationService
        );
    }
}

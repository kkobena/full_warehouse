package com.kobe.warehouse.service.sale.integration;

import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.AvoirClientUtilisationRepository;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.repository.RetourClientRepository;
import com.kobe.warehouse.repository.SalePaymentRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.VenteDepotRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.declaration_ca.DeclarationCaService;
import com.kobe.warehouse.service.declaration_ca.PonctionService;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.impl.PaymentServiceImpl;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.produit_prix.service.PrixRererenceService;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.report.SaleInvoiceReportService;
import com.kobe.warehouse.service.ReceiptPrinterService;
import com.kobe.warehouse.service.sale.AssuredCustomerManager;
import com.kobe.warehouse.service.sale.AvoirClientDocumentService;
import com.kobe.warehouse.service.sale.AvoirClientNotificationService;
import com.kobe.warehouse.service.sale.AvoirClientService;
import com.kobe.warehouse.service.sale.RetourAvoirDashboardService;
import com.kobe.warehouse.service.sale.RetourClientService;
import com.kobe.warehouse.service.sale.SimplifiedSaleService;
import com.kobe.warehouse.service.sale.ThirdPartyCalculationManager;
import com.kobe.warehouse.service.sale.ThirdPartyClientManager;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.service.sale.SaleDepotExtensionService;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.SalesManager;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.calculation.TiersPayantCalculationService;
import com.kobe.warehouse.service.sale.impl.AssuredCustomerManagerImpl;
import com.kobe.warehouse.service.sale.impl.AvoirClientDocumentServiceImpl;
import com.kobe.warehouse.service.sale.impl.AvoirClientServiceImpl;
import com.kobe.warehouse.service.sale.impl.RetourClientServiceImpl;
import com.kobe.warehouse.service.sale.impl.SimplifiedSaleServiceImpl;
import com.kobe.warehouse.service.sale.impl.ConsommationService;
import com.kobe.warehouse.service.sale.impl.SaleCommonService;
import com.kobe.warehouse.service.sale.impl.SaleDepotExtensionImpl;
import com.kobe.warehouse.service.sale.impl.SaleLineServiceFactory;
import com.kobe.warehouse.service.sale.impl.SaleServiceImpl;
import com.kobe.warehouse.service.sale.impl.SalesLineServiceBaseImpl;
import com.kobe.warehouse.service.sale.impl.SalesManagerImpl;
import com.kobe.warehouse.service.sale.impl.StockUpdateService;
import com.kobe.warehouse.service.sale.impl.ThirdPartyCalculationManagerImpl;
import com.kobe.warehouse.service.sale.impl.ThirdPartyClientManagerImpl;
import com.kobe.warehouse.service.sale.impl.ThirdPartySaleLineService;
import com.kobe.warehouse.service.sale.impl.ThirdPartySaleServiceImpl;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.DataMatrixParserService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.Supplier;

/**
 * Le graphe des services de vente, câblé à la main sur les vrais repositories du conteneur.
 *
 * <p>Sont <b>réels</b> tous les objets dont le comportement dépend de la base : les repositories,
 * les générateurs d'identifiants (qui lisent des séquences Postgres), {@link ReferenceService},
 * {@link SalesLineServiceBaseImpl}, {@link StockUpdateService}, {@link SaleCommonService} et les
 * cinq services sous test — c'est leur dialogue avec Postgres qu'on veut voir.
 *
 * <p>Sont <b>simulés</b> les collaborateurs qui parlent au matériel ou à un autre domaine :
 * afficheur client, imprimante, journal, licences, déclaration de chiffre d'affaires. Le stockage
 * ({@link StorageService}) et la caisse ({@link CashRegisterService}) le sont aussi : ils
 * dépendent du contexte de sécurité, que le test remplace par un utilisateur choisi explicitement.
 */
final class SaleServicesUnderTest {

    final ObjectMapper objectMapper = new ObjectMapper();

    // --- collaborateurs simulés, exposés pour être paramétrés par les tests ---
    final StorageService storageService = mock(StorageService.class);
    final CashRegisterService cashRegisterService = mock(CashRegisterService.class);
    final CustomerDisplayService customerDisplayService = mock(CustomerDisplayService.class);
    final AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);
    final UtilisationCleSecuriteService utilisationCleSecuriteService = mock(UtilisationCleSecuriteService.class);
    final DeclarationCaService declarationCaService = mock(DeclarationCaService.class);
    final PonctionService ponctionService = mock(PonctionService.class);
    final LogsService logsService = mock(LogsService.class);
    final SuggestionReassortService suggestionReassortService = mock(SuggestionReassortService.class);
    final InventoryTransactionService inventoryTransactionService = mock(InventoryTransactionService.class);
    final LotService lotService = mock(LotService.class);
    final LotStockLocationService lotStockLocationService = mock(LotStockLocationService.class);
    final RepartitionStockService repartitionStockService = mock(RepartitionStockService.class);
    final AvoirClientNotificationService avoirClientNotificationService = mock(AvoirClientNotificationService.class);
    final DataMatrixParserService dataMatrixParserService = mock(DataMatrixParserService.class);
    final PrixRererenceService prixRererenceService = mock(PrixRererenceService.class);
    final SaleInvoiceReportService saleInvoiceReportService = mock(SaleInvoiceReportService.class);
    final ReceiptPrinterService receiptPrinterService = mock(ReceiptPrinterService.class);

    // --- repositories réels ---
    final SalesRepository salesRepository;
    final SalesLineRepository salesLineRepository;
    final CashSaleRepository cashSaleRepository;
    final VenteDepotRepository venteDepotRepository;
    final ThirdPartySaleRepository thirdPartySaleRepository;
    final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    final StockProduitRepository stockProduitRepository;
    final ProduitRepository produitRepository;
    final UserRepository userRepository;
    final ClientTiersPayantRepository clientTiersPayantRepository;
    final TiersPayantRepository tiersPayantRepository;
    final AssuredCustomerRepository assuredCustomerRepository;
    final SalePaymentRepository salePaymentRepository;
    final AvoirClientRepository avoirClientRepository;
    final AvoirClientUtilisationRepository avoirClientUtilisationRepository;
    final RetourClientRepository retourClientRepository;

    // --- services sous test ---
    SaleService saleService;
    final SalesLineService salesLineService;
    ThirdPartySaleService thirdPartySaleService;
    final SaleDepotExtensionService saleDepotExtensionService;
    final SaleDataService saleDataService;
    final SimplifiedSaleService simplifiedSaleService;
    final AvoirClientDocumentService avoirClientDocumentService;
    final AvoirClientService avoirClientService;
    final RetourClientService retourClientService;
    final RetourAvoirDashboardService retourAvoirDashboardService;

    // --- rouages réels utiles aux assertions ---
    final SalesManager salesManager;
    final PaymentService paymentService;
    final ReferenceService referenceService;
    final SaleIdGeneratorService saleIdGeneratorService;
    final SaleLineIdGeneratorService saleLineIdGeneratorService;
    final StockUpdateService stockUpdateService;
    final ConsommationService consommationService;
    final ThirdPartyClientManager thirdPartyClientManager;
    final ThirdPartyCalculationManager thirdPartyCalculationManager;
    final AssuredCustomerManager assuredCustomerManager;

    SaleServicesUnderTest(EntityManager entityManager) {
        this.salesRepository = SalePostgresDatabase.bean(SalesRepository.class);
        this.salesLineRepository = SalePostgresDatabase.bean(SalesLineRepository.class);
        this.cashSaleRepository = SalePostgresDatabase.bean(CashSaleRepository.class);
        this.venteDepotRepository = SalePostgresDatabase.bean(VenteDepotRepository.class);
        this.thirdPartySaleRepository = SalePostgresDatabase.bean(ThirdPartySaleRepository.class);
        this.thirdPartySaleLineRepository = SalePostgresDatabase.bean(ThirdPartySaleLineRepository.class);
        this.stockProduitRepository = SalePostgresDatabase.bean(StockProduitRepository.class);
        this.produitRepository = SalePostgresDatabase.bean(ProduitRepository.class);
        this.userRepository = SalePostgresDatabase.bean(UserRepository.class);
        this.clientTiersPayantRepository = SalePostgresDatabase.bean(ClientTiersPayantRepository.class);
        this.tiersPayantRepository = SalePostgresDatabase.bean(TiersPayantRepository.class);
        this.assuredCustomerRepository = SalePostgresDatabase.bean(AssuredCustomerRepository.class);
        this.salePaymentRepository = SalePostgresDatabase.bean(SalePaymentRepository.class);
        this.avoirClientRepository = SalePostgresDatabase.bean(AvoirClientRepository.class);
        this.avoirClientUtilisationRepository = SalePostgresDatabase.bean(AvoirClientUtilisationRepository.class);
        this.retourClientRepository = SalePostgresDatabase.bean(RetourClientRepository.class);

        PaymentModeRepository paymentModeRepository = SalePostgresDatabase.bean(PaymentModeRepository.class);
        PosteRepository posteRepository = SalePostgresDatabase.bean(PosteRepository.class);
        RemiseRepository remiseRepository = SalePostgresDatabase.bean(RemiseRepository.class);
        UninsuredCustomerRepository uninsuredCustomerRepository = SalePostgresDatabase.bean(UninsuredCustomerRepository.class);

        this.referenceService = new ReferenceService(SalePostgresDatabase.bean(com.kobe.warehouse.repository.ReferenceRepository.class));
        this.saleIdGeneratorService = new SaleIdGeneratorService(entityManager);
        this.saleLineIdGeneratorService = new SaleLineIdGeneratorService(entityManager);
        AssuranceItemIdGeneratorService assuranceItemIdGeneratorService = new AssuranceItemIdGeneratorService(entityManager);
        TransactionIdGeneratorService transactionIdGeneratorService = new TransactionIdGeneratorService(entityManager);

        this.paymentService = new PaymentServiceImpl(salePaymentRepository, paymentModeRepository, transactionIdGeneratorService);

        this.stockUpdateService = new StockUpdateService(stockProduitRepository, logsService, suggestionReassortService);

        // L'avoir client est le premier effet de bord d'une vente servie en partie : on le garde
        // reel, c'est une ligne ecrite en base, pas un appel a verifier.
        this.avoirClientDocumentService = new AvoirClientDocumentServiceImpl(
            avoirClientRepository,
            salesLineRepository,
            referenceService,
            storageService,
            stockProduitRepository,
            avoirClientNotificationService,
            appConfigurationService,
            avoirClientUtilisationRepository
        );

        SalesLineServiceBaseImpl salesLineServiceBase = new SalesLineServiceBaseImpl(
            produitRepository,
            salesLineRepository,
            stockProduitRepository,
            lotService,
            inventoryTransactionService,
            saleLineIdGeneratorService,
            this.stockUpdateService,
            storageService,
            repartitionStockService,
            lotStockLocationService,
            avoirClientDocumentService,
            dataMatrixParserService
        );
        this.salesLineService = salesLineServiceBase;
        SaleLineServiceFactory saleLineServiceFactory = new SaleLineServiceFactory(salesLineServiceBase);

        SaleCommonService saleCommonService = new SaleCommonService(
            referenceService,
            storageService,
            userRepository,
            saleLineServiceFactory,
            cashRegisterService,
            posteRepository,
            customerDisplayService,
            saleIdGeneratorService,
            objectMapper,
            appConfigurationService
        );

        // SalesManager et les deux services de vente se référencent mutuellement : la production
        // casse le cycle avec @Lazy, on le casse ici avec un mandataire qui résout au premier appel.
        SaleService saleServiceProxy = lazy(SaleService.class, () -> this.saleService);
        ThirdPartySaleService thirdPartyProxy = lazy(ThirdPartySaleService.class, () -> this.thirdPartySaleService);

        this.salesManager = new SalesManagerImpl(
            saleLineServiceFactory,
            storageService,
            customerDisplayService,
            cashSaleRepository,
            venteDepotRepository,
            saleServiceProxy,
            thirdPartyProxy,
            saleCommonService
        );

        this.saleService = new SaleServiceImpl(
            salesRepository,
            userRepository,
            uninsuredCustomerRepository,
            paymentModeRepository,
            storageService,
            cashSaleRepository,
            cashRegisterService,
            saleLineServiceFactory,
            paymentService,
            referenceService,
            posteRepository,
            utilisationCleSecuriteService,
            remiseRepository,
            customerDisplayService,
            saleIdGeneratorService,
            objectMapper,
            salesManager,
            appConfigurationService,
            declarationCaService,
            ponctionService
        );

        ThirdPartySaleLineService thirdPartySaleLineService = new ThirdPartySaleLineService(
            assuranceItemIdGeneratorService,
            thirdPartySaleLineRepository
        );
        this.consommationService = new ConsommationService();
        ThirdPartyCalculationManagerImpl thirdPartyCalculationManager = new ThirdPartyCalculationManagerImpl(
            new TiersPayantCalculationService(),
            thirdPartySaleLineService,
            saleLineServiceFactory,
            thirdPartySaleRepository,
            prixRererenceService,
            saleCommonService
        );
        ThirdPartyClientManagerImpl thirdPartyClientManager = new ThirdPartyClientManagerImpl(
            thirdPartySaleLineService,
            clientTiersPayantRepository,
            tiersPayantRepository,
            thirdPartySaleRepository,
            consommationService,
            storageService,
            assuranceItemIdGeneratorService,
            thirdPartyCalculationManager
        );

        this.thirdPartyCalculationManager = thirdPartyCalculationManager;
        this.thirdPartyClientManager = thirdPartyClientManager;
        this.assuredCustomerManager = new AssuredCustomerManagerImpl(assuredCustomerRepository);

        this.thirdPartySaleService = new ThirdPartySaleServiceImpl(
            thirdPartySaleLineService,
            clientTiersPayantRepository,
            saleLineServiceFactory,
            storageService,
            thirdPartySaleRepository,
            assuredCustomerRepository,
            userRepository,
            paymentService,
            referenceService,
            cashRegisterService,
            posteRepository,
            cashSaleRepository,
            utilisationCleSecuriteService,
            remiseRepository,
            customerDisplayService,
            logsService,
            saleIdGeneratorService,
            objectMapper,
            salesManager,
            thirdPartyClientManager,
            thirdPartyCalculationManager,
            assuredCustomerManager,
            appConfigurationService,
            declarationCaService
        );

        this.saleDepotExtensionService = new SaleDepotExtensionImpl(
            remiseRepository,
            referenceService,
            storageService,
            userRepository,
            saleLineServiceFactory,
            cashRegisterService,
            posteRepository,
            customerDisplayService,
            saleIdGeneratorService,
            venteDepotRepository,
            this.stockUpdateService,
            inventoryTransactionService,
            objectMapper,
            salesManager,
            appConfigurationService
        );

        this.simplifiedSaleService = new SimplifiedSaleServiceImpl(
            paymentService,
            cashSaleRepository,
            referenceService,
            storageService,
            userRepository,
            saleLineServiceFactory,
            cashRegisterService,
            posteRepository,
            customerDisplayService,
            saleIdGeneratorService,
            uninsuredCustomerRepository,
            objectMapper,
            appConfigurationService
        );

        this.avoirClientService = new AvoirClientServiceImpl(salesLineRepository);

        this.retourClientService = new RetourClientServiceImpl(
            retourClientRepository,
            salesRepository,
            salesLineRepository,
            stockProduitRepository,
            avoirClientRepository,
            referenceService,
            storageService,
            inventoryTransactionService,
            appConfigurationService
        );

        this.retourAvoirDashboardService = new RetourAvoirDashboardService(retourClientRepository, avoirClientRepository);

        this.saleDataService = new SaleDataService(
            entityManager,
            saleInvoiceReportService,
            salesLineRepository,
            thirdPartySaleLineRepository,
            receiptPrinterService,
            salesRepository,
            storageService
        );
    }

    private static <T> T lazy(Class<T> type, Supplier<T> supplier) {
        return type.cast(
            Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, (proxy, method, args) -> {
                try {
                    return method.invoke(supplier.get(), args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            })
        );
    }
}

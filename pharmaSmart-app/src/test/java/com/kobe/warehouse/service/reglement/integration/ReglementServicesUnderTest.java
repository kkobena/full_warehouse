package com.kobe.warehouse.service.reglement.integration;

import static org.mockito.Mockito.mock;

import com.kobe.warehouse.repository.BanqueRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.DifferePaymentRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.InvoicePaymentItemRepository;
import com.kobe.warehouse.repository.InvoicePaymentRepository;
import com.kobe.warehouse.repository.ReferenceRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import com.kobe.warehouse.service.id_generator.FactureIdGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.id_generator.TransactionItemIdGeneratorService;
import com.kobe.warehouse.service.receipt.service.DiffereReceiptService;
import com.kobe.warehouse.service.receipt.service.InvoiceReceiptService;
import com.kobe.warehouse.service.reglement.ReglementRegistry;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereReportService;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereServiceImpl;
import com.kobe.warehouse.service.reglement.service.InvoicePaymentItemService;
import com.kobe.warehouse.service.reglement.service.ReglementDataService;
import com.kobe.warehouse.service.reglement.service.ReglementDataServiceImpl;
import com.kobe.warehouse.service.reglement.service.ReglementFactureModeAllService;
import com.kobe.warehouse.service.reglement.service.ReglementFactureSelectionneesService;
import com.kobe.warehouse.service.reglement.service.ReglementGroupeFactureService;
import com.kobe.warehouse.service.reglement.service.ReglementGroupeSelectionFactureService;
import com.kobe.warehouse.service.reglement.service.ReglementReportService;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;

/**
 * Le graphe des services de règlement, câblé à la main sur les vrais repositories du conteneur.
 *
 * <p>Sont <b>réels</b> tous les objets dont le comportement dépend de la base : les repositories,
 * les générateurs d'identifiants (qui lisent des séquences Postgres), {@link ReferenceService} —
 * qui numérote les transactions en incrémentant une ligne — et les six services sous test. C'est
 * leur dialogue avec Postgres qu'on veut voir : montants reportés sur la facture et sur chaque
 * dossier, statuts recalculés, paiements groupés reliés à leur parent.
 *
 * <p>Sont <b>simulés</b> les collaborateurs qui parlent au matériel, au système de fichiers ou au
 * contexte de sécurité : imprimante à tickets, rapports PDF, utilisateur connecté
 * ({@link UserService}) et caisse ouverte ({@link CashRegisterService}), que le test remplace par
 * un utilisateur et une caisse choisis explicitement.
 */
final class ReglementServicesUnderTest {

    // --- collaborateurs simulés, exposés pour être paramétrés par les tests ---
    final CashRegisterService cashRegisterService = mock(CashRegisterService.class);
    final UserService userService = mock(UserService.class);
    final ReglementReportService reglementReportService = mock(ReglementReportService.class);
    final ReglementDiffereReportService reglementDiffereReportService = mock(ReglementDiffereReportService.class);
    final InvoiceReceiptService invoiceReceiptService = mock(InvoiceReceiptService.class);
    final DiffereReceiptService differeReceiptService = mock(DiffereReceiptService.class);

    // --- repositories réels ---
    final FacturationRepository facturationRepository;
    final InvoicePaymentRepository invoicePaymentRepository;
    final InvoicePaymentItemRepository invoicePaymentItemRepository;
    final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    final DifferePaymentRepository differePaymentRepository;
    final SalesRepository salesRepository;
    final CustomerRepository customerRepository;
    final BanqueRepository banqueRepository;

    // --- services sous test ---
    final ReglementFactureModeAllService reglementFactureModeAllService;
    final ReglementFactureSelectionneesService reglementFactureSelectionneesService;
    final ReglementGroupeFactureService reglementGroupeFactureService;
    final ReglementGroupeSelectionFactureService reglementGroupeSelectionFactureService;
    final ReglementDataService reglementDataService;
    final ReglementDiffereService reglementDiffereService;
    final ReglementRegistry reglementRegistry;

    // --- rouages réels utiles aux fabriques du jeu d'essai ---
    final ReferenceService referenceService;
    final SaleIdGeneratorService saleIdGeneratorService;
    final FactureIdGeneratorService factureIdGeneratorService;
    final AssuranceItemIdGeneratorService assuranceItemIdGeneratorService;

    ReglementServicesUnderTest(EntityManager entityManager) {
        this.facturationRepository = IntegrationPostgresDatabase.bean(FacturationRepository.class);
        this.invoicePaymentRepository = IntegrationPostgresDatabase.bean(InvoicePaymentRepository.class);
        this.invoicePaymentItemRepository = IntegrationPostgresDatabase.bean(InvoicePaymentItemRepository.class);
        this.thirdPartySaleLineRepository = IntegrationPostgresDatabase.bean(ThirdPartySaleLineRepository.class);
        this.differePaymentRepository = IntegrationPostgresDatabase.bean(DifferePaymentRepository.class);
        this.salesRepository = IntegrationPostgresDatabase.bean(SalesRepository.class);
        this.customerRepository = IntegrationPostgresDatabase.bean(CustomerRepository.class);
        this.banqueRepository = IntegrationPostgresDatabase.bean(BanqueRepository.class);

        this.referenceService = new ReferenceService(IntegrationPostgresDatabase.bean(ReferenceRepository.class));
        this.saleIdGeneratorService = new SaleIdGeneratorService(entityManager);
        this.factureIdGeneratorService = new FactureIdGeneratorService(entityManager);
        this.assuranceItemIdGeneratorService = new AssuranceItemIdGeneratorService(entityManager);
        TransactionIdGeneratorService transactionIdGeneratorService = new TransactionIdGeneratorService(entityManager);
        InvoicePaymentItemService invoicePaymentItemService = new InvoicePaymentItemService(
            new TransactionItemIdGeneratorService(entityManager)
        );

        this.reglementFactureModeAllService = new ReglementFactureModeAllService(
            cashRegisterService,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            thirdPartySaleLineRepository,
            banqueRepository,
            transactionIdGeneratorService,
            invoicePaymentItemService,
            referenceService
        );

        this.reglementFactureSelectionneesService = new ReglementFactureSelectionneesService(
            cashRegisterService,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            thirdPartySaleLineRepository,
            banqueRepository,
            transactionIdGeneratorService,
            invoicePaymentItemService,
            referenceService
        );

        this.reglementGroupeFactureService = new ReglementGroupeFactureService(
            cashRegisterService,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            thirdPartySaleLineRepository,
            banqueRepository,
            reglementFactureModeAllService,
            transactionIdGeneratorService,
            invoicePaymentItemService,
            referenceService
        );

        this.reglementGroupeSelectionFactureService = new ReglementGroupeSelectionFactureService(
            cashRegisterService,
            invoicePaymentRepository,
            userService,
            facturationRepository,
            thirdPartySaleLineRepository,
            banqueRepository,
            reglementFactureSelectionneesService,
            transactionIdGeneratorService,
            invoicePaymentItemService,
            referenceService
        );

        this.reglementRegistry = new ReglementRegistry(
            reglementGroupeSelectionFactureService,
            reglementGroupeFactureService,
            reglementFactureModeAllService,
            reglementFactureSelectionneesService
        );

        this.reglementDataService = new ReglementDataServiceImpl(
            facturationRepository,
            invoicePaymentRepository,
            thirdPartySaleLineRepository,
            invoicePaymentItemRepository,
            reglementReportService,
            invoiceReceiptService
        );

        this.reglementDiffereService = new ReglementDiffereServiceImpl(
            differePaymentRepository,
            salesRepository,
            customerRepository,
            cashRegisterService,
            reglementDiffereReportService,
            banqueRepository,
            differeReceiptService,
            transactionIdGeneratorService,
            referenceService,
            new ReportExcelExportService()
        );
    }
}

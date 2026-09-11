package com.kobe.warehouse.service.facturation.integration;

import static org.mockito.Mockito.mock;

import com.kobe.warehouse.repository.AvoirTiersPayantRepository;
import com.kobe.warehouse.repository.FacturationRepository;
import com.kobe.warehouse.repository.FactureTiersPayantRepository;
import com.kobe.warehouse.repository.GroupeTiersPayantRepository;
import com.kobe.warehouse.repository.HistoriquePlanificationRepository;
import com.kobe.warehouse.repository.PlanificationFacturationRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.facturation.registry.FacturationServiceRegistry;
import com.kobe.warehouse.service.facturation.service.AvoirExcelService;
import com.kobe.warehouse.service.facturation.service.AvoirPdfService;
import com.kobe.warehouse.service.facturation.service.AvoirService;
import com.kobe.warehouse.service.facturation.service.AvoirServiceImpl;
import com.kobe.warehouse.service.facturation.service.EditionAllService;
import com.kobe.warehouse.service.facturation.service.EditionDataService;
import com.kobe.warehouse.service.facturation.service.EditionDataServiceImpl;
import com.kobe.warehouse.service.facturation.service.FacturationPdfExportService;
import com.kobe.warehouse.service.facturation.service.GroupeFacturePdfExportService;
import com.kobe.warehouse.service.facturation.service.EditionByGroupTiersService;
import com.kobe.warehouse.service.facturation.service.EditionBySelectionBonsService;
import com.kobe.warehouse.service.facturation.service.EditionByTiersPayantService;
import com.kobe.warehouse.service.facturation.service.EditionByTypeTiersPayantService;
import com.kobe.warehouse.service.facturation.service.PlanificationFacturationService;
import com.kobe.warehouse.service.facturation.service.PlanificationFacturationServiceImpl;
import com.kobe.warehouse.service.facturation.service.PlanificationStatutService;
import com.kobe.warehouse.service.facturation.service.RapprochementService;
import com.kobe.warehouse.service.facturation.service.RecapitulatifMensuelService;
import com.kobe.warehouse.service.facturation.service.RecapitulatifMensuelServiceImpl;
import com.kobe.warehouse.service.facturation.service.RapprochementServiceImpl;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import com.kobe.warehouse.service.id_generator.FactureIdGeneratorService;
import com.kobe.warehouse.service.id_generator.InvoiceGenerationCodeGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.report.excel.ReportExcelExportService;
import com.kobe.warehouse.service.report.pdf.RapprochementPdfReportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Le graphe des services de facturation, câblé à la main sur les vrais repositories du conteneur.
 *
 * <p>Sont <b>réels</b> les repositories, les générateurs d'identifiants — qui lisent des séquences
 * Postgres — et les cinq services d'édition : c'est leur dialogue avec la base qu'on veut voir,
 * depuis la sélection des dossiers par spécification Criteria jusqu'aux montants écrits sur la
 * facture.
 *
 * <p>Sont <b>simulés</b> l'utilisateur connecté ({@link UserService}, qui dépend du contexte de
 * sécurité) et le paramétrage applicatif ({@link AppConfigurationService}).
 */
final class FacturationServicesUnderTest {

    // --- collaborateurs simulés, exposés pour être paramétrés par les tests ---
    final UserService userService = mock(UserService.class);
    final AppConfigurationService appConfigurationService = mock(AppConfigurationService.class);
    final StorageService storageService = mock(StorageService.class);
    final AvoirPdfService avoirPdfService = mock(AvoirPdfService.class);
    final AvoirExcelService avoirExcelService = mock(AvoirExcelService.class);
    final RapprochementPdfReportService rapprochementPdfReportService = mock(RapprochementPdfReportService.class);
    final FacturationPdfExportService facturationPdfExportService = mock(FacturationPdfExportService.class);
    final GroupeFacturePdfExportService groupeFacturePdfExportService = mock(GroupeFacturePdfExportService.class);

    // --- repositories réels ---
    final FacturationRepository facturationRepository;
    final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    final AvoirTiersPayantRepository avoirTiersPayantRepository;
    final FactureTiersPayantRepository factureTiersPayantRepository;
    final PlanificationFacturationRepository planificationFacturationRepository;
    final HistoriquePlanificationRepository historiquePlanificationRepository;
    final TiersPayantRepository tiersPayantRepository;
    final GroupeTiersPayantRepository groupeTiersPayantRepository;

    // --- services sous test ---
    final EditionAllService editionAllService;
    final EditionBySelectionBonsService editionBySelectionBonsService;
    final EditionByTiersPayantService editionByTiersPayantService;
    final EditionByTypeTiersPayantService editionByTypeTiersPayantService;
    final EditionByGroupTiersService editionByGroupTiersService;
    final FacturationServiceRegistry facturationServiceRegistry;
    final AvoirService avoirService;
    final RapprochementService rapprochementService;
    final EditionDataService editionDataService;
    final RecapitulatifMensuelService recapitulatifMensuelService;
    final PlanificationFacturationService planificationFacturationService;

    // --- rouages réels utiles aux fabriques du jeu d'essai ---
    final SaleIdGeneratorService saleIdGeneratorService;
    final AssuranceItemIdGeneratorService assuranceItemIdGeneratorService;
    final FactureIdGeneratorService factureIdGeneratorService;
    final TransactionIdGeneratorService transactionIdGeneratorService;

    FacturationServicesUnderTest(EntityManager entityManager) {
        this.facturationRepository = IntegrationPostgresDatabase.bean(FacturationRepository.class);
        this.thirdPartySaleLineRepository = IntegrationPostgresDatabase.bean(ThirdPartySaleLineRepository.class);
        this.avoirTiersPayantRepository = IntegrationPostgresDatabase.bean(AvoirTiersPayantRepository.class);
        this.factureTiersPayantRepository = IntegrationPostgresDatabase.bean(FactureTiersPayantRepository.class);
        this.planificationFacturationRepository = IntegrationPostgresDatabase.bean(PlanificationFacturationRepository.class);
        this.historiquePlanificationRepository = IntegrationPostgresDatabase.bean(HistoriquePlanificationRepository.class);
        this.tiersPayantRepository = IntegrationPostgresDatabase.bean(TiersPayantRepository.class);
        this.groupeTiersPayantRepository = IntegrationPostgresDatabase.bean(GroupeTiersPayantRepository.class);

        this.saleIdGeneratorService = new SaleIdGeneratorService(entityManager);
        this.assuranceItemIdGeneratorService = new AssuranceItemIdGeneratorService(entityManager);
        this.factureIdGeneratorService = new FactureIdGeneratorService(entityManager);
        this.transactionIdGeneratorService = new TransactionIdGeneratorService(entityManager);
        InvoiceGenerationCodeGeneratorService invoiceGenerationCodeGeneratorService = new InvoiceGenerationCodeGeneratorService(
            entityManager
        );

        this.editionAllService = new EditionAllService(
            thirdPartySaleLineRepository,
            facturationRepository,
            appConfigurationService,
            userService,
            factureIdGeneratorService,
            invoiceGenerationCodeGeneratorService
        );
        this.editionBySelectionBonsService = new EditionBySelectionBonsService(
            thirdPartySaleLineRepository,
            facturationRepository,
            appConfigurationService,
            userService,
            factureIdGeneratorService,
            invoiceGenerationCodeGeneratorService
        );
        this.editionByTiersPayantService = new EditionByTiersPayantService(
            thirdPartySaleLineRepository,
            facturationRepository,
            appConfigurationService,
            userService,
            factureIdGeneratorService,
            invoiceGenerationCodeGeneratorService
        );
        this.editionByTypeTiersPayantService = new EditionByTypeTiersPayantService(
            thirdPartySaleLineRepository,
            facturationRepository,
            appConfigurationService,
            userService,
            factureIdGeneratorService,
            invoiceGenerationCodeGeneratorService
        );
        this.editionByGroupTiersService = new EditionByGroupTiersService(
            thirdPartySaleLineRepository,
            facturationRepository,
            appConfigurationService,
            userService,
            factureIdGeneratorService,
            invoiceGenerationCodeGeneratorService
        );

        this.avoirService = new AvoirServiceImpl(
            avoirTiersPayantRepository,
            factureTiersPayantRepository,
            storageService,
            avoirPdfService,
            avoirExcelService
        );

        this.editionDataService = new EditionDataServiceImpl(
            facturationRepository,
            thirdPartySaleLineRepository,
            facturationPdfExportService,
            groupeFacturePdfExportService,
            new ReportExcelExportService(),
            appConfigurationService
        );

        RapprochementServiceImpl rapprochement = new RapprochementServiceImpl(
            appConfigurationService,
            rapprochementPdfReportService,
            new ReportExcelExportService()
        );
        // Son EntityManager est injecté par @PersistenceContext, que personne ne traite hors Spring.
        ReflectionTestUtils.setField(rapprochement, "entityManager", entityManager);
        this.rapprochementService = rapprochement;

        this.facturationServiceRegistry = new FacturationServiceRegistry(
            editionAllService,
            editionBySelectionBonsService,
            editionByTiersPayantService,
            editionByTypeTiersPayantService,
            editionByGroupTiersService
        );

        RecapitulatifMensuelServiceImpl recapitulatif = new RecapitulatifMensuelServiceImpl(appConfigurationService);
        ReflectionTestUtils.setField(recapitulatif, "entityManager", entityManager);
        this.recapitulatifMensuelService = recapitulatif;

        this.planificationFacturationService = new PlanificationFacturationServiceImpl(
            planificationFacturationRepository,
            historiquePlanificationRepository,
            facturationServiceRegistry,
            tiersPayantRepository,
            groupeTiersPayantRepository,
            new PlanificationStatutService(planificationFacturationRepository, historiquePlanificationRepository)
        );
    }
}

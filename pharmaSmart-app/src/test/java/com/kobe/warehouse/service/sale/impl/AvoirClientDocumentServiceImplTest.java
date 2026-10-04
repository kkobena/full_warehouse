package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.AvoirClientUtilisationRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.sale.AvoirClientNotificationService;
import com.kobe.warehouse.service.sale.dto.AvoirClientDocumentDTO;
import com.kobe.warehouse.service.sale.dto.CloturerAvoirRequest;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AvoirClientDocumentServiceImplTest {

    @Mock private AvoirClientRepository repository;
    @Mock private SalesLineRepository lineRepository;
    @Mock private ReferenceService referenceService;
    @Mock private StorageService storageService;
    @Mock private StockProduitRepository stockRepository;
    @Mock private AvoirClientNotificationService notificationService;
    @Mock private AppConfigurationService configurationService;
    @Mock private AvoirClientUtilisationRepository utilisationRepository;
    @Mock private LotService lotService;
    @Mock private LotStockLocationService lotStockLocationService;
    @Mock private LogsService logsService;
    @Mock private CashRegisterService cashRegisterService;
    @Mock private FinancialTransactionService financialTransactionService;
    @Mock private PaymentModeRepository paymentModeRepository;

    private AvoirClientDocumentServiceImpl service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1);
        user.setFirstName("Alice");
        user.setLastName("Martin");
        Magasin magasin = new Magasin();
        magasin.setId(2);
        user.setMagasin(magasin);
        service = new AvoirClientDocumentServiceImpl(
            repository, lineRepository, referenceService, storageService, stockRepository,
            notificationService, configurationService, utilisationRepository,
            lotService, lotStockLocationService, logsService,
            cashRegisterService, financialTransactionService, paymentModeRepository
        );
        // Par défaut une caisse est ouverte : seuls les tests de remboursement s'intéressent à l'autre cas.
        lenient().when(cashRegisterService.getOpiningCashRegisterByUser(any())).thenReturn(Optional.of(new CashRegister()));
        lenient().when(paymentModeRepository.findById(any())).thenReturn(Optional.of(new PaymentMode()));
    }

    @Test
    void createsCreditFromSaleLineAndSkipsCompletelyEmptyRequest() {
        SalesLine line = saleLine();
        Customer customer = customer();
        when(configurationService.getDelaiValiditeAvoir()).thenReturn(30);
        when(referenceService.buildNumAvoirClient()).thenReturn("AV-1");
        when(storageService.getUser()).thenReturn(user);

        service.createAvoirsFromSale(line, customer);

        ArgumentCaptor<AvoirClient> captor = ArgumentCaptor.forClass(AvoirClient.class);
        verify(repository).save(captor.capture());
        AvoirClient saved = captor.getValue();
        assertEquals("AV-1", saved.getReference());
        assertEquals(1_000, saved.getMontant());
        assertEquals(LocalDate.now().plusDays(30), saved.getDateExpiration());
        assertSame(customer, saved.getCustomer());

        SalesLine empty = saleLine();
        empty.setQuantityAvoir(0);
        service.createAvoirsFromSale(empty, null);
    }

    @Test
    void cancelsExistingCreditAndIgnoresMissingOne() {
        AvoirClient avoir = avoir();
        when(repository.findBySalesLineId(10L)).thenReturn(Optional.of(avoir));
        when(repository.findBySalesLineId(99L)).thenReturn(Optional.empty());

        service.cancelAvoirsFromSale(10L);
        service.cancelAvoirsFromSale(99L);

        assertEquals(AvoirClientStatut.ANNULE, avoir.getStatut());
        verify(repository).save(avoir);
    }

    @Test
    void linksOpenCreditsToMatchingCommandeAndHandlesEarlyReturns() {
        Commande commande = org.mockito.Mockito.mock(Commande.class);
        OrderLine orderLine = org.mockito.Mockito.mock(OrderLine.class);
        FournisseurProduit supplier = org.mockito.Mockito.mock(FournisseurProduit.class);
        Produit product = new Produit();
        product.setId(100);
        when(commande.getOrderLines()).thenReturn(List.of(orderLine));
        when(orderLine.getFournisseurProduit()).thenReturn(supplier);
        when(supplier.getProduit()).thenReturn(product);
        AvoirClient avoir = avoir();
        when(repository.existsByStatutAndCommandeIsNull(AvoirClientStatut.OUVERT)).thenReturn(true);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
            .thenReturn(List.of(avoir));

        service.linkCommandeToAvoirs(commande);

        assertSame(commande, avoir.getCommande());
        verify(repository).saveAll(List.of(avoir));
        verify(notificationService).notifierProduitsDisponibles(avoir);
    }

    @Test
    void doesNotQueryCreditsWhenNoOpenCreditExists() {
        Commande commande = org.mockito.Mockito.mock(Commande.class);
        when(repository.existsByStatutAndCommandeIsNull(AvoirClientStatut.OUVERT)).thenReturn(false);

        service.linkCommandeToAvoirs(commande);

        verify(commande, never()).getOrderLines();
        verify(notificationService, never()).notifierProduitsDisponibles(any());
        verify(repository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class));
    }

    @Test
    void rejectsMissingClosedInsufficientStockAndExcessiveAmount() {
        CloturerAvoirRequest request = new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, "x", 100);
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertThrows(GenericError.class, () -> service.cloturerAvoir(99, request));

        AvoirClient closed = avoir().setStatut(AvoirClientStatut.CLOTURE);
        when(repository.findById(1)).thenReturn(Optional.of(closed));
        assertThrows(GenericError.class, () -> service.cloturerAvoir(1, request));

        AvoirClient open = avoir();
        when(repository.findById(2)).thenReturn(Optional.of(open));
        when(storageService.getUser()).thenReturn(user);
        when(stockRepository.findTotalQuantityByMagasinIdIdAndProduitId(2, 100)).thenReturn(-1);
        assertThrows(GenericError.class, () -> service.cloturerAvoir(2,
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, "x", null)));

        AvoirClient noProduct = avoir().setProduit(null);
        when(repository.findById(3)).thenReturn(Optional.of(noProduct));
        assertThrows(GenericError.class, () -> service.cloturerAvoir(3,
            new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, "x", 1_001)));
    }

    @Test
    void partiallyUsesCreditWithoutClosingIt() {
        AvoirClient avoir = avoir().setProduit(null);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(repository.save(avoir)).thenReturn(avoir);

        AvoirClientDocumentDTO result = service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.COMPENSATION_VENTE, "partiel", 300));

        assertEquals(300, result.montantUtilise());
        assertEquals(700, result.montantRestant());
        assertEquals(AvoirClientStatut.OUVERT, result.statut());
        verify(utilisationRepository).save(any());
        verify(lineRepository, never()).save(any());
        verify(notificationService, never()).notifierProduitsDisponibles(any());
    }

    /** Remboursement en espèces ou par CB : une sortie de caisse au mode de paiement choisi, sur une caisse ouverte. */
    @ParameterizedTest
    @EnumSource(value = ModeClotureAvoir.class, names = {"REMBOURSEMENT_ESPECES", "REMBOURSEMENT_CB"})
    void refundWritesACashOutflowAtTheChosenPaymentMode(ModeClotureAvoir mode) {
        AvoirClient avoir = avoir().setProduit(null);
        String code = mode == ModeClotureAvoir.REMBOURSEMENT_ESPECES ? "CASH" : "CB";
        PaymentMode paymentMode = new PaymentMode();
        paymentMode.setCode(code);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(cashRegisterService.getOpiningCashRegisterByUser(user)).thenReturn(Optional.of(new CashRegister()));
        when(paymentModeRepository.findById(code)).thenReturn(Optional.of(paymentMode));
        when(repository.save(avoir)).thenReturn(avoir);

        service.cloturerAvoir(1, new CloturerAvoirRequest(mode, "client pressé", 300));

        ArgumentCaptor<FinancialTransactionDTO> captor = ArgumentCaptor.forClass(FinancialTransactionDTO.class);
        verify(financialTransactionService).create(captor.capture());
        FinancialTransactionDTO sortie = captor.getValue();
        assertEquals(300, sortie.getAmount());
        assertEquals(TypeFinancialTransaction.SORTIE_CAISSE, sortie.getTypeTransaction());
        assertSame(paymentMode, sortie.getPaymentMode());
        assertTrue(sortie.getCommentaire().contains(avoir.getReference()));
        assertTrue(sortie.getCommentaire().contains("client pressé"));
    }

    @ParameterizedTest
    @EnumSource(value = ModeClotureAvoir.class, names = {"REMBOURSEMENT_ESPECES", "REMBOURSEMENT_CB"})
    void refundWithoutOpenCashRegisterChangesNothing(ModeClotureAvoir mode) {
        AvoirClient avoir = avoir().setProduit(null);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(cashRegisterService.getOpiningCashRegisterByUser(user)).thenReturn(Optional.empty());

        assertThrows(GenericError.class, () -> service.cloturerAvoir(1, new CloturerAvoirRequest(mode, null, null)));

        assertEquals(AvoirClientStatut.OUVERT, avoir.getStatut());
        assertEquals(0, avoir.getMontantUtilise());
        verify(utilisationRepository, never()).save(any());
        verify(financialTransactionService, never()).create(any());
    }

    @ParameterizedTest
    @EnumSource(value = ModeClotureAvoir.class, names = {"BON_AVOIR", "COMPENSATION_VENTE"})
    void otherModesDoNotTouchTheCashRegister(ModeClotureAvoir mode) {
        AvoirClient avoir = avoir().setProduit(null);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(repository.save(avoir)).thenReturn(avoir);

        service.cloturerAvoir(1, new CloturerAvoirRequest(mode, null, null));

        verify(cashRegisterService, never()).getOpiningCashRegisterByUser(any());
        verify(financialTransactionService, never()).create(any());
    }

    @Test
    void fullyClosesCreditResetsSaleLineWithoutNotifyingTheCustomer() {
        AvoirClient avoir = avoir();
        avoir.setMontantUtilise(200);
        SalesLine line = avoir.getSalesLine();
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(stockRepository.findTotalQuantityByMagasinIdIdAndProduitId(2, 100)).thenReturn(null);
        avoir.setQuantite(0);
        when(repository.save(avoir)).thenReturn(avoir);

        AvoirClientDocumentDTO result = service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, "complet", null));

        assertEquals(AvoirClientStatut.CLOTURE, result.statut());
        assertEquals(0, line.getQuantityAvoir());
        assertSame(user, avoir.getClosedBy());
        verify(lineRepository).save(line);
        // Le client est au comptoir : il est prévenu à la réception de la commande, pas à la clôture.
        verify(notificationService, never()).notifierProduitsDisponibles(any());
    }

    /** Bugs 2 et 3 du plan PLAN-VENTE-SUR-STOCK-ERRONE. */
    @Test
    void handsOverProductWhenStockIsZeroAndDebitsLots() {
        AvoirClient avoir = avoir();
        Storage rayon = new Storage();
        rayon.setId(1);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
        when(stockRepository.findTotalQuantityByMagasinIdIdAndProduitId(2, 100)).thenReturn(0);
        when(repository.save(avoir)).thenReturn(avoir);

        service.cloturerAvoir(1, new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null));

        verify(lotService).adjustLots(avoir.getProduit(), -2);
        verify(lotStockLocationService).debitFefo(avoir.getProduit(), rayon, 2);
        verify(stockRepository, never()).save(any());
    }

    /** Bug 5 du plan PLAN-VENTE-SUR-STOCK-ERRONE. */
    @Test
    void refundIgnoresStockAndCreditsOwedQuantityBack() {
        AvoirClient avoir = avoir();
        Storage rayon = new Storage();
        rayon.setId(1);
        StockProduit stock = new StockProduit();
        stock.setQtyStock(-2);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
        when(stockRepository.findOneByProduitIdAndStockageId(100, 1)).thenReturn(stock);
        when(repository.save(avoir)).thenReturn(avoir);

        service.cloturerAvoir(1, new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_ESPECES, null, null));

        assertEquals(0, stock.getQtyStock());
        assertEquals(0, stock.getQtyVirtual());
        verify(stockRepository, never()).findTotalQuantityByMagasinIdIdAndProduitId(any(), any());
        verify(lotService, never()).adjustLots(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(logsService).create(org.mockito.ArgumentMatchers.eq(TransactionType.AVOIR_SOLDE_SANS_PRODUIT),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("1"));
    }

    @ParameterizedTest
    @EnumSource(value = ModeClotureAvoir.class, names = "RETOUR_PRODUIT", mode = EnumSource.Mode.EXCLUDE)
    void everyModeWithoutProductCreditsStockBack(ModeClotureAvoir mode) {
        AvoirClient avoir = avoir();
        StockProduit stock = stockRayon(-2);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(repository.save(avoir)).thenReturn(avoir);

        service.cloturerAvoir(1, new CloturerAvoirRequest(mode, null, null));

        assertEquals(0, stock.getQtyStock());
        verify(stockRepository).save(stock);
        verify(lotStockLocationService, never()).debitFefo(any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void refundLogsReferenceModeAndStockMovement() {
        AvoirClient avoir = avoir();
        stockRayon(-2);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(repository.save(avoir)).thenReturn(avoir);

        service.cloturerAvoir(1, new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_CB, null, null));

        ArgumentCaptor<String> comment = ArgumentCaptor.forClass(String.class);
        verify(logsService).create(org.mockito.ArgumentMatchers.eq(TransactionType.AVOIR_SOLDE_SANS_PRODUIT),
            comment.capture(), org.mockito.ArgumentMatchers.eq("1"));
        assertTrue(comment.getValue().contains("AV-1"), comment.getValue());
        assertTrue(comment.getValue().contains("REMBOURSEMENT_CB"), comment.getValue());
        assertTrue(comment.getValue().contains("-2 -> 0"), comment.getValue());
    }

    @Test
    void partialRefundCreditsStockOnlyOnceTheCreditIsSettled() {
        AvoirClient avoir = avoir();
        StockProduit stock = stockRayon(-2);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(repository.save(avoir)).thenReturn(avoir);

        service.cloturerAvoir(1, new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, null, 400));
        assertEquals(-2, stock.getQtyStock(), "une utilisation partielle ne touche pas au stock");
        verify(stockRepository, never()).save(any());

        service.cloturerAvoir(1, new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, null, 600));
        assertEquals(0, stock.getQtyStock());
        verify(stockRepository).save(stock);
    }

    @Test
    void refusesAnAmountForAProductHandOver() {
        AvoirClient avoir = avoir();
        when(repository.findById(1)).thenReturn(Optional.of(avoir));

        assertThrows(GenericError.class, () -> service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, 300)));

        assertEquals(AvoirClientStatut.OUVERT, avoir.getStatut());
        assertEquals(0, avoir.getMontantUtilise());
        verify(lotService, never()).adjustLots(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(utilisationRepository, never()).save(any());
    }

    /** Avoir de 4 unités à 500 : chaque remise sort ses unités des lots tout de suite et impute leur prix. */
    @Test
    void partialHandOverInUnitsDebitsOnlyTheHandedUnits() {
        AvoirClient avoir = avoir().setQuantite(4).setMontant(2_000);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(stockRepository.findTotalQuantityByMagasinIdIdAndProduitId(2, 100)).thenReturn(5);
        when(repository.save(avoir)).thenReturn(avoir);

        AvoirClientDocumentDTO result = service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null, 1));

        assertEquals(AvoirClientStatut.OUVERT, result.statut());
        assertEquals(500, result.montantUtilise());
        assertEquals(1, result.quantiteRemise());
        assertEquals(3, result.quantiteRestante());
        verify(lotService).adjustLots(any(), org.mockito.ArgumentMatchers.eq(-1));
        verify(lotStockLocationService).debitFefo(any(), any(), org.mockito.ArgumentMatchers.eq(1));
        verify(stockRepository, never()).save(any());
    }

    @Test
    void lastHandOverSettlesTheCreditAndCreditsNothingBackToStock() {
        AvoirClient avoir = avoir().setQuantite(4).setMontant(2_000).setQuantiteRemise(3).setMontantUtilise(1_500);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(stockRepository.findTotalQuantityByMagasinIdIdAndProduitId(2, 100)).thenReturn(0);
        when(repository.save(avoir)).thenReturn(avoir);

        AvoirClientDocumentDTO result = service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null));

        assertEquals(AvoirClientStatut.CLOTURE, result.statut());
        assertEquals(2_000, result.montantUtilise(), "la dernière remise prend tout le solde");
        assertEquals(0, result.quantiteRestante());
        verify(lotService).adjustLots(any(), org.mockito.ArgumentMatchers.eq(-1));
        verify(stockRepository, never()).save(any());
    }

    @Test
    void refusesMoreUnitsThanRemain() {
        AvoirClient avoir = avoir().setQuantite(4).setMontant(2_000).setQuantiteRemise(1).setMontantUtilise(500);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));

        assertThrows(GenericError.class, () -> service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null, 4)));
        assertThrows(GenericError.class, () -> service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null, 0)));

        assertEquals(1, avoir.getQuantiteRemise());
        verify(lotService, never()).adjustLots(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    /** Une unité a déjà été remise : solder le reste en argent ne recrédite que les trois autres. */
    @Test
    void settlingInMoneyAfterAPartialHandOverCreditsOnlyTheUnhandedUnits() {
        AvoirClient avoir = avoir().setQuantite(4).setMontant(2_000).setQuantiteRemise(1).setMontantUtilise(500);
        StockProduit stock = stockRayon(-3);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(repository.save(avoir)).thenReturn(avoir);

        AvoirClientDocumentDTO result = service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, null, null));

        assertEquals(AvoirClientStatut.CLOTURE, result.statut());
        assertEquals(0, stock.getQtyStock(), "3 unités recréditées, pas les 4 de l'avoir");
    }

    @Test
    void refusedHandOverStatesMissingUnitsAndWritesNothing() {
        AvoirClient avoir = avoir();
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(stockRepository.findTotalQuantityByMagasinIdIdAndProduitId(2, 100)).thenReturn(-3);

        GenericError error = assertThrows(GenericError.class, () -> service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null)));

        assertTrue(error.getMessage().contains("il manque 3"), error.getMessage());
        assertEquals(AvoirClientStatut.OUVERT, avoir.getStatut());
        assertEquals(0, avoir.getMontantUtilise());
        verify(utilisationRepository, never()).save(any());
        verify(lotService, never()).adjustLots(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void refundWithoutStockRowInRayonCreditsNothing() {
        AvoirClient avoir = avoir();
        Storage rayon = new Storage();
        rayon.setId(1);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
        when(stockRepository.findOneByProduitIdAndStockageId(100, 1)).thenReturn(null);
        when(repository.save(avoir)).thenReturn(avoir);

        AvoirClientDocumentDTO result = service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_ESPECES, null, null));

        assertEquals(AvoirClientStatut.CLOTURE, result.statut());
        verify(stockRepository, never()).save(any());
        verify(logsService, never()).create(any(TransactionType.class), any(String.class), any(String.class));
    }

    @Test
    void creditWithoutProductOrQuantityHasNoStockEffect() {
        AvoirClient sansProduit = avoir().setProduit(null);
        AvoirClient sansQuantite = avoir().setId(2).setQuantite(0);
        when(repository.findById(1)).thenReturn(Optional.of(sansProduit));
        when(repository.findById(2)).thenReturn(Optional.of(sansQuantite));
        when(storageService.getUser()).thenReturn(user);
        when(repository.save(any(AvoirClient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cloturerAvoir(1, new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null));
        service.cloturerAvoir(2, new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_ESPECES, null, null));

        verify(stockRepository, never()).findTotalQuantityByMagasinIdIdAndProduitId(any(), any());
        verify(stockRepository, never()).findOneByProduitIdAndStockageId(any(), any());
        verify(lotService, never()).adjustLots(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(logsService, never()).create(any(TransactionType.class), any(String.class), any(String.class));
    }

    private StockProduit stockRayon(int quantite) {
        Storage rayon = new Storage();
        rayon.setId(1);
        StockProduit stock = new StockProduit();
        stock.setQtyStock(quantite);
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
        when(stockRepository.findOneByProduitIdAndStockageId(100, 1)).thenReturn(stock);
        return stock;
    }

    @Test
    void mapsCustomerListsPagesAndNullableRelationships() {
        AvoirClient complete = avoir();
        complete.setDateExpiration(LocalDate.now().plusDays(3));
        Commande commande = org.mockito.Mockito.mock(Commande.class);
        when(commande.getReceiptReference()).thenReturn("CMD-1");
        complete.setCommande(commande);
        AvoirClient minimal = new AvoirClient()
            .setId(2).setReference("AV-2").setMontant(100).setQuantite(1)
            .setDateExpiration(LocalDate.now().plusDays(8));
        when(repository.findByCustomerIdOrderByCreatedAtDesc(8)).thenReturn(List.of(complete, minimal));
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
            org.mockito.ArgumentMatchers.eq(pageable))).thenReturn(new PageImpl<>(List.of(complete)));

        List<AvoirClientDocumentDTO> customerCredits = service.findAllByCustomer(8);

        assertEquals(2, customerCredits.size());
        assertTrue(customerCredits.getFirst().procheExpiration());
        assertEquals("CMD-1", customerCredits.getFirst().commandeReference());
        assertNull(customerCredits.get(1).customerName());
        assertFalse(customerCredits.get(1).procheExpiration());
        assertEquals(1, service.findAll("AV", LocalDate.now().minusDays(1), LocalDate.now(),
            AvoirClientStatut.OUVERT, pageable).getTotalElements());
    }

    private AvoirClient avoir() {
        return new AvoirClient()
            .setId(1)
            .setReference("AV-1")
            .setStatut(AvoirClientStatut.OUVERT)
            .setQuantite(2)
            .setMontant(1_000)
            .setCustomer(customer())
            .setProduit(saleLine().getProduit())
            .setSalesLine(saleLine())
            .setCreatedBy(user)
            .setDateExpiration(LocalDate.now().plusDays(30));
    }

    private Customer customer() {
        UninsuredCustomer customer = new UninsuredCustomer();
        customer.setId(8);
        customer.setFirstName("Jean");
        customer.setLastName(null);
        return customer;
    }

    private SalesLine saleLine() {
        Produit product = new Produit();
        product.setId(100);
        product.setLibelle("Produit test");
        product.setCodeEanLaboratoire("EAN-1");
        Sales sale = new com.kobe.warehouse.domain.CashSale();
        sale.setId(5L);
        sale.setSaleDate(LocalDate.now());
        sale.setNumberTransaction("SALE-5");
        SalesLine line = new SalesLine();
        line.setId(10L);
        line.setSaleDate(sale.getSaleDate());
        line.setSales(sale);
        line.setProduit(product);
        line.setQuantityAvoir(2);
        line.setRegularUnitPrice(500);
        return line;
    }
}


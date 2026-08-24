package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RemiseClient;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.PaymentModeRepository;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.PaymentAmountException;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.SalesManager;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

    @Mock
    private SalesRepository salesRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UninsuredCustomerRepository uninsuredCustomerRepository;
    @Mock
    private PaymentModeRepository paymentModeRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private CashSaleRepository cashSaleRepository;
    @Mock
    private SalesLineService salesLineService;
    @Mock
    private SaleLineServiceFactory saleLineServiceFactory;
    @Mock
    private PaymentService paymentService;
    @Mock
    private ReferenceService referenceService;
    @Mock
    private PosteRepository posteRepository;
    @Mock
    private UtilisationCleSecuriteService utilisationCleSecuriteService;
    @Mock
    private RemiseRepository remiseRepository;
    @Mock
    private CustomerDisplayService customerDisplayService;
    @Mock
    private SaleIdGeneratorService idGeneratorService;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private SalesManager salesManager;
    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private CashRegisterService cashRegisterService;

    @Mock
    private com.kobe.warehouse.service.declaration_ca.DeclarationCaService declarationCaService;

    @Mock
    private com.kobe.warehouse.service.declaration_ca.PonctionService ponctionService;

    private SaleServiceImpl saleService;
    private CashSale testSale;
    private SalesLine testSalesLine;
    private LocalDate testDate;
    private AppUser testUser;
    private Storage testStorage;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now();
        testUser = new AppUser();
        testUser.setId(1);
        testUser.setLogin("testUser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setActivated(true);
        testUser.setMagasin(new Magasin());

        testStorage = new Storage();
        testStorage.setId(1);

        lenient().when(saleLineServiceFactory.getService(TypeVente.CashSale))
            .thenReturn(salesLineService);
        lenient().when(referenceService.buildNumSale()).thenReturn("NUM");
        lenient().when(referenceService.buildNumPreventeSale()).thenReturn("PRE");
        lenient().when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(testStorage);
        lenient().when(storageService.getUser()).thenReturn(testUser);

        ObjectMapper mapper = new ObjectMapper();
        lenient().when(objectMapper.createArrayNode()).thenReturn(mapper.createArrayNode());
        lenient().when(objectMapper.createObjectNode()).thenReturn(mapper.createObjectNode());

        saleService = new SaleServiceImpl(
            salesRepository, userRepository, uninsuredCustomerRepository,
            paymentModeRepository, storageService, cashSaleRepository,
            cashRegisterService, saleLineServiceFactory,
            paymentService, referenceService, posteRepository,
            utilisationCleSecuriteService, remiseRepository,
            customerDisplayService, idGeneratorService, objectMapper,
            salesManager, appConfigurationService, declarationCaService, ponctionService
        );

        setupTestData();
    }

    private void setupTestData() {
        testSale = new CashSale();
        testSale.setId(1L);
        testSale.setSaleDate(testDate);
        testSale.setSalesAmount(1000);
        testSale.setNetAmount(1000);
        testSale.setCostAmount(500);
        testSale.setStatut(SalesStatut.ACTIVE);
        testSale.setSalesLines(new HashSet<>());
        testSale.setUser(testUser);
        testSale.setMagasin(new Magasin());
        testSale.setRestToPay(1000);
        testSale.setAmountToBePaid(1000);
        testSale.setPayrollAmount(1000);
        testSale.setDiscountAmount(0);
        testSale.setTaxAmount(0);

        testSalesLine = new SalesLine();
        testSalesLine.setId(1L);
        testSalesLine.setSalesAmount(500);
        testSalesLine.setQuantitySold(1);
        testSalesLine.setQuantityRequested(1);
        testSalesLine.setQuantityUg(0);
        testSalesLine.setRegularUnitPrice(500);
        testSalesLine.setCostAmount(250);
        testSalesLine.setSales(testSale);
        testSalesLine.setTaxValue(0);

        Produit produit = new Produit();
        produit.setCodeRemise(CodeRemise.NONE);
        testSalesLine.setProduit(produit);

        testSale.getSalesLines().add(testSalesLine);
    }


    @Test
    void testFromDTOOldCashSale() {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setSalesAmount(1000);
        dto.setUserFullName("userLogin");
        dto.setCustomerNum("CUST001");

        AppUser user = new AppUser();
        user.setMagasin(new Magasin());
        when(userRepository.findOneByLogin("userLogin")).thenReturn(Optional.of(user));
        when(uninsuredCustomerRepository.findOneByCode("CUST001")).thenReturn(
            Optional.of(new UninsuredCustomer()));

        CashSale result = saleService.fromDTOOldCashSale(dto);

        assertNotNull(result);
        assertEquals(1000, result.getSalesAmount());
        assertEquals(SalesStatut.CLOSED, result.getStatut());
        assertNotNull(result.getCustomer());
    }

    @Test
    void testFromDTOOldCashSale_UsesImportFallbackForUnknownUsers() {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setUserFullName("missing-user");
        dto.setSellerUserName("missing-seller");
        when(userRepository.findOneByLogin("missing-user")).thenReturn(Optional.empty());
        when(userRepository.findOneByLogin("missing-seller")).thenReturn(Optional.empty());
        when(userRepository.findOneByLogin(Constants.SYSTEM)).thenReturn(Optional.of(testUser));

        CashSale result = saleService.fromDTOOldCashSale(dto);

        assertEquals(testUser, result.getUser());
        assertEquals(testUser, result.getSeller());
    }

    @Test
    void testFromDTOOldCashSale_UsesImportFallbackWhenUsersAreBlank() {
        CashSaleDTO dto = new CashSaleDTO();
        when(userRepository.findOneByLogin(Constants.SYSTEM)).thenReturn(Optional.of(testUser));

        CashSale result = saleService.fromDTOOldCashSale(dto);

        assertSame(testUser, result.getUser());
        assertSame(testUser, result.getSeller());
    }

    @Test
    void testBuildPaymentFromDTO_CashSale() {
        PaymentDTO dto = new PaymentDTO();
        dto.setNetAmount(1000);
        dto.setPaymentCode(Constants.MODE_ESP);

        PaymentMode mode = new PaymentMode();
        when(paymentModeRepository.findById(Constants.MODE_ESP)).thenReturn(Optional.of(mode));

        SalePayment result = saleService.buildPaymentFromDTO(dto, new CashSale());

        assertNotNull(result);
        assertEquals(1000, result.getExpectedAmount());
        assertEquals(TypeFinancialTransaction.CASH_SALE, result.getTypeFinancialTransaction());
    }

    @Test
    void testBuildPaymentFromDTO_ThirdPartySaleUsesFallbackMode() {
        PaymentDTO dto = new PaymentDTO();
        dto.setNetAmount(800);
        dto.setPaidAmount(750);
        dto.setPaymentCode("UNKNOWN");
        PaymentMode fallback = new PaymentMode();
        when(paymentModeRepository.findById("UNKNOWN")).thenReturn(Optional.empty());
        when(paymentModeRepository.getReferenceById(Constants.MODE_ESP)).thenReturn(fallback);

        SalePayment result = saleService.buildPaymentFromDTO(dto, new ThirdPartySales());

        assertSame(fallback, result.getPaymentMode());
        assertEquals(TypeFinancialTransaction.CREDIT_SALE, result.getTypeFinancialTransaction());
    }

    @Test
    void testSetCustomer() {
        UpdateSaleInfo info = new UpdateSaleInfo(new SaleId(1L, testDate), 1);
        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);
        when(uninsuredCustomerRepository.getReferenceById(1)).thenReturn(new UninsuredCustomer());

        saleService.setCustomer(info);

        assertNotNull(testSale.getCustomer());
        verify(cashSaleRepository).save(testSale);
    }

    @Test
    void testRemoveCustomer() {
        testSale.setCustomer(new UninsuredCustomer());
        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);

        saleService.removeCustomer(new SaleId(1L, testDate));

        assertNull(testSale.getCustomer());
        verify(cashSaleRepository).save(testSale);
    }

    @Test
    void testCreateCashSale() {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setCustomerId(1);
        dto.setCassierId(1);
        dto.setSellerId(1);
        dto.setSellerUserName("testUser");
        dto.setUserFullName("testUser");
        SaleLineDTO lineDTO = new SaleLineDTO();
        dto.setSalesLines(List.of(lineDTO));

        when(uninsuredCustomerRepository.getReferenceById(1)).thenReturn(new UninsuredCustomer());

        when(salesLineService.createSaleLineFromDTO(any(), anyInt())).thenReturn(testSalesLine);

        CashSale savedSale = new CashSale();
        savedSale.setId(1L);
        savedSale.setUser(testUser);
        savedSale.setCaissier(testUser);
        savedSale.setSeller(testUser);
        savedSale.setMagasin(testUser.getMagasin());
        savedSale.setSalesLines(new HashSet<>(List.of(testSalesLine)));

        when(salesRepository.save(any())).thenReturn(savedSale);

        System.out.println("[DEBUG_LOG] Calling createCashSale");
        CashSaleDTO result = saleService.createCashSale(dto);

        assertNotNull(result);
        verify(salesRepository).save(any());
        verify(salesLineService).saveSalesLine(any());
    }

    @Test
    void testUpdateItemQuantityRequested() throws Exception {
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setSaleCompositeId(new SaleId(1L, testDate));

        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);
        when(salesManager.updateItemQuantityRequested(any(), any(), anyBoolean())).thenReturn(dto);

        SaleLineDTO result = saleService.updateItemQuantityRequested(dto, true);

        assertNotNull(result);
        verify(salesManager).updateItemQuantityRequested(eq(dto), eq(testSale), eq(true));
    }

    @Test
    void testUpdateItemQuantitySold() {
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setSaleCompositeId(new SaleId(1L, testDate));

        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);
        when(salesManager.updateItemQuantitySold(any(), any())).thenReturn(dto);

        SaleLineDTO result = saleService.updateItemQuantitySold(dto);

        assertNotNull(result);
        verify(salesManager).updateItemQuantitySold(eq(dto), eq(testSale));
    }

    @Test
    void testUpdateItemRegularPrice() {
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setSaleCompositeId(new SaleId(1L, testDate));

        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);
        when(salesManager.updateItemRegularPrice(any(), any())).thenReturn(dto);

        SaleLineDTO result = saleService.updateItemRegularPrice(dto);

        assertNotNull(result);
        verify(salesManager).updateItemRegularPrice(eq(dto), eq(testSale));
    }

    @Test
    void testAddOrUpdateSaleLine_New() {
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleCompositeId(new SaleId(1L, testDate));
        dto.setProduitId(1);

        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);
        when(salesManager.addOrUpdateSaleLine(any(), any())).thenReturn(dto);

        SaleLineDTO result = saleService.addOrUpdateSaleLine(dto);

        assertNotNull(result);
        verify(salesManager).addOrUpdateSaleLine(eq(dto), eq(testSale));
    }

    @Test
    void testSave() throws Exception {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setSaleId(new SaleId(1L, testDate));
        dto.setCustomerId(1);
        dto.setPayrollAmount(1000);
        dto.setAmountToBePaid(1000);
        dto.setRestToPay(0);

        CashSale eagerSale = new CashSale();
        eagerSale.setId(1L);
        eagerSale.setSaleDate(testDate);
        eagerSale.setRestToPay(1000);
        eagerSale.setAmountToBePaid(1000);
        eagerSale.setPayrollAmount(1000);
        eagerSale.setNetAmount(1000);
        eagerSale.setSalesLines(new HashSet<>(List.of(testSalesLine)));
        eagerSale.setUser(testUser);

        when(cashSaleRepository.findOneWithEagerSalesLines(anyLong(), any())).thenReturn(
            Optional.of(eagerSale));
        when(uninsuredCustomerRepository.getReferenceById(1)).thenReturn(new UninsuredCustomer());
        lenient().when(storageService.getUser()).thenReturn(testUser);

        FinalyseSaleDTO result = saleService.save(dto);

        assertNotNull(result);
        assertTrue(result.success());
        assertEquals(SalesStatut.CLOSED, eagerSale.getStatut());
        verify(cashSaleRepository).findOneWithEagerSalesLines(1L, testDate);
        InOrder finalizationOrder = org.mockito.Mockito.inOrder(paymentService,
            declarationCaService, salesRepository);
        finalizationOrder.verify(paymentService).buildPaymentFromFromPaymentDTO(eagerSale, dto);
        finalizationOrder.verify(declarationCaService).appliquerExclusions(eagerSale);
        finalizationOrder.verify(salesRepository).save(eagerSale);
    }

    @Test
    void testSave_SaleNotFound_HasNoSideEffect() {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setSaleId(new SaleId(42L, testDate));
        when(cashSaleRepository.findOneWithEagerSalesLines(42L, testDate))
            .thenReturn(Optional.empty());

        assertThrows(GenericError.class, () -> saleService.save(dto));

        verify(paymentService, never()).buildPaymentFromFromPaymentDTO(any(), any());
        verify(declarationCaService, never()).appliquerExclusions(any());
        verify(salesRepository, never()).save(any());
    }

    @Test
    void testSave_InsufficientPayment_DoesNotCreatePaymentOrDeclaration() {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setSaleId(new SaleId(1L, testDate));
        dto.setAmountToBePaid(1000);
        dto.setPayrollAmount(900);
        dto.setRestToPay(100);
        when(cashSaleRepository.findOneWithEagerSalesLines(1L, testDate))
            .thenReturn(Optional.of(testSale));

        assertThrows(PaymentAmountException.class, () -> saleService.save(dto));

        verify(paymentService, never()).buildPaymentFromFromPaymentDTO(any(), any());
        verify(declarationCaService, never()).appliquerExclusions(any());
        verify(salesRepository, never()).save(any());
    }

    @Test
    void testPutCashSaleOnHold_DeleteIfEmpty() {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setSaleId(new SaleId(1L, testDate));
        testSale.setSalesLines(new HashSet<>());

        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);

        saleService.putCashSaleOnHold(dto);

        verify(salesRepository).delete(testSale);
    }

    @Test
    void testDeleteSaleLineById() {
        SaleLineId id = new SaleLineId(1L, testDate);
        when(salesLineService.getOneById(id)).thenReturn(testSalesLine);

        saleService.deleteSaleLineById(id);

        verify(salesManager).deleteSaleLineById(testSalesLine);
    }

    @Test
    void testDeleteSalePrevente() {
        SaleId id = new SaleId(1L, testDate);
        when(salesRepository.findOneWithEagerSalesLines(anyLong(), any())).thenReturn(
            Optional.of(testSale));

        saleService.deleteSalePrevente(id);

        verify(salesRepository).delete(testSale);
    }

    @Test
    void testCancelCashSale() {
        SaleId id = new SaleId(1L, testDate);
        testSale.setStatut(SalesStatut.CLOSED);
        when(storageService.getUser()).thenReturn(testUser);
        when(cashSaleRepository.findOneWithEagerSalesLines(anyLong(), any())).thenReturn(
            Optional.of(testSale));
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(testStorage);
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(testUser)).thenReturn(new CashRegister());

        saleService.cancelCashSale(id, null);

        assertTrue(testSale.isCanceled());
        InOrder cancellationOrder = org.mockito.Mockito.inOrder(ponctionService,
            cashSaleRepository);
        cancellationOrder.verify(ponctionService).annulerPourVente(1L, testDate,
            testSale.getPonctionId());
        cancellationOrder.verify(cashSaleRepository, times(2)).save(any());
        verify(cashSaleRepository, times(2)).save(any());
    }

    @Test
    void testCancelCashSale_ClonesExistingPayments() {
        SaleId id = testSale.getId();
        testSale.setStatut(SalesStatut.CLOSED);
        testSale.setPayments(new HashSet<>());
        SalePayment payment = new SalePayment();
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(testUser)).thenReturn(new CashRegister());
        when(cashSaleRepository.findOneWithEagerSalesLines(1L, testDate)).thenReturn(Optional.of(testSale));
        when(paymentService.findAllBySale(testSale)).thenReturn(List.of(payment));

        saleService.cancelCashSale(id, "erreur");

        verify(paymentService).clonePayment(eq(payment), argThat(copy -> copy != testSale && copy.isCanceled()));
    }

    @Test
    void testUpdateAmountsAfterRemovingItem() {
        testSale.setSalesAmount(1_000);
        testSale.setCostAmount(500);
        testSale.setHtAmount(1_000);
        testSalesLine.setSalesAmount(500);

        saleService.upddateCashSaleAmountsOnRemovingItem(testSale, testSalesLine);

        assertEquals(500, testSale.getSalesAmount());
        assertEquals(250, testSale.getCostAmount());
        assertEquals(500, testSale.getAmountToBePaid());
    }

    @Test
    void testSavePreventeUpdatesExistingSale() {
        testSale.setStatut(SalesStatut.PROCESSING);
        CashSaleDTO dto = new CashSaleDTO();
        dto.setSaleId(testSale.getId());
        when(cashSaleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));

        saleService.savePrevente(dto, true);

        assertEquals(SalesStatut.ACTIVE, testSale.getStatut());
        verify(cashSaleRepository).save(testSale);
    }

    @Test
    void testTransformProcessingSaleWithoutClone() {
        testSale.setStatut(SalesStatut.PROCESSING);
        testSale.setNatureVente(NatureVente.COMPTANT);
        when(cashSaleRepository.findOneWithEagerSalesLine(1L, testDate)).thenReturn(Optional.of(testSale));

        assertEquals(testSale.getId(), saleService.transformToVenteEncour(testSale.getId()));

        assertEquals(SalesStatut.ACTIVE, testSale.getStatut());
        verify(cashSaleRepository).save(testSale);
    }

    @Test
    void testTransformMissingSaleThrowsBusinessError() {
        when(cashSaleRepository.findOneWithEagerSalesLine(1L, testDate))
            .thenReturn(Optional.empty());

        assertThrows(GenericError.class,
            () -> saleService.transformToVenteEncour(testSale.getId()));
    }

    @Test
    void testTransformDevisClonesAndDeletesOriginal() {
        testSale.setStatut(SalesStatut.DEVIS);
        testSale.setNatureVente(NatureVente.COMPTANT);
        testSale.setPayments(new HashSet<>());
        when(cashSaleRepository.findOneWithEagerSalesLine(1L, testDate)).thenReturn(Optional.of(testSale));
        when(idGeneratorService.nextId()).thenReturn(20L);
        when(cashSaleRepository.saveAndFlush(any(CashSale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(salesLineService.cloneSalesLine(anySet(), any(CashSale.class))).thenReturn(Set.of());

        SaleId cloneId = saleService.transformToVenteEncour(testSale.getId());

        assertEquals(20L, cloneId.getId());
        verify(salesLineService).deleteSaleLine(testSalesLine);
        verify(salesRepository).delete(testSale);
        verify(cashSaleRepository).flush();
    }

    @Test
    void testCloneDevisValidAndInvalidStates() {
        testSale.setPayments(new HashSet<>());
        when(cashSaleRepository.findOneWithEagerSalesLines(1L, testDate)).thenReturn(Optional.of(testSale));
        testSale.setStatut(SalesStatut.ACTIVE);
        assertThrows(GenericError.class, () -> saleService.cloneDevis(testSale.getId()));

        testSale.setStatut(SalesStatut.DEVIS);
        when(idGeneratorService.nextId()).thenReturn(21L);
        when(cashSaleRepository.saveAndFlush(any(CashSale.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(salesLineService.cloneSalesLine(anySet(), any(CashSale.class))).thenReturn(Set.of());
        saleService.cloneDevis(testSale.getId());

        verify(cashSaleRepository).flush();
        verify(salesLineService).saveAll(Set.of());
    }

    @Test
    void testCancelCashSale_AlreadyCanceled_DoesNotCancelPonction() {
        SaleId id = new SaleId(1L, testDate);
        testSale.setStatut(SalesStatut.CLOSED);
        testSale.setCanceled(true);
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(testUser))
            .thenReturn(new CashRegister());
        when(cashSaleRepository.findOneWithEagerSalesLines(1L, testDate))
            .thenReturn(Optional.of(testSale));

        GenericError error = assertThrows(GenericError.class,
            () -> saleService.cancelCashSale(id, "motif"));

        assertEquals("La vente est déjà annulée", error.getMessage());
        verify(ponctionService, never()).annulerPourVente(anyLong(), any(), any());
        verify(cashSaleRepository, never()).save(any());
    }

    @Test
    void testCancelCashSale_NotClosed_DoesNotCancelPonction() {
        SaleId id = new SaleId(1L, testDate);
        testSale.setStatut(SalesStatut.ACTIVE);
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(testUser))
            .thenReturn(new CashRegister());
        when(cashSaleRepository.findOneWithEagerSalesLines(1L, testDate))
            .thenReturn(Optional.of(testSale));

        GenericError error = assertThrows(GenericError.class,
            () -> saleService.cancelCashSale(id, "motif"));

        assertEquals("La vente doit être clôturée pour être annulée", error.getMessage());
        verify(ponctionService, never()).annulerPourVente(anyLong(), any(), any());
        verify(cashSaleRepository, never()).save(any());
    }

    @Test
    void testAuthorizeAction() throws Exception {
        UtilisationCleSecuriteDTO dto = new UtilisationCleSecuriteDTO();

        saleService.authorizeAction(dto);

        verify(utilisationCleSecuriteService).authorizeAction(dto, ThirdPartySaleService.class);
    }

    @Test
    void testProcessDiscount() {
        UpdateSaleInfo info = new UpdateSaleInfo(new SaleId(1L, testDate), 1);
        RemiseClient remise = new RemiseClient();

        when(cashSaleRepository.findById(any())).thenReturn(Optional.of(testSale));
        when(remiseRepository.findById(1)).thenReturn(Optional.of(remise));

        saleService.processDiscount(info);

        verify(cashSaleRepository).save(testSale);
        assertInstanceOf(RemiseClient.class, testSale.getRemise());
    }

    @Test
    void testProcessDiscount_RemiseProduit() {
        UpdateSaleInfo info = new UpdateSaleInfo(new SaleId(1L, testDate), 2);
        RemiseProduit remiseProduit = new RemiseProduit();

        when(cashSaleRepository.findById(any())).thenReturn(Optional.of(testSale));
        when(remiseRepository.findById(2)).thenReturn(Optional.of(remiseProduit));

        saleService.processDiscount(info);

        verify(cashSaleRepository).save(testSale);
        assertInstanceOf(RemiseProduit.class, testSale.getRemise());
    }

    @Test
    void testProcessDiscount_ReplacesExistingDiscount() {
        UpdateSaleInfo info = new UpdateSaleInfo(testSale.getId(), 3);
        testSale.setRemise(new RemiseClient());
        RemiseClient replacement = new RemiseClient();
        when(cashSaleRepository.findById(testSale.getId())).thenReturn(Optional.of(testSale));
        when(remiseRepository.findById(3)).thenReturn(Optional.of(replacement));

        saleService.processDiscount(info);

        assertSame(replacement, testSale.getRemise());
        verify(salesLineService).saveSalesLine(testSalesLine);
    }


    @Test
    void testAddOrUpdateSaleLine_UpdateExisting() {
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleCompositeId(new SaleId(1L, testDate));
        dto.setProduitId(1);

        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);
        when(salesManager.addOrUpdateSaleLine(any(), any())).thenReturn(dto);

        SaleLineDTO result = saleService.addOrUpdateSaleLine(dto);

        assertNotNull(result);
        verify(salesManager).addOrUpdateSaleLine(eq(dto), eq(testSale));
    }

    @Test
    void testPutCashSaleOnHold_KeepIfNotEmpty() {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setSaleId(new SaleId(1L, testDate));
        testSale.setSalesLines(new HashSet<>(List.of(testSalesLine)));

        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);

        saleService.putCashSaleOnHold(dto);

        verify(salesRepository, never()).delete(testSale);
        verify(salesRepository).save(testSale);
    }

    @Test
    void testRemoveRemiseFromCashSale() {
        SaleId id = new SaleId(1L, testDate);
        when(cashSaleRepository.getReferenceById(any())).thenReturn(testSale);

        saleService.removeRemiseFromCashSale(id);

        verify(cashSaleRepository).save(testSale);
    }

    @Test
    void testFindBySalesIdAndSalesSaleDateOrderByProduitLibelle() {
        saleService.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(1L, testDate);
        verify(salesLineService).findBySalesIdAndSalesSaleDateOrderByProduitLibelle(1L, testDate);
    }
}

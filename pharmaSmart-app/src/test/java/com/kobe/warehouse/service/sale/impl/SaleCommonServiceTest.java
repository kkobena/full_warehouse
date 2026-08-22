package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.*;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.errors.*;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleCommonServiceTest {

    @Mock private ReferenceService referenceService;
    @Mock private StorageService storageService;
    @Mock private UserRepository userRepository;
    @Mock private SaleLineServiceFactory saleLineServiceFactory;
    @Mock private SalesLineService salesLineService;
    @Mock private CashRegisterService cashRegisterService;
    @Mock private PosteRepository posteRepository;
    @Mock private CustomerDisplayService displayService;
    @Mock private SaleIdGeneratorService idGeneratorService;
    @Mock private AppConfigurationService configurationService;

    private TestableSaleCommonService service;
    private AppUser user;
    private Storage storage;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1);
        user.setFirstName("Jean");
        user.setLastName("Test");
        user.setMagasin(new Magasin());
        storage = new Storage();
        storage.setId(4);
        service = new TestableSaleCommonService(referenceService, storageService, userRepository,
            saleLineServiceFactory, cashRegisterService, posteRepository, displayService,
            idGeneratorService, new ObjectMapper(), configurationService);
        lenient().when(storageService.getUser()).thenReturn(user);
        lenient().when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(storage);
        lenient().when(referenceService.buildNumSale()).thenReturn("001");
        lenient().when(referenceService.buildNumPreventeSale()).thenReturn("P001");
        lenient().when(saleLineServiceFactory.getService(any())).thenReturn(salesLineService);
    }

    @Test
    void rejectsCancellationAfterConfiguredDelay() {
        when(configurationService.getCancelSaleMaxDays()).thenReturn(5);
        assertThrows(GenericError.class,
            () -> service.checkDelay(LocalDate.now().minusDays(6)));
        service.checkDelay(LocalDate.now().minusDays(5));
    }

    @Test
    void computesTaxRemovalAndRoundingVariants() {
        CashSale sale = cashSale();
        SalesLine taxed = line(2, 600, 100, 20);
        sale.setSalesLines(new HashSet<>(Set.of(taxed)));
        service.computeSaleEagerAmount(sale);
        assertEquals(1_000, sale.getHtAmount());
        assertEquals(200, sale.getTaxAmount());
        assertNotNull(service.buildTvaData(sale.getSalesLines()));

        sale.setSalesAmount(2_000);
        sale.setCostAmount(500);
        sale.setHtAmount(1_500);
        sale.setTaxAmount(500);
        taxed.setSalesAmount(1_200);
        service.computeSaleEagerAmountOnRemovingItem(sale, taxed);
        service.computeSaleLazyAmountOnRemovingItem(sale, taxed);
        service.computeTvaAmountOnRemovingItem(sale, taxed);
        assertEquals(800, sale.getSalesAmount());
        assertEquals(300, sale.getCostAmount());
        assertEquals(500, sale.getHtAmount());
        assertEquals(300, sale.getTaxAmount());

        SalesLine untaxed = line(1, 400, 50, 0);
        untaxed.setSalesAmount(400);
        service.computeTvaAmountOnRemovingItem(sale, untaxed);
        assertEquals(100, sale.getHtAmount());
        assertEquals(10, service.roundedAmount(10));
        assertEquals(10, service.roundedAmount(12));
        assertEquals(15, service.roundedAmount(13));
    }

    @Test
    void initializesSaleWithDifferentSellerAndPoste() {
        AppUser seller = new AppUser();
        seller.setId(2);
        Poste poste = new Poste();
        SaleDTO dto = new SaleDTO();
        dto.setSellerId(2);
        dto.setNatureVente(NatureVente.COMPTANT);
        dto.setCaisseNum("POS");
        when(idGeneratorService.nextId()).thenReturn(9L);
        when(userRepository.getReferenceById(2)).thenReturn(seller);
        when(posteRepository.findFirstByAddressOrName("POS", "POS")).thenReturn(Optional.of(poste));
        CashSale sale = new CashSale();

        service.initialize(dto, sale);

        assertEquals(9L, sale.getId().getId());
        assertSame(seller, sale.getSeller());
        assertSame(poste, sale.getCaisse());
        assertSame(poste, sale.getLastCaisse());
        assertEquals(SalesStatut.ACTIVE, sale.getStatut());
    }

    @Test
    void validatesSaleStateAndCashRegister() {
        CashSale empty = cashSale();
        service.prevalideSale(empty);
        CashSale closed = cashSale();
        closed.setStatut(SalesStatut.CLOSED);
        closed.setSalesLines(Set.of(line(1, 100, 10, 0)));
        assertThrows(SaleAlreadyCloseException.class, () -> service.prevalideSale(closed));

        CashRegister opened = new CashRegister();
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(user))
            .thenReturn(null, opened, null);
        when(cashRegisterService.openCashRegister(user, user)).thenReturn(opened);
        assertSame(opened, service.cashRegister());
        service.checkRegister();
        assertThrows(CashRegisterException.class, service::checkRegister);
    }

    @Test
    void finalizationRejectsInvalidPaymentsAndDeferredAnonymousSale() {
        CashSale sale = cashSale();
        sale.setSalesLines(Set.of(line(1, 100, 10, 0)));
        CashRegister register = new CashRegister();
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(user)).thenReturn(register);
        SaleDTO insufficient = saleDto(false, 50, 100);
        assertThrows(PaymentAmountException.class, () -> service.save(sale, insufficient));

        CashSale deferred = cashSale();
        deferred.setSalesLines(Set.of(line(1, 100, 10, 0)));
        SaleDTO deferredDto = saleDto(true, 0, 100);
        assertThrows(SaleNotFoundCustomerException.class, () -> service.save(deferred, deferredDto));
    }

    @Test
    void updatesCashAmountsAndRemovalAmounts() {
        CashSale sale = cashSale();
        SalesLine line = line(2, 500, 100, 0);
        line.setSalesAmount(1_000);
        sale.setSalesLines(new HashSet<>(Set.of(line)));
        service.upddateCashSaleAmounts(sale);
        assertEquals(1_000, sale.getAmountToBePaid());

        sale.setSalesAmount(1_500);
        sale.setCostAmount(500);
        sale.setHtAmount(1_500);
        service.upddateCashSaleAmountsOnRemovingItem(sale, line);
        assertEquals(500, sale.getSalesAmount());
        assertEquals(300, sale.getCostAmount());
        assertEquals(500, sale.getAmountToBePaid());
    }

    @Test
    void coversDisplayTypeOriginAndCashFinalizationRules() {
        service.showChange(25);
        verify(displayService).displayMonnaie(25);

        Sales unknown = new Sales();
        SalesLine unknownLine = line(1, 100, 10, 0);
        unknown.setSalesLines(new HashSet<>(Set.of(unknownLine)));
        service.removeRemise(unknown);
        verify(saleLineServiceFactory).getService(null);

        Sales original = new Sales();
        Sales copy = new Sales();
        service.origin(original, copy);
        assertSame(original, copy.getCanceledSale());

        CashSale deferred = cashSale();
        CashSaleDTO dto = new CashSaleDTO();
        dto.setDiffere(true);
        dto.setPayrollAmount(100);
        dto.setRestToPay(0);
        assertThrows(SaleNotFoundCustomerException.class, () -> service.finishCash(deferred, dto));

        CashSale completed = cashSale();
        completed.setCustomer(new Customer());
        dto.setRestToPay(-2);
        service.finishCash(completed, dto);
        assertEquals(0, completed.getRestToPay());
        assertEquals(PaymentStatus.IMPAYE, completed.getPaymentStatus());
    }

    @Test
    void validatesPreventeAndTransformationStates() {
        CashSale invalid = cashSale();
        invalid.setStatut(SalesStatut.ACTIVE);
        assertThrows(GenericError.class,
            () -> service.prevalidatePrevente(invalid, SalesStatut.PROCESSING));

        CashSale devis = cashSale();
        devis.setStatut(SalesStatut.DEVIS);
        assertThrows(SaleNotFoundCustomerException.class,
            () -> service.prevalidatePrevente(devis, SalesStatut.ACTIVE));

        assertThrows(GenericError.class,
            () -> service.prevalidateTransform(SalesStatut.ACTIVE, NatureVente.COMPTANT));
        service.prevalidateTransform(SalesStatut.PROCESSING, NatureVente.CARNET);
    }

    @Test
    void initializesExplicitStatusAndCoversAllFinalPaymentStates() {
        SaleDTO initial = new SaleDTO();
        initial.setStatut(SalesStatut.PROCESSING);
        initial.setSellerId(user.getId());
        CashSale initialized = cashSale();
        service.initialize(initial, initialized);
        assertEquals(SalesStatut.PROCESSING, initialized.getStatut());

        CashRegister register = new CashRegister();
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(user)).thenReturn(register);

        CashSale paid = cashSale();
        paid.setSalesLines(Set.of(line(1, 100, 10, 0)));
        service.save(paid, saleDto(false, 100, 100));
        assertEquals(PaymentStatus.PAYE, paid.getPaymentStatus());

        CashSale unpaid = cashSale();
        unpaid.setCustomer(new Customer());
        unpaid.setSalesLines(Set.of(line(1, 100, 10, 0)));
        service.save(unpaid, saleDto(true, 90, 100));
        assertEquals(PaymentStatus.IMPAYE, unpaid.getPaymentStatus());
        assertEquals(10, unpaid.getRestToPay());
    }

    @Test
    void rejectsNullPaymentAmounts() {
        CashSale sale = cashSale();
        sale.setCustomer(new Customer());
        sale.setSalesLines(Set.of(line(1, 100, 10, 0)));
        SaleDTO dto = saleDto(true, 100, 100);
        dto.setPayrollAmount(null);
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(user))
            .thenReturn(new CashRegister());

        assertThrows(PaymentAmountException.class, () -> service.save(sale, dto));
    }

    @Test
    void appliesProductDiscountAndResolvesDepotLineService() {
        CashSale discounted = cashSale();
        discounted.setSalesLines(Set.of(line(1, 100, 10, 0)));
        discounted.setRemise(new RemiseProduit());
        service.proccessDiscount(discounted);
        verify(salesLineService).processProductDiscount(any(SalesLine.class));

        VenteDepot depot = new VenteDepot();
        depot.setSalesLines(Set.of(line(1, 100, 10, 0)));
        service.removeRemise(depot);
        verify(saleLineServiceFactory).getService(TypeVente.VenteDepot);
    }

    @Test
    void finalizesCashSaleAsPaidAndUnpaid() {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setPayrollAmount(100);
        dto.setRestToPay(0);
        CashSale paid = cashSale();
        service.finishCash(paid, dto);
        assertEquals(PaymentStatus.PAYE, paid.getPaymentStatus());

        dto.setRestToPay(10);
        CashSale unpaid = cashSale();
        service.finishCash(unpaid, dto);
        assertEquals(PaymentStatus.IMPAYE, unpaid.getPaymentStatus());
        assertEquals(10, unpaid.getRestToPay());
    }

    private CashSale cashSale() {
        CashSale sale = new CashSale();
        sale.setSalesLines(new HashSet<>());
        sale.setPayments(new HashSet<>());
        sale.setSalesAmount(0);
        sale.setCostAmount(0);
        sale.setHtAmount(0);
        sale.setTaxAmount(0);
        sale.setDiscountAmount(0);
        sale.setNetAmount(0);
        sale.setAmountToBePaid(0);
        sale.setAmountToBeTakenIntoAccount(0);
        sale.setPayrollAmount(0);
        sale.setRestToPay(0);
        sale.setStatut(SalesStatut.ACTIVE);
        return sale;
    }

    private SalesLine line(int quantity, int price, int cost, int tax) {
        SalesLine line = new SalesLine();
        line.setId((long) (quantity + price + tax));
        Produit produit = new Produit();
        produit.setCodeRemise(CodeRemise.NONE);
        line.setProduit(produit);
        line.setQuantityRequested(quantity);
        line.setRegularUnitPrice(price);
        line.setCostAmount(cost);
        line.setTaxValue(tax);
        line.setDiscountAmount(0);
        return line;
    }

    private SaleDTO saleDto(boolean deferred, int payroll, int amountToPay) {
        SaleDTO dto = new SaleDTO();
        dto.setDiffere(deferred);
        dto.setPayrollAmount(payroll);
        dto.setAmountToBePaid(amountToPay);
        dto.setMontantRendu(0);
        return dto;
    }

    private static final class TestableSaleCommonService extends SaleCommonService {
        private TestableSaleCommonService(ReferenceService referenceService, StorageService storageService,
            UserRepository userRepository, SaleLineServiceFactory factory,
            CashRegisterService cashRegisterService, PosteRepository posteRepository,
            CustomerDisplayService displayService, SaleIdGeneratorService idGenerator,
            ObjectMapper objectMapper, AppConfigurationService configurationService) {
            super(referenceService, storageService, userRepository, factory, cashRegisterService,
                posteRepository, displayService, idGenerator, objectMapper, configurationService);
        }

        void checkDelay(LocalDate date) { checkCancellationDelay(date); }
        void initialize(SaleDTO dto, Sales sale) { intSale(dto, sale); }
        CashRegister cashRegister() { return getCashRegister(); }
        void checkRegister() { checkOpenningCaisse(); }
        void showChange(Integer value) { displayMonnaie(value); }
        void origin(Sales original, Sales copy) { copyOrigin(original, copy); }
        void finishCash(CashSale sale, CashSaleDTO dto) { finalizeSale(sale, dto); }
        void prevalidatePrevente(Sales sale, SalesStatut status) { preValidatePrevente(sale, status); }
        void prevalidateTransform(SalesStatut status, NatureVente nature) { preValidateTrasnform(status, nature); }
    }
}


package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.service.PaymentService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SimplifiedSaleServiceImplTest {

    @Mock private PaymentService paymentService;
    @Mock private CashSaleRepository repository;
    @Mock private ReferenceService referenceService;
    @Mock private StorageService storageService;
    @Mock private UserRepository userRepository;
    @Mock private SaleLineServiceFactory lineServiceFactory;
    @Mock private CashRegisterService cashRegisterService;
    @Mock private PosteRepository posteRepository;
    @Mock private CustomerDisplayService customerDisplayService;
    @Mock private SaleIdGeneratorService idGeneratorService;
    @Mock private UninsuredCustomerRepository customerRepository;
    @Mock private AppConfigurationService appConfigurationService;
    @Mock private SalesLineService lineService;

    private SimplifiedSaleServiceImpl service;
    private AppUser user;
    private Storage storage;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1);
        user.setMagasin(new Magasin());
        storage = new Storage();
        storage.setId(9);
        lenient().when(lineServiceFactory.getService(TypeVente.CashSale)).thenReturn(lineService);
        lenient().when(storageService.getUser()).thenReturn(user);
        lenient().when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(storage);
        lenient().when(idGeneratorService.nextId()).thenReturn(42L);
        lenient().when(referenceService.buildNumPreventeSale()).thenReturn("PRE");
        lenient().when(referenceService.buildNumSale()).thenReturn("SALE");
        lenient().when(posteRepository.findFirstByAddressOrName(any(), any())).thenReturn(Optional.empty());

        service = new SimplifiedSaleServiceImpl(
            paymentService, repository, referenceService, storageService, userRepository,
            lineServiceFactory, cashRegisterService, posteRepository, customerDisplayService,
            idGeneratorService, customerRepository, new ObjectMapper(), appConfigurationService
        );
    }

    @Test
    void createsAndFinalizesSaleUsingExistingRegisterAndCustomer() {
        CashRegister register = new CashRegister();
        UninsuredCustomer customer = new UninsuredCustomer();
        customer.setId(5);
        CashSaleDTO dto = validDto(5);
        SalesLine line = validLine();
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(user)).thenReturn(register);
        when(customerRepository.getReferenceById(5)).thenReturn(customer);
        when(lineService.createSaleLinesFromDTO(any(CashSale.class), eq(dto.getSalesLines()), eq(9)))
            .thenReturn(List.of(line));
        when(repository.save(any(CashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinalyseSaleDTO result = service.createCashSale(dto);

        ArgumentCaptor<CashSale> saleCaptor = ArgumentCaptor.forClass(CashSale.class);
        verify(repository).save(saleCaptor.capture());
        CashSale persistedSale = saleCaptor.getValue();
        assertEquals(42L, result.saleId().getId());
        assertSame(customer, persistedSale.getCustomer());
        assertSame(register, persistedSale.getCashRegister());
        assertEquals(SalesStatut.CLOSED, persistedSale.getStatut());
        assertEquals(1_000, persistedSale.getSalesAmount());
        verify(cashRegisterService, never()).openCashRegister(any(AppUser.class), any(AppUser.class));
        verify(paymentService).buildPaymentFromFromPaymentDTO(persistedSale, dto);
        verify(lineService).saveAllSalesLines(persistedSale.getSalesLines(), user, 9);
    }

    @Test
    void opensRegisterAndAllowsAnonymousCustomer() {
        CashRegister opened = new CashRegister();
        CashSaleDTO dto = validDto(null);
        SalesLine line = validLine();
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(user)).thenReturn(null);
        when(cashRegisterService.openCashRegister(user, user)).thenReturn(opened);
        when(lineService.createSaleLinesFromDTO(any(CashSale.class), eq(dto.getSalesLines()), eq(9)))
            .thenReturn(List.of(line));
        when(repository.save(any(CashSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FinalyseSaleDTO result = service.createCashSale(dto);

        ArgumentCaptor<CashSale> saleCaptor = ArgumentCaptor.forClass(CashSale.class);
        verify(repository).save(saleCaptor.capture());
        assertEquals(42L, result.saleId().getId());
        assertNull(saleCaptor.getValue().getCustomer());
        assertSame(opened, saleCaptor.getValue().getCashRegister());
        verify(customerRepository, never()).getReferenceById(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void listsLatestCurrentUserSalesWithoutSearchFilter() {
        Specification<CashSale> between = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> status = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> afterStatus = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> cashier = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> finalSpec = org.mockito.Mockito.mock(Specification.class);
        when(repository.between(any(LocalDate.class), any(LocalDate.class))).thenReturn(between);
        when(repository.hasStatut(EnumSet.of(SalesStatut.CLOSED))).thenReturn(status);
        when(between.and(status)).thenReturn(afterStatus);
        when(repository.hasCaissier(user)).thenReturn(cashier);
        when(afterStatus.and(cashier)).thenReturn(finalSpec);
        when(repository.findAll(eq(finalSpec), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        Slice<CashSaleDTO> result = service.getList("  ");

        assertEquals(0, result.getNumberOfElements());
        verify(repository, never()).filterNumberTransaction(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void addsNumberFilterWhenSearchIsProvided() {
        Specification<CashSale> between = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> status = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> afterStatus = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> number = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> afterNumber = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> cashier = org.mockito.Mockito.mock(Specification.class);
        Specification<CashSale> finalSpec = org.mockito.Mockito.mock(Specification.class);
        when(repository.between(any(LocalDate.class), any(LocalDate.class))).thenReturn(between);
        when(repository.hasStatut(EnumSet.of(SalesStatut.CLOSED))).thenReturn(status);
        when(between.and(status)).thenReturn(afterStatus);
        when(repository.filterNumberTransaction("V-42")).thenReturn(number);
        when(afterStatus.and(number)).thenReturn(afterNumber);
        when(repository.hasCaissier(user)).thenReturn(cashier);
        when(afterNumber.and(cashier)).thenReturn(finalSpec);
        when(repository.findAll(eq(finalSpec), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertEquals(0, service.getList("V-42").getNumberOfElements());
        verify(repository).filterNumberTransaction("V-42");
    }

    private CashSaleDTO validDto(Integer customerId) {
        CashSaleDTO dto = new CashSaleDTO();
        dto.setCustomerId(customerId);
        dto.setPayrollAmount(1_000);
        dto.setAmountToBePaid(1_000);
        dto.setRestToPay(0);
        dto.setMontantRendu(0);
        dto.setSalesLines(List.of(new SaleLineDTO()));
        return dto;
    }

    private SalesLine validLine() {
        SalesLine line = new SalesLine();
        line.setId(1L);
        line.setProduit(new Produit());
        line.setQuantityRequested(2);
        line.setQuantitySold(2);
        line.setRegularUnitPrice(500);
        line.setNetUnitPrice(500);
        line.setCostAmount(200);
        line.setTaxValue(0);
        line.setDiscountAmount(0);
        return line;
    }
}



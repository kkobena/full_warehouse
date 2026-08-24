package com.kobe.warehouse.service.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.ReceiptPrinterService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.report.SaleInvoiceReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class SaleDataServiceNativeQueryTest {

    @Mock private EntityManager entityManager;
    @Mock private SaleInvoiceReportService invoiceService;
    @Mock private SalesLineRepository lineRepository;
    @Mock private ThirdPartySaleLineRepository thirdPartyLineRepository;
    @Mock private ReceiptPrinterService printerService;
    @Mock private SalesRepository salesRepository;
    @Mock private StorageService storageService;
    @Mock private Query countQuery;
    @Mock private Query dataQuery;

    private SaleDataService service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        Magasin magasin = new Magasin();
        magasin.setId(12);
        user.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(user);
        service = new SaleDataService(entityManager, invoiceService, lineRepository,
            thirdPartyLineRepository, printerService, salesRepository, storageService);
    }

    @Test
    void listsAndMapsAllSaleKindsWithCompleteFilters() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery, dataQuery);
        when(countQuery.getSingleResult()).thenReturn(5L);
        when(dataQuery.getResultList()).thenReturn(List.of(
            row("CashSale", 1L, "COMPTANT", 7, true),
            row("ThirdPartySales", 2L, "ASSURANCE", 8, true),
            row("VenteDepot", 3L, "CARNET", null, false)
        ));
        PageRequest pageable = PageRequest.of(1, 2);

        Page<SaleDTO> result = service.listVenteTerminees(
            "para", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 21),
            "08:00", "18:30", true, 5,
            Set.of(EntityConstant.TOUT, "ASSURANCE", "COMPTANT"),
            EnumSet.of(SalesStatut.CLOSED), PaymentStatus.PAYE, true,
            Set.of(CategorieChiffreAffaire.CA), pageable
        );

        assertEquals(5, result.getTotalElements());
        assertInstanceOf(CashSaleDTO.class, result.getContent().get(0));
        assertInstanceOf(ThirdPartySaleDTO.class, result.getContent().get(1));
        assertInstanceOf(DepotExtensionSaleDTO.class, result.getContent().get(2));
        assertEquals("VNO", result.getContent().get(0).getCategorie());
        assertEquals("VO", result.getContent().get(1).getCategorie());
        assertEquals("DEPOT", result.getContent().get(2).getCategorie());
        assertEquals(2, result.getContent().get(0).getItemCount());
        assertEquals(0, result.getContent().get(2).getItemCount());
        verify(dataQuery).setFirstResult(2);
        verify(dataQuery).setMaxResults(2);
        verify(countQuery).setParameter("search", "PARA%");
        verify(countQuery).setParameter("fromHour", "08:00:00");
        verify(countQuery).setParameter("toHour", "18:30:59");
        verify(countQuery).setParameter("paymentStatus", "PAYE");
        verify(countQuery).setParameter("isDiffere", true);
    }

    @Test
    void returnsEmptyPageWithDefaultsAndNoDataQuery() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(0L);
        PageRequest pageable = PageRequest.of(0, 5);

        Page<SaleDTO> result = service.listVenteTerminees(
            "V-", null, null, "00:00", "23:59", false, null,
            Set.of(), Set.of(), PaymentStatus.ALL, null, Set.of(), pageable
        );

        assertTrue(result.isEmpty());
        verify(countQuery).setParameter("refSearch", "V-%");
        verify(countQuery).setParameter("magasinId", 12L);
        verify(dataQuery, never()).getResultList();
    }

    @Test
    void totalsSalesWithDefaultStatusesAndSingleTypeFilter() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(12_345L);

        long total = service.totalVenteTerminees(
            null, null, null, null, null, false, null,
            Set.of("COMPTANT"), Set.of(), null
        );

        assertEquals(12_345L, total);
        verify(countQuery).setParameter("magasinId", 12L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void delegatesCustomerPurchasesPendingCountAndCredits() {
        Specification<com.kobe.warehouse.domain.Sales> customer = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> closed = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> afterClosed = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> dates = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> finalCustomer = org.mockito.Mockito.mock(Specification.class);
        when(salesRepository.filterByCustomerId(7)).thenReturn(customer);
        when(salesRepository.hasStatut(EnumSet.of(SalesStatut.CLOSED))).thenReturn(closed);
        when(customer.and(closed)).thenReturn(afterClosed);
        when(salesRepository.between(any(), any())).thenReturn(dates);
        when(afterClosed.and(dates)).thenReturn(finalCustomer);
        when(salesRepository.findAll(finalCustomer)).thenReturn(List.of());

        assertTrue(service.customerPurchases(7, LocalDate.now().minusDays(2), LocalDate.now()).isEmpty());

        Specification<com.kobe.warehouse.domain.Sales> active = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> today = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> noDepot = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> first = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> base = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> cashier = org.mockito.Mockito.mock(Specification.class);
        Specification<com.kobe.warehouse.domain.Sales> withCashier = org.mockito.Mockito.mock(Specification.class);
        when(salesRepository.isActif()).thenReturn(active);
        when(salesRepository.toDay()).thenReturn(today);
        when(active.and(today)).thenReturn(first);
        when(salesRepository.notDepot()).thenReturn(noDepot);
        when(first.and(noDepot)).thenReturn(base);
        when(salesRepository.hasCaissier(user)).thenReturn(cashier);
        when(base.and(cashier)).thenReturn(withCashier);
        when(salesRepository.count(withCashier)).thenReturn(4L);
        when(lineRepository.findAllByQuantityAvoirGreaterThan(0)).thenReturn(List.of());

        assertEquals(4L, service.countPendingSales(3));
        assertTrue(service.getAllAvoirs().isEmpty());
    }

    private Object[] row(String dtype, long id, String nature, Integer customerId, boolean fullUsers) {
        LocalDate date = LocalDate.of(2026, 8, 21);
        LocalDateTime dateTime = LocalDateTime.of(2026, 8, 21, 10, 30);
        return new Object[]{
            dtype, id, Date.valueOf(date), "V-" + id,
            100, 1_000, 900, 0,
            "CLOSED", "PAYE", nature, "CONSEIL",
            true, false, Timestamp.valueOf(dateTime), dateTime,
            "comment", 100, null,
            "BON", 300, 600, "User Full",
            fullUsers ? 4 : null, fullUsers ? "Seller" : null, fullUsers ? "One" : null,
            fullUsers ? 5 : null, fullUsers ? "Cashier" : null, fullUsers ? "Two" : null,
            "POS-1", "POS-2", "Client", "Name", "0102", customerId,
            customerId == null ? null : 2
        };
    }
}


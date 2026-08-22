package com.kobe.warehouse.service.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.ReceiptPrinterService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.report.SaleInvoiceReportService;
import com.kobe.warehouse.service.stock.dto.StockDepotExportDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class SaleDataServiceCriteriaTest {

    @Mock private EntityManager entityManager;
    @Mock private SaleInvoiceReportService invoiceService;
    @Mock private SalesLineRepository lineRepository;
    @Mock private ThirdPartySaleLineRepository thirdPartyLineRepository;
    @Mock private ReceiptPrinterService printerService;
    @Mock private SalesRepository salesRepository;
    @Mock private StorageService storageService;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS) private CriteriaBuilder cb;

    private SaleDataService service;

    @BeforeEach
    void setUp() {
        AppUser user = new AppUser();
        Magasin magasin = new Magasin();
        magasin.setId(3);
        user.setMagasin(magasin);
        lenient().when(storageService.getUser()).thenReturn(user);
        when(entityManager.getCriteriaBuilder()).thenReturn(cb);
        service = new SaleDataService(entityManager, invoiceService, lineRepository,
            thirdPartyLineRepository, printerService, salesRepository, storageService);
    }

    @Test
    void queriesAllCashAndThirdPartyPreventesThroughDispatcher() {
        CriteriaQuery<Sales> cq = org.mockito.Mockito.mock(CriteriaQuery.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Root<Sales> root = org.mockito.Mockito.mock(Root.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Root<CashSale> cashRoot = org.mockito.Mockito.mock(Root.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        Root<ThirdPartySales> thirdRoot = org.mockito.Mockito.mock(Root.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        TypedQuery<Sales> query = org.mockito.Mockito.mock(TypedQuery.class);
        Order order = org.mockito.Mockito.mock(Order.class);
        when(cb.createQuery(Sales.class)).thenReturn(cq);
        when(cq.from(Sales.class)).thenReturn(root);
        when(cq.select(any())).thenReturn(cq);
        when(cq.distinct(true)).thenReturn(cq);
        when(cq.orderBy(any(Order.class))).thenReturn(cq);
        when(cb.desc(any())).thenReturn(order);
        when(cb.treat(root, CashSale.class)).thenReturn(cashRoot);
        when(cb.treat(root, ThirdPartySales.class)).thenReturn(thirdRoot);
        when(entityManager.createQuery(cq)).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        Set<SalesStatut> statuses = Set.of(SalesStatut.ACTIVE);

        assertTrue(service.allPrevente("para", EntityConstant.TOUT, 1, statuses, from, to, true).isEmpty());
        assertTrue(service.allPrevente("", EntityConstant.VNO, null, statuses, from, to, false).isEmpty());
        assertTrue(service.allPrevente(null, "VO", null, statuses, from, to, true).isEmpty());

        verify(cb).treat(root, CashSale.class);
        verify(cb).treat(root, ThirdPartySales.class);
    }

    @Test
    void returnsEmptyDepotPageWhenCountIsZero() {
        DepotCriteria mocks = depotCriteria(0L);
        PageRequest pageable = PageRequest.of(0, 10);

        Page<DepotExtensionSaleDTO> result = service.fetchVenteDepot(
            null, null, null, null, PaymentStatus.ALL, null, pageable);

        assertTrue(result.isEmpty());
        verify(mocks.countQuery).getSingleResult();
    }

    @Test
    void fetchesPagedDepotSalesWithAllPredicates() {
        DepotCriteria mocks = depotCriteria(2L);
        CriteriaQuery<VenteDepot> dataCriteria = org.mockito.Mockito.mock(CriteriaQuery.class,
            org.mockito.Answers.RETURNS_DEEP_STUBS);
        Root<VenteDepot> dataRoot = org.mockito.Mockito.mock(Root.class,
            org.mockito.Answers.RETURNS_DEEP_STUBS);
        TypedQuery<VenteDepot> dataQuery = org.mockito.Mockito.mock(TypedQuery.class);
        when(cb.createQuery(VenteDepot.class)).thenReturn(dataCriteria);
        when(dataCriteria.from(VenteDepot.class)).thenReturn(dataRoot);
        when(dataCriteria.select(dataRoot)).thenReturn(dataCriteria);
        when(dataCriteria.distinct(true)).thenReturn(dataCriteria);
        when(dataCriteria.orderBy(any(Order.class))).thenReturn(dataCriteria);
        when(entityManager.createQuery(dataCriteria)).thenReturn(dataQuery);
        when(dataQuery.getResultList()).thenReturn(List.of());
        PageRequest pageable = PageRequest.of(1, 5);

        Page<DepotExtensionSaleDTO> result = service.fetchVenteDepot(
            "produit", LocalDate.now().minusDays(2), LocalDate.now(), 4,
            PaymentStatus.IMPAYE, 9, pageable);

        assertEquals(2, result.getTotalElements());
        verify(dataQuery).setFirstResult(5);
        verify(dataQuery).setMaxResults(5);
        verify(mocks.countQuery).getSingleResult();
    }

    @Test
    void exportsDepotStockProjection() {
        CriteriaQuery<StockDepotExportDTO> cq = org.mockito.Mockito.mock(CriteriaQuery.class,
            org.mockito.Answers.RETURNS_DEEP_STUBS);
        Root<SalesLine> root = org.mockito.Mockito.mock(Root.class, org.mockito.Answers.RETURNS_DEEP_STUBS);
        TypedQuery<StockDepotExportDTO> query = org.mockito.Mockito.mock(TypedQuery.class);
        when(cb.createQuery(StockDepotExportDTO.class)).thenReturn(cq);
        when(cq.from(SalesLine.class)).thenReturn(root);
        when(cq.select(any())).thenReturn(cq);
        when(cq.orderBy(any(Order.class))).thenReturn(cq);
        when(entityManager.createQuery(cq)).thenReturn(query);
        StockDepotExportDTO dto = org.mockito.Mockito.mock(StockDepotExportDTO.class);
        when(query.getResultList()).thenReturn(List.of(dto));

        List<StockDepotExportDTO> result = service.exportVenteDepotStock(
            new SaleId(8L, LocalDate.of(2026, 8, 21)));

        assertEquals(List.of(dto), result);
    }

    private DepotCriteria depotCriteria(long count) {
        CriteriaQuery<Long> countCriteria = org.mockito.Mockito.mock(CriteriaQuery.class,
            org.mockito.Answers.RETURNS_DEEP_STUBS);
        Root<VenteDepot> countRoot = org.mockito.Mockito.mock(Root.class,
            org.mockito.Answers.RETURNS_DEEP_STUBS);
        TypedQuery<Long> countQuery = org.mockito.Mockito.mock(TypedQuery.class);
        Expression<Long> countExpression = org.mockito.Mockito.mock(Expression.class);
        when(cb.createQuery(Long.class)).thenReturn(countCriteria);
        when(countCriteria.from(VenteDepot.class)).thenReturn(countRoot);
        when(cb.countDistinct(countRoot)).thenReturn(countExpression);
        when(countCriteria.select(countExpression)).thenReturn(countCriteria);
        when(entityManager.createQuery(countCriteria)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(count);
        return new DepotCriteria(countQuery);
    }

    private record DepotCriteria(TypedQuery<Long> countQuery) {
    }
}


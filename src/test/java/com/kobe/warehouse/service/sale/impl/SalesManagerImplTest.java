package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.repository.CashSaleRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.repository.VenteDepotRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.PlafondVenteException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SalesManagerImplTest {

    @Mock private SaleLineServiceFactory saleLineServiceFactory;
    @Mock private SalesLineService salesLineService;
    @Mock private StorageService storageService;
    @Mock private CustomerDisplayService customerDisplayService;
    @Mock private CashSaleRepository cashSaleRepository;
    @Mock private VenteDepotRepository venteDepotRepository;
    @Mock private SaleService saleService;
    @Mock private ThirdPartySaleService thirdPartySaleService;
    @Mock private SaleCommonService saleCommonService;

    private SalesManagerImpl salesManager;
    private CashSale testCashSale;
    private ThirdPartySales testThirdPartySale;
    private VenteDepot testVenteDepot;
    private SalesLine testSalesLine;
    private Storage testStorage;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now();
        testStorage = new Storage();
        testStorage.setId(1);

        lenient().when(saleLineServiceFactory.getService(null)).thenReturn(salesLineService);
        lenient().when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(testStorage);

        salesManager = new SalesManagerImpl(
            saleLineServiceFactory,
            storageService,
            customerDisplayService,
            cashSaleRepository,
            venteDepotRepository,
            saleService,
            thirdPartySaleService,
            saleCommonService
        );

        setupTestData();
    }

    private void setupTestData() {
        AppUser user = new AppUser();
        user.setId(1);
        user.setLogin("testUser");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActivated(true);
        user.setAuthorities(new HashSet<>());

        // Setup CashSale
        testCashSale = new CashSale();
        testCashSale.setId(1L);
        testCashSale.setSaleDate(testDate);
        testCashSale.setSalesAmount(1000);
        testCashSale.setNetAmount(1000);
        testCashSale.setCostAmount(500);
        testCashSale.setStatut(SalesStatut.ACTIVE);
        testCashSale.setSalesLines(new HashSet<>());
        testCashSale.setRestToPay(1000);
        testCashSale.setAmountToBePaid(1000);
        testCashSale.setUser(user);

        // Setup ThirdPartySales
        testThirdPartySale = new ThirdPartySales();
        testThirdPartySale.setId(2L);
        testThirdPartySale.setSaleDate(testDate);
        testThirdPartySale.setSalesAmount(2000);
        testThirdPartySale.setNetAmount(2000);
        testThirdPartySale.setStatut(SalesStatut.ACTIVE);
        testThirdPartySale.setSalesLines(new HashSet<>());
        testThirdPartySale.setPartAssure(1500);
        testThirdPartySale.setUser(user);
        testThirdPartySale.setCaissier(user);
        testThirdPartySale.setSeller(user);

        // Setup VenteDepot
        testVenteDepot = new VenteDepot();
        testVenteDepot.setId(3L);
        testVenteDepot.setSaleDate(testDate);
        testVenteDepot.setSalesAmount(1500);
        testVenteDepot.setNetAmount(1500);
        testVenteDepot.setStatut(SalesStatut.ACTIVE);
        testVenteDepot.setSalesLines(new HashSet<>());
        testVenteDepot.setRestToPay(1500);
        testVenteDepot.setAmountToBePaid(1500);
        testVenteDepot.setUser(user);
        Magasin depot = new Magasin();
        depot.setId(1);
        testVenteDepot.setDepot(depot);

        // Setup SalesLine
        testSalesLine = new SalesLine();
        testSalesLine.setId(1L);
        testSalesLine.setSalesAmount(500);
        testSalesLine.setQuantitySold(1);
        testSalesLine.setQuantityRequested(1);
        testSalesLine.setQuantityUg(0);
        testSalesLine.setRegularUnitPrice(500);
        testSalesLine.setCostAmount(250);
        testSalesLine.setTaxValue(0);

        Produit produit = new Produit();
        produit.setCodeRemise(CodeRemise.NONE);
        produit.setLibelle("Test Product");
        testSalesLine.setProduit(produit);
    }

    @Test
    void testUpdateItemQuantityRequested_CashSale() throws StockException, DeconditionnementStockOut {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setSaleCompositeId(new SaleId(1L, testDate));
        dto.setQuantityRequested(2);

        testSalesLine.setSales(testCashSale);
        testCashSale.getSalesLines().add(testSalesLine);

        when(salesLineService.getOneById(any())).thenReturn(testSalesLine);
        doNothing().when(salesLineService).updateItemQuantityRequested(any(), any(), anyInt());
        doNothing().when(saleService).upddateCashSaleAmounts(any());

        // When
        SaleLineDTO result = salesManager.updateItemQuantityRequested(dto, testCashSale,true);

        // Then
        assertNotNull(result);
        verify(salesLineService).updateItemQuantityRequested(eq(dto), eq(testSalesLine), eq(1));
        verify(saleService).upddateCashSaleAmounts(testCashSale);
        verify(cashSaleRepository).saveAndFlush(testCashSale);
        verify(customerDisplayService).displaySaleTotal(testCashSale.getNetAmount());
    }

    @Test
    void testUpdateItemQuantityRequested_ThirdPartySale() throws StockException, DeconditionnementStockOut {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setSaleCompositeId(new SaleId(2L, testDate));
        dto.setQuantityRequested(2);

        testSalesLine.setSales(testThirdPartySale);
        testThirdPartySale.getSalesLines().add(testSalesLine);

        when(salesLineService.getOneById(any())).thenReturn(testSalesLine);
        doNothing().when(salesLineService).updateItemQuantityRequested(any(), any(), anyInt());
        when(thirdPartySaleService.computeThirdPartySaleAmounts(any())).thenReturn(null);

        // When
        SaleLineDTO result = salesManager.updateItemQuantityRequested(dto, testThirdPartySale,true);

        // Then
        assertNotNull(result);
        verify(salesLineService).updateItemQuantityRequested(eq(dto), eq(testSalesLine), eq(1));
        verify(thirdPartySaleService).computeThirdPartySaleAmounts(testThirdPartySale);
        verify(customerDisplayService).displaySaleTotal(testThirdPartySale.getPartAssure());
    }

    @Test
    void testUpdateItemQuantityRequested_VenteDepot() throws StockException, DeconditionnementStockOut {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setSaleCompositeId(new SaleId(3L, testDate));
        dto.setQuantityRequested(2);

        testSalesLine.setSales(testVenteDepot);
        testVenteDepot.getSalesLines().add(testSalesLine);

        when(salesLineService.getOneById(any())).thenReturn(testSalesLine);
        doNothing().when(salesLineService).updateItemQuantityRequested(any(), any(), anyInt());
        doNothing().when(saleCommonService).computeSaleEagerAmount(any());
        doNothing().when(saleCommonService).proccessDiscount(any());
        doNothing().when(saleCommonService).arrondirMontantCaisse(any());

        // When
        SaleLineDTO result = salesManager.updateItemQuantityRequested(dto, testVenteDepot,true);

        // Then
        assertNotNull(result);
        verify(salesLineService).updateItemQuantityRequested(eq(dto), eq(testSalesLine), eq(1));
        verify(saleCommonService).computeSaleEagerAmount(testVenteDepot);
        verify(saleCommonService).proccessDiscount(testVenteDepot);
        verify(saleCommonService).arrondirMontantCaisse(testVenteDepot);
        verify(venteDepotRepository).saveAndFlush(testVenteDepot);
        verify(customerDisplayService).displaySaleTotal(testVenteDepot.getNetAmount());
    }

    @Test
    void testUpdateItemQuantitySold_CashSale() {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setQuantitySold(2);

        testSalesLine.setSales(testCashSale);

        when(salesLineService.getOneById(any())).thenReturn(testSalesLine);
        doNothing().when(salesLineService).updateItemQuantitySold(any(), any(), anyInt());
        doNothing().when(saleService).upddateCashSaleAmounts(any());

        // When
        SaleLineDTO result = salesManager.updateItemQuantitySold(dto, testCashSale);

        // Then
        assertNotNull(result);
        verify(salesLineService).updateItemQuantitySold(testSalesLine, dto, 1);
        verify(saleService).upddateCashSaleAmounts(testCashSale);
        verify(cashSaleRepository).saveAndFlush(testCashSale);
    }

    @Test
    void testUpdateItemRegularPrice_VenteDepot() {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setRegularUnitPrice(600);

        testSalesLine.setSales(testVenteDepot);

        when(salesLineService.getOneById(any())).thenReturn(testSalesLine);
        doNothing().when(salesLineService).updateItemRegularPrice(any(), any(), anyInt());
        doNothing().when(saleCommonService).computeSaleEagerAmount(any());
        doNothing().when(saleCommonService).proccessDiscount(any());
        doNothing().when(saleCommonService).arrondirMontantCaisse(any());

        // When
        SaleLineDTO result = salesManager.updateItemRegularPrice(dto, testVenteDepot);

        // Then
        assertNotNull(result);
        verify(salesLineService).updateItemRegularPrice(dto, testSalesLine, 1);
        verify(venteDepotRepository).saveAndFlush(testVenteDepot);
        verify(customerDisplayService).displaySaleTotal(testVenteDepot.getNetAmount());
    }

    @Test
    void testAddOrUpdateSaleLine_NewLine() {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleCompositeId(new SaleId(1L, testDate));
        dto.setProduitId(1);

        SalesLine newSalesLine = new SalesLine();
        newSalesLine.setId(2L);
        newSalesLine.setSalesAmount(300);
        Produit produit = new Produit();
        produit.setCodeRemise(CodeRemise.NONE);
        produit.setLibelle("New Product");
        newSalesLine.setProduit(produit);

        when(salesLineService.findBySalesIdAndProduitId(any(), anyInt())).thenReturn(Optional.empty());
        when(salesLineService.create(any(), anyInt(), any())).thenReturn(newSalesLine);
        doNothing().when(saleService).upddateCashSaleAmounts(any());

        // When
        SaleLineDTO result = salesManager.addOrUpdateSaleLine(dto, testCashSale);

        // Then
        assertNotNull(result);
        assertTrue(testCashSale.getSalesLines().contains(newSalesLine));
        verify(salesLineService).create(dto, 1, testCashSale);
        verify(cashSaleRepository).saveAndFlush(testCashSale);
    }

    @Test
    void testAddOrUpdateSaleLine_UpdateExisting() {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleCompositeId(new SaleId(1L, testDate));
        dto.setProduitId(1);

        testSalesLine.setSales(testCashSale);
        testCashSale.getSalesLines().add(testSalesLine);

        when(salesLineService.findBySalesIdAndProduitId(any(), anyInt())).thenReturn(Optional.of(testSalesLine));
        doNothing().when(salesLineService).updateSaleLine(any(), any(), anyInt());
        doNothing().when(saleService).upddateCashSaleAmounts(any());

        // When
        SaleLineDTO result = salesManager.addOrUpdateSaleLine(dto, testCashSale);

        // Then
        assertNotNull(result);
        verify(salesLineService).updateSaleLine(dto, testSalesLine, 1);
        verify(cashSaleRepository).saveAndFlush(testCashSale);
    }

    @Test
    void testDeleteSaleLineById_CashSale() {
        // Given
        testSalesLine.setSales(testCashSale);
        testCashSale.getSalesLines().add(testSalesLine);
        testCashSale.setUpdatedAt(LocalDateTime.now().minusHours(1));

        doNothing().when(salesLineService).deleteSaleLine(any());
        doNothing().when(saleService).upddateCashSaleAmountsOnRemovingItem(any(), any());

        // When
        salesManager.deleteSaleLineById(testSalesLine);

        // Then
        assertFalse(testCashSale.getSalesLines().contains(testSalesLine));
        assertNotNull(testCashSale.getUpdatedAt());
        verify(salesLineService).deleteSaleLine(testSalesLine);
        verify(saleService).upddateCashSaleAmountsOnRemovingItem(testCashSale, testSalesLine);
        verify(cashSaleRepository).save(testCashSale);
        verify(customerDisplayService).displaySaleTotal(testCashSale.getNetAmount());
    }

    @Test
    void testDeleteSaleLineById_ThirdPartySale() {
        // Given
        testSalesLine.setSales(testThirdPartySale);
        testThirdPartySale.getSalesLines().add(testSalesLine);

        doNothing().when(salesLineService).deleteSaleLine(any());
        doNothing().when(thirdPartySaleService).upddateSaleAmountsOnRemovingItem(any());

        // When
        salesManager.deleteSaleLineById(testSalesLine);

        // Then
        assertFalse(testThirdPartySale.getSalesLines().contains(testSalesLine));
        verify(salesLineService).deleteSaleLine(testSalesLine);
        verify(thirdPartySaleService).upddateSaleAmountsOnRemovingItem(testThirdPartySale);
        verify(customerDisplayService).displaySaleTotal(testThirdPartySale.getPartAssure());
    }

    @Test
    void testDeleteSaleLineById_VenteDepot() {
        // Given
        testSalesLine.setSales(testVenteDepot);
        testVenteDepot.getSalesLines().add(testSalesLine);

        doNothing().when(salesLineService).deleteSaleLine(any());
        doNothing().when(saleCommonService).computeSaleEagerAmountOnRemovingItem(any(), any());
        doNothing().when(saleCommonService).proccessDiscount(any());
        doNothing().when(saleCommonService).computeSaleLazyAmountOnRemovingItem(any(), any());
        doNothing().when(saleCommonService).computeTvaAmountOnRemovingItem(any(), any());

        // When
        salesManager.deleteSaleLineById(testSalesLine);

        // Then
        assertFalse(testVenteDepot.getSalesLines().contains(testSalesLine));
        verify(salesLineService).deleteSaleLine(testSalesLine);
        verify(saleCommonService).computeSaleEagerAmountOnRemovingItem(testVenteDepot, testSalesLine);
        verify(saleCommonService).proccessDiscount(testVenteDepot);
        verify(saleCommonService).computeSaleLazyAmountOnRemovingItem(testVenteDepot, testSalesLine);
        verify(saleCommonService).computeTvaAmountOnRemovingItem(testVenteDepot, testSalesLine);
        verify(venteDepotRepository).save(testVenteDepot);
        verify(customerDisplayService).displaySaleTotal(testVenteDepot.getNetAmount());
    }

    @Test
    void testUpdateItemQuantityRequested_ThrowsPlafondVenteException() {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setSaleCompositeId(new SaleId(2L, testDate));

        testSalesLine.setSales(testThirdPartySale);

        // Setup complete ThirdPartySale with all required fields for DTO creation
        testThirdPartySale.setSalesLines(new HashSet<>());
        testThirdPartySale.setPayments(new HashSet<>());

        when(salesLineService.getOneById(any())).thenReturn(testSalesLine);
        doNothing().when(salesLineService).updateItemQuantityRequested(any(), any(), anyInt());
        when(thirdPartySaleService.computeThirdPartySaleAmounts(any())).thenReturn("Plafond dépassé");

        // When & Then
        assertThrows(PlafondVenteException.class, () ->
            salesManager.updateItemQuantityRequested(dto, testThirdPartySale,true)
        );
    }

    @Test
    void testAddOrUpdateSaleLine_VenteDepot_NewLine() {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleCompositeId(new SaleId(3L, testDate));
        dto.setProduitId(2);

        SalesLine newSalesLine = new SalesLine();
        newSalesLine.setId(3L);
        newSalesLine.setSalesAmount(400);
        Produit produit = new Produit();
        produit.setCodeRemise(CodeRemise.NONE);
        produit.setLibelle("Depot Product");
        newSalesLine.setProduit(produit);

        when(salesLineService.findBySalesIdAndProduitId(any(), anyInt())).thenReturn(Optional.empty());
        when(salesLineService.create(any(), anyInt(), any())).thenReturn(newSalesLine);
        doNothing().when(saleCommonService).computeSaleEagerAmount(any());
        doNothing().when(saleCommonService).proccessDiscount(any());
        doNothing().when(saleCommonService).arrondirMontantCaisse(any());

        // When
        SaleLineDTO result = salesManager.addOrUpdateSaleLine(dto, testVenteDepot);

        // Then
        assertNotNull(result);
        assertTrue(testVenteDepot.getSalesLines().contains(newSalesLine));
        verify(salesLineService).create(dto, 1, testVenteDepot);
        verify(saleCommonService).computeSaleEagerAmount(testVenteDepot);
        verify(venteDepotRepository).saveAndFlush(testVenteDepot);
        verify(customerDisplayService).displaySaleTotal(testVenteDepot.getNetAmount());
    }

    @Test
    void testUpdateItemQuantitySold_VenteDepot() {
        // Given
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleLineId(new SaleLineId(1L, testDate));
        dto.setQuantitySold(3);

        testSalesLine.setSales(testVenteDepot);

        when(salesLineService.getOneById(any())).thenReturn(testSalesLine);
        doNothing().when(salesLineService).updateItemQuantitySold(any(), any(), anyInt());
        doNothing().when(saleCommonService).computeSaleEagerAmount(any());
        doNothing().when(saleCommonService).proccessDiscount(any());
        doNothing().when(saleCommonService).arrondirMontantCaisse(any());

        // When
        SaleLineDTO result = salesManager.updateItemQuantitySold(dto, testVenteDepot);

        // Then
        assertNotNull(result);
        verify(salesLineService).updateItemQuantitySold(testSalesLine, dto, 1);
        verify(saleCommonService).computeSaleEagerAmount(testVenteDepot);
        verify(venteDepotRepository).saveAndFlush(testVenteDepot);
        verify(customerDisplayService).displaySaleTotal(testVenteDepot.getNetAmount());
    }
}

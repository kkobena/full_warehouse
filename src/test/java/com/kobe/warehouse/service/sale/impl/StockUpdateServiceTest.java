package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("StockUpdateService Tests")
class StockUpdateServiceTest {

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private LogsService logsService;

    private StockUpdateService stockUpdateService;

    @BeforeEach
    void setUp() {
        stockUpdateService = new StockUpdateService(stockProduitRepository, logsService);
    }

    @Test
    @DisplayName("Should update stock successfully with sufficient quantity")
    void testUpdateStock_SuccessfulUpdate() {
        // Given
        int storageId = 1;
        int initialQtyStock = 100;
        int initialQtyUG = 10;
        int quantityRequested = 5;
        int quantityUg = 2;

        SalesLine salesLine = createSalesLine(quantityRequested, quantityUg, 1000);
        StockProduit stockProduit = createStockProduit(initialQtyStock, initialQtyUG);

        when(stockProduitRepository.findOneByProduitIdAndStockageId(anyInt(), eq(storageId)))
            .thenReturn(stockProduit);

        // When
        StockUpdateService.StockUpdateResult result = stockUpdateService.updateStock(salesLine, storageId);

        // Then
        assertEquals(110, result.getQuantityBefore(), "Quantity before should be sum of stock and UG");
        assertEquals(105, result.getQuantityAfter(), "Quantity after should be reduced by requested amount");

        verify(stockProduitRepository).save(argThat(sp ->
            sp.getQtyStock() == (initialQtyStock - (quantityRequested - quantityUg)) &&
            sp.getQtyUG() == (initialQtyUG - quantityUg)
        ));

        verify(logsService, never()).create(eq(TransactionType.FORCE_STOCK), anyString(), anyString());
    }

    @Test
    @DisplayName("Should log force stock when quantity is insufficient")
    void testUpdateStock_ForceStockLogged() {
        // Given
        int storageId = 1;
        int initialQtyStock = 5;
        int initialQtyUG = 0;
        int quantityRequested = 10; // More than available

        SalesLine salesLine = createSalesLine(quantityRequested, 0, 1000);
        StockProduit stockProduit = createStockProduit(initialQtyStock, initialQtyUG);

        when(stockProduitRepository.findOneByProduitIdAndStockageId(anyInt(), eq(storageId)))
            .thenReturn(stockProduit);

        // When
        StockUpdateService.StockUpdateResult result = stockUpdateService.updateStock(salesLine, storageId);

        // Then
        assertEquals(5, result.getQuantityBefore());
        assertEquals(-5, result.getQuantityAfter());

        verify(logsService).create(
            eq(TransactionType.FORCE_STOCK),
            eq(TransactionType.FORCE_STOCK.getValue()),
            anyString()
        );
    }

    @Test
    @DisplayName("Should log price modification when sale price exceeds usual price")
    void testUpdateStock_PriceModificationLogged() {
        // Given
        int storageId = 1;
        int usualPrice = 100;
        int salePrice = 150; // Higher than usual

        SalesLine salesLine = createSalesLine(5, 0, salePrice);
        salesLine.getProduit().setFournisseurProduitPrincipal(createFournisseurProduit(usualPrice));

        StockProduit stockProduit = createStockProduit(100, 10);

        when(stockProduitRepository.findOneByProduitIdAndStockageId(anyInt(), eq(storageId)))
            .thenReturn(stockProduit);

        // When
        stockUpdateService.updateStock(salesLine, storageId);

        // Then
        ArgumentCaptor<String> descriptionCaptor = ArgumentCaptor.forClass(String.class);
        verify(logsService).create(
            eq(TransactionType.MODIFICATION_PRIX_PRODUCT_A_LA_VENTE),
            descriptionCaptor.capture(),
            anyString()
        );

        String description = descriptionCaptor.getValue();
        assertTrue(description.contains("modifié"), "Description should mention price modification");
        assertTrue(description.contains(String.valueOf(usualPrice)), "Description should contain usual price");
        assertTrue(description.contains(String.valueOf(salePrice)), "Description should contain sale price");
    }

    @Test
    @DisplayName("Should not log price modification when sale price is equal or lower")
    void testUpdateStock_NoPriceModificationWhenPriceLower() {
        // Given
        int storageId = 1;
        int usualPrice = 150;
        int salePrice = 100; // Lower than usual

        SalesLine salesLine = createSalesLine(5, 0, salePrice);
        salesLine.getProduit().setFournisseurProduitPrincipal(createFournisseurProduit(usualPrice));

        StockProduit stockProduit = createStockProduit(100, 10);

        when(stockProduitRepository.findOneByProduitIdAndStockageId(anyInt(), eq(storageId)))
            .thenReturn(stockProduit);

        // When
        stockUpdateService.updateStock(salesLine, storageId);

        // Then
        verify(logsService, never()).create(
            eq(TransactionType.MODIFICATION_PRIX_PRODUCT_A_LA_VENTE),
            anyString(),
            anyString()
        );
    }

    @Test
    @DisplayName("Should handle UG (Gestion d'Urgence) quantities correctly")
    void testUpdateStock_WithUGQuantities() {
        // Given
        int storageId = 1;
        int initialQtyStock = 100;
        int initialQtyUG = 20;
        int quantityRequested = 10;
        int quantityUg = 5;

        SalesLine salesLine = createSalesLine(quantityRequested, quantityUg, 1000);
        StockProduit stockProduit = createStockProduit(initialQtyStock, initialQtyUG);

        when(stockProduitRepository.findOneByProduitIdAndStockageId(anyInt(), eq(storageId)))
            .thenReturn(stockProduit);

        // When
        stockUpdateService.updateStock(salesLine, storageId);

        // Then
        verify(stockProduitRepository).save(argThat(sp ->
            sp.getQtyStock() == (initialQtyStock - (quantityRequested - quantityUg)) && // 100 - 5 = 95
            sp.getQtyUG() == (initialQtyUG - quantityUg) // 20 - 5 = 15
        ));
    }

    @Test
    @DisplayName("Should update timestamp on stock")
    void testUpdateStock_TimestampUpdated() {
        // Given
        int storageId = 1;
        SalesLine salesLine = createSalesLine(5, 0, 1000);
        StockProduit stockProduit = createStockProduit(100, 10);
        LocalDateTime oldTimestamp = stockProduit.getUpdatedAt();

        when(stockProduitRepository.findOneByProduitIdAndStockageId(anyInt(), eq(storageId)))
            .thenReturn(stockProduit);

        // When
        stockUpdateService.updateStock(salesLine, storageId);

        // Then
        verify(stockProduitRepository).save(argThat(sp ->
            sp.getUpdatedAt() != null &&
            (oldTimestamp == null || sp.getUpdatedAt().isAfter(oldTimestamp))
        ));
    }

    // Helper methods

    private SalesLine createSalesLine(int quantityRequested, int quantityUg, int regularUnitPrice) {
        SalesLine salesLine = new SalesLine();
        salesLine.setId(1L);
        salesLine.setSaleDate(LocalDate.now());
        salesLine.setQuantityRequested(quantityRequested);
        salesLine.setQuantityUg(quantityUg);
        salesLine.setRegularUnitPrice(regularUnitPrice);

        Produit produit = new Produit();
        produit.setId(1);
        produit.setLibelle("Test Product");
        salesLine.setProduit(produit);

        Sales sales = mock(Sales.class);
        lenient().when(sales.getNumberTransaction()).thenReturn("TEST-001");
        salesLine.setSales(sales);

        return salesLine;
    }

    private StockProduit createStockProduit(int qtyStock, int qtyUG) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setQtyStock(qtyStock);
        stockProduit.setQtyUG(qtyUG);
        stockProduit.setTotalStockQuantity(qtyStock + qtyUG);
        stockProduit.setUpdatedAt(LocalDateTime.now().minusHours(1));
        return stockProduit;
    }

    private FournisseurProduit createFournisseurProduit(int prixUni) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setPrixUni(prixUni);
        fournisseurProduit.setCodeCip("CIP123");
        return fournisseurProduit;
    }
}

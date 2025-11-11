package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalesLineServiceBaseImpl Tests")
class SalesLineServiceBaseImplTest {

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private SalesLineRepository salesLineRepository;

    @Mock
    private StockProduitRepository stockProduitRepository;

    @Mock
    private LogsService logsService;

    @Mock
    private SuggestionProduitService suggestionProduitService;

    @Mock
    private LotService lotService;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @Mock
    private SaleLineIdGeneratorService saleLineIdGeneratorService;

    private SalesLineServiceBaseImpl salesLineService;

    @BeforeEach
    void setUp() {
        salesLineService = new SalesLineServiceBaseImpl(
            produitRepository,
            salesLineRepository,
            stockProduitRepository,
            logsService,
            suggestionProduitService,
            lotService,
            inventoryTransactionService,
            saleLineIdGeneratorService
        );
    }

    @Test
    @DisplayName("Should create sales line from DTO with all required fields")
    void testCreateSaleLineFromDTO_Success() {
        // Given
        int storageId = 1;
        int produitId = 100;
        int regularUnitPrice = 500;
        int quantityRequested = 10;
        int quantitySold = 10;

        SaleLineDTO dto = new SaleLineDTO();
        dto.setProduitId(produitId);
        dto.setRegularUnitPrice(regularUnitPrice);
        dto.setQuantityRequested(quantityRequested);
        dto.setQuantitySold(quantitySold);

        Produit produit = createProduit(produitId, "Test Product", 400);
        StockProduit stockProduit = createStockProduit(100, 10);

        when(produitRepository.getReferenceById(produitId)).thenReturn(produit);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId, storageId))
            .thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        assertNotNull(result, "Sales line should be created");
        assertEquals(regularUnitPrice, result.getRegularUnitPrice(), "Regular unit price should match");
        assertEquals(regularUnitPrice, result.getNetUnitPrice(), "Net unit price should match");
        assertEquals(quantityRequested, result.getQuantityRequested(), "Quantity requested should match");
        assertEquals(quantitySold, result.getQuantitySold(), "Quantity sold should match");
        assertEquals(regularUnitPrice * quantityRequested, result.getSalesAmount(), "Sales amount should be calculated");
        assertEquals(0, result.getDiscountAmount(), "Discount should be zero initially");
        assertEquals(produit, result.getProduit(), "Product should be set");
        assertEquals(produit.getCostAmount(), result.getCostAmount(), "Cost amount should be set from product");
        assertEquals(produit.getTva().getTaux(), result.getTaxValue(), "Tax value should be set from product");
        assertNotNull(result.getCreatedAt(), "Created timestamp should be set");
        assertNotNull(result.getUpdatedAt(), "Updated timestamp should be set");
        assertNotNull(result.getEffectiveUpdateDate(), "Effective update date should be set");
    }

    @Test
    @DisplayName("Should process UG (Gestion d'Urgence) when stock has UG quantity")
    void testCreateSaleLineFromDTO_WithUG() {
        // Given
        int storageId = 1;
        int produitId = 100;
        int quantitySold = 15;

        SaleLineDTO dto = new SaleLineDTO();
        dto.setProduitId(produitId);
        dto.setRegularUnitPrice(500);
        dto.setQuantityRequested(20);
        dto.setQuantitySold(quantitySold);

        Produit produit = createProduit(produitId, "Test Product", 400);
        StockProduit stockProduit = createStockProduit(100, 10); // 10 UG available

        when(produitRepository.getReferenceById(produitId)).thenReturn(produit);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId, storageId))
            .thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        assertEquals(10, result.getQuantityUg(), "UG quantity should be limited to available stock UG");
    }

    @Test
    @DisplayName("Should not set UG when quantity sold is less than UG stock")
    void testCreateSaleLineFromDTO_LessQuantityThanUG() {
        // Given
        int storageId = 1;
        int produitId = 100;
        int quantitySold = 5; // Less than UG available

        SaleLineDTO dto = new SaleLineDTO();
        dto.setProduitId(produitId);
        dto.setRegularUnitPrice(500);
        dto.setQuantityRequested(5);
        dto.setQuantitySold(quantitySold);

        Produit produit = createProduit(produitId, "Test Product", 400);
        StockProduit stockProduit = createStockProduit(100, 10); // 10 UG available

        when(produitRepository.getReferenceById(produitId)).thenReturn(produit);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId, storageId))
            .thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        assertEquals(quantitySold, result.getQuantityUg(), "UG quantity should match quantity sold when less than stock UG");
    }

    @Test
    @DisplayName("Should not set UG when stock has no UG")
    void testCreateSaleLineFromDTO_NoUG() {
        // Given
        int storageId = 1;
        int produitId = 100;

        SaleLineDTO dto = new SaleLineDTO();
        dto.setProduitId(produitId);
        dto.setRegularUnitPrice(500);
        dto.setQuantityRequested(10);
        dto.setQuantitySold(10);

        Produit produit = createProduit(produitId, "Test Product", 400);
        StockProduit stockProduit = createStockProduit(100, 0); // No UG

        when(produitRepository.getReferenceById(produitId)).thenReturn(produit);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId, storageId))
            .thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        assertEquals(0, result.getQuantityUg(), "UG quantity should be zero when stock has no UG");
    }

    @Test
    @DisplayName("Should calculate sales amount correctly")
    void testCreateSaleLineFromDTO_SalesAmountCalculation() {
        // Given
        int storageId = 1;
        int produitId = 100;
        int regularUnitPrice = 250;
        int quantityRequested = 12;

        SaleLineDTO dto = new SaleLineDTO();
        dto.setProduitId(produitId);
        dto.setRegularUnitPrice(regularUnitPrice);
        dto.setQuantityRequested(quantityRequested);
        dto.setQuantitySold(quantityRequested);

        Produit produit = createProduit(produitId, "Test Product", 200);
        StockProduit stockProduit = createStockProduit(100, 0);

        when(produitRepository.getReferenceById(produitId)).thenReturn(produit);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId, storageId))
            .thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        int expectedSalesAmount = regularUnitPrice * quantityRequested; // 250 * 12 = 3000
        assertEquals(expectedSalesAmount, result.getSalesAmount(), "Sales amount should be price * quantity");
    }

    @Test
    @DisplayName("Should set correct tax value from product TVA")
    void testCreateSaleLineFromDTO_TaxValue() {
        // Given
        int storageId = 1;
        int produitId = 100;
        int tvaRate = 18; // 18% TVA

        SaleLineDTO dto = new SaleLineDTO();
        dto.setProduitId(produitId);
        dto.setRegularUnitPrice(500);
        dto.setQuantityRequested(10);
        dto.setQuantitySold(10);

        Produit produit = createProduit(produitId, "Test Product", 400);
        produit.getTva().setTaux(tvaRate);
        StockProduit stockProduit = createStockProduit(100, 0);

        when(produitRepository.getReferenceById(produitId)).thenReturn(produit);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId, storageId))
            .thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        assertEquals(tvaRate, result.getTaxValue(), "Tax value should match product TVA rate");
    }

    @Test
    @DisplayName("Should verify repository interactions")
    void testCreateSaleLineFromDTO_RepositoryInteractions() {
        // Given
        int storageId = 1;
        int produitId = 100;

        SaleLineDTO dto = new SaleLineDTO();
        dto.setProduitId(produitId);
        dto.setRegularUnitPrice(500);
        dto.setQuantityRequested(10);
        dto.setQuantitySold(10);

        Produit produit = createProduit(produitId, "Test Product", 400);
        StockProduit stockProduit = createStockProduit(100, 5);

        when(produitRepository.getReferenceById(produitId)).thenReturn(produit);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId, storageId))
            .thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        verify(produitRepository).getReferenceById(produitId);
        verify(stockProduitRepository).findOneByProduitIdAndStockageId(produitId, storageId);
        verify(saleLineIdGeneratorService).nextId();
    }

    // Helper methods

    private Produit createProduit(Integer id, String libelle, int costAmount) {
        Produit produit = new Produit();
        produit.setId(id);
        produit.setLibelle(libelle);
        produit.setCostAmount(costAmount);

        Tva tva = new Tva();
        tva.setTaux(0); // Default 0% TVA
        produit.setTva(tva);

        return produit;
    }

    private StockProduit createStockProduit(int qtyStock, int qtyUG) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setQtyStock(qtyStock);
        stockProduit.setQtyUG(qtyUG);
        return stockProduit;
    }
}

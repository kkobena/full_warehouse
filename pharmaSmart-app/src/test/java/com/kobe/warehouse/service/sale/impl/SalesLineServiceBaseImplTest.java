package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.GrilleRemise;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.DataMatrixInfo;
import com.kobe.warehouse.service.errors.DeconditionnementStockOut;
import com.kobe.warehouse.service.errors.QuantitySoldException;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.errors.StockInReserveException;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.sale.AvoirClientDocumentService;
import com.kobe.warehouse.service.stock.DataMatrixParserService;
import com.kobe.warehouse.service.stock.LotService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private LotService lotService;

    @Mock
    private InventoryTransactionService inventoryTransactionService;

    @Mock
    private SaleLineIdGeneratorService saleLineIdGeneratorService;
    @Mock
    private StockUpdateService stockUpdateService;

    private SalesLineServiceBaseImpl salesLineService;
    @Mock
    private StorageService storageService;
    @Mock
    private RepartitionStockService repartitionStockService;
    @Mock
    private  LotStockLocationService lotStockLocationService;
    @Mock
    private AvoirClientDocumentService avoirClientDocumentService;
    @Mock private DataMatrixParserService dataMatrixParserService;

    @BeforeEach
    void setUp() {
        Storage mainStorage = new Storage();
        mainStorage.setId(1);
        lenient().when(storageService.getDefaultConnectedUserMainStorage())
            .thenReturn(mainStorage);
        lenient().when(stockProduitRepository.findPointVenteStock(anyInt(), eq(1)))
            .thenReturn(100);
        salesLineService = new SalesLineServiceBaseImpl(
            produitRepository,
            salesLineRepository,
            stockProduitRepository,
            lotService,
            inventoryTransactionService,
            saleLineIdGeneratorService,
            stockUpdateService,
            storageService,
            repartitionStockService,
            lotStockLocationService,
            avoirClientDocumentService,
            dataMatrixParserService
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
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId,
            storageId)).thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        assertNotNull(result, "Sales line should be created");
        assertEquals(regularUnitPrice, result.getRegularUnitPrice(),
            "Regular unit price should match");
        assertEquals(regularUnitPrice, result.getNetUnitPrice(), "Net unit price should match");
        assertEquals(quantityRequested, result.getQuantityRequested(),
            "Quantity requested should match");
        assertEquals(quantitySold, result.getQuantitySold(), "Quantity sold should match");
        assertEquals(regularUnitPrice * quantityRequested, result.getSalesAmount(),
            "Sales amount should be calculated");
        assertEquals(0, result.getDiscountAmount(), "Discount should be zero initially");
        assertEquals(produit, result.getProduit(), "Product should be set");
        assertEquals(produit.getCostAmount(), result.getCostAmount(),
            "Cost amount should be set from product");
        assertEquals(produit.getTva().getTaux(), result.getTaxValue(),
            "Tax value should be set from product");
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
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId,
            storageId)).thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        assertEquals(10, result.getQuantityUg(),
            "UG quantity should be limited to available stock UG");
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
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId,
            storageId)).thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        assertEquals(quantitySold, result.getQuantityUg(),
            "UG quantity should match quantity sold when less than stock UG");
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
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId,
            storageId)).thenReturn(stockProduit);
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
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId,
            storageId)).thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        SalesLine result = salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        int expectedSalesAmount = regularUnitPrice * quantityRequested; // 250 * 12 = 3000
        assertEquals(expectedSalesAmount, result.getSalesAmount(),
            "Sales amount should be price * quantity");
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
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId,
            storageId)).thenReturn(stockProduit);
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
        when(stockProduitRepository.findOneByProduitIdAndStockageId(produitId,
            storageId)).thenReturn(stockProduit);
        when(saleLineIdGeneratorService.nextId()).thenReturn(1L);

        // When
        salesLineService.createSaleLineFromDTO(dto, storageId);

        // Then
        verify(produitRepository).getReferenceById(produitId);
        verify(stockProduitRepository).findOneByProduitIdAndStockageId(produitId, storageId);
        verify(saleLineIdGeneratorService).nextId();
    }

    @Test
    void delegatesCrudAndLookupOperations() {
        LocalDate date = LocalDate.now();
        SaleLineId id = new SaleLineId(4L, date);
        SalesLine line = completeLine();
        when(salesLineRepository.findById(id)).thenReturn(Optional.of(line));
        when(salesLineRepository.findBySalesIdAndProduitIdAndSalesSaleDate(8L, 100, date))
            .thenReturn(Optional.of(line));
        when(salesLineRepository.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(8L, date))
            .thenReturn(List.of(line));
        when(saleLineIdGeneratorService.nextId()).thenReturn(77L);

        assertEquals(line, salesLineService.getOneById(id));
        assertTrue(salesLineService.findBySalesIdAndProduitId(new SaleId(8L, date), 100).isPresent());
        assertEquals(1, salesLineService.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(8L, date).size());
        assertEquals(77L, salesLineService.getNextId());
        salesLineService.saveSalesLine(line);
        salesLineService.deleteSaleLine(line);

        verify(salesLineRepository).save(line);
        verify(salesLineRepository).delete(line);
        assertThrows(java.util.NoSuchElementException.class,
            () -> salesLineService.getOneById(new SaleLineId(99L, date)));
    }

    @Test
    void createsLineAndAddsItToSale() {
        SaleLineDTO dto = basicDto(2, 500);
        Produit product = createProduit(100, "Produit", 200);
        CashSale sale = new CashSale();
        sale.setSalesLines(new HashSet<>());
        when(produitRepository.getReferenceById(100)).thenReturn(product);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(100, 1))
            .thenReturn(createStockProduit(20, 0));
        when(saleLineIdGeneratorService.nextId()).thenReturn(9L);
        when(salesLineRepository.save(any(SalesLine.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SalesLine result = salesLineService.create(dto, 1, sale);

        assertEquals(sale, result.getSales());
        assertTrue(sale.getSalesLines().contains(result));
    }

    @Test
    void updatesPriceRequestedAndSoldQuantities() {
        SalesLine line = completeLine();
        SaleLineDTO dto = basicDto(3, 700);
        dto.setQuantitySold(2);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(100, 1))
            .thenReturn(createStockProduit(20, 4));

        salesLineService.updateItemRegularPrice(dto, line, 1);
        assertEquals(1_400, line.getSalesAmount());

        salesLineService.updateItemQuantityRequested(dto, line, 1);
        assertEquals(3, line.getQuantityRequested());

        dto.setQuantityRequested(2);
        salesLineService.incrementItemQuantityRequested(dto, line, 1);
        assertEquals(5, line.getQuantityRequested());

        dto.setQuantitySold(4);
        salesLineService.updateItemQuantitySold(line, dto, 1);
        assertEquals(1, line.getQuantityAvoir());
        verify(salesLineRepository, times(4)).save(line);
    }

    @Test
    void validatesRequestedAndSoldQuantities() {
        SalesLine line = completeLine();
        SaleLineDTO dto = basicDto(101, 500);
        when(stockProduitRepository.findPointVenteStock(100, 1)).thenReturn(2);
        when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(null);
        assertThrows(StockException.class,
            () -> salesLineService.updateItemQuantityRequested(dto, line, 1));

        Produit parent = new Produit();
        parent.setId(200);
        line.getProduit().setParent(parent);
        assertThrows(DeconditionnementStockOut.class,
            () -> salesLineService.updateItemQuantityRequested(dto, line, 1));

        dto.setQuantitySold(20);
        assertThrows(QuantitySoldException.class,
            () -> salesLineService.updateItemQuantitySold(line, dto, 1));
    }

    @Test
    void signalsReserveOrTransfersItWhenForced() {
        SalesLine line = completeLine();
        SaleLineDTO dto = basicDto(8, 500);
        Storage reserve = new Storage();
        reserve.setId(2);
        when(stockProduitRepository.findPointVenteStock(100, 1)).thenReturn(3);
        when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
        when(stockProduitRepository.findReserveStock(100, 2)).thenReturn(4);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(100, 1))
            .thenReturn(createStockProduit(7, 0));

        assertThrows(StockInReserveException.class,
            () -> salesLineService.updateItemQuantityRequested(dto, line, 1));

        dto.setForceStock(true);
        salesLineService.updateItemQuantityRequested(dto, line, 1);
        verify(repartitionStockService).transfertImpliciteReserveVersRayon(100, 1, 2, 4);
        assertEquals(7, line.getQuantitySold());
    }

    @Test
    void appliesProductDiscountForCashAndNonCashSales() {
        SalesLine line = completeLine();
        RemiseProduit remise = new RemiseProduit();
        GrilleRemise cashGrid = new GrilleRemise();
        cashGrid.setCode(CodeRemise.CODE_0.getCodeVno());
        cashGrid.setTauxRemise(0.10f);
        remise.setGrilles(List.of(cashGrid));
        CashSale sale = new CashSale();
        sale.setRemise(remise);
        line.setSales(sale);
        line.setSalesAmount(1_000);
        line.getProduit().setCodeRemise(CodeRemise.CODE_0);

        salesLineService.processProductDiscount(line);

        assertEquals(100, line.getDiscountAmount());
        assertEquals(0.10f, line.getTauxRemise());
    }

    @Test
    void savesLinesUpdatesStockAndCreatesCreditForShortfall() {
        SalesLine line = completeLine();
        line.setQuantityRequested(5);
        line.setQuantitySold(3);
        line.setCodeScan(null);
        AppUser user = new AppUser();
        CashSale sale = new CashSale();
        sale.setCustomer(null);
        line.setSales(sale);
        Storage storage = new Storage();
        storage.setId(1);
        StockProduit stock = createStockProduit(10, 0);
        stock.setStorage(storage);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(100, 1)).thenReturn(stock);
        when(stockUpdateService.updateStock(line, stock))
            .thenReturn(new StockUpdateService.StockUpdateResult(10, 7));

        salesLineService.save(Set.of(line), user, 1);

        assertEquals(2, line.getQuantityAvoir());
        assertEquals(10, line.getInitStock());
        assertEquals(7, line.getAfterStock());
        verify(inventoryTransactionService).save(line);
        verify(avoirClientDocumentService).createAvoirsFromSale(line, null);
    }

    @Test
    void clonesLinesForCopyAndCancellation() {
        SalesLine line = completeLine();
        line.setQuantityAvoir(1);
        line.setQuantityUg(1);
        line.setAmountToBeTakenIntoAccount(1_000);
        line.setLots(new java.util.ArrayList<>());
        CashSale copy = new CashSale();
        when(saleLineIdGeneratorService.nextId()).thenReturn(55L);

        Set<SalesLine> copies = salesLineService.cloneSalesLine(Set.of(line), copy);
        assertEquals(1, copies.size());
        assertEquals(copy, copies.iterator().next().getSales());

        Storage storage = new Storage();
        storage.setId(1);
        StockProduit stock = createStockProduit(20, 2);
        stock.setStorage(storage);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(100, 1)).thenReturn(stock);
        salesLineService.cloneSalesLine(Set.of(line), copy, new AppUser(), 1);

        verify(salesLineRepository, times(2)).save(any(SalesLine.class));
        verify(stockUpdateService).updateStockOnCancellation(any(SalesLine.class), eq(stock));
        verify(avoirClientDocumentService).cancelAvoirsFromSale(4L);
    }

    @Test
    void saveAllIgnoresEmptyCollections() {
        salesLineService.saveAll(Set.of());
        verify(salesLineRepository, never()).saveAll(any());
        Set<SalesLine> lines = Set.of(completeLine());
        salesLineService.saveAll(lines);
        verify(salesLineRepository).saveAll(lines);
    }

    @Test
    void buildsImportedLineFromDtoAndRejectsUnknownProduct() {
        SaleLineDTO dto = basicDto(3, 600);
        dto.setProduitLibelle("  Produit importé  ");
        dto.setCreatedAt(LocalDateTime.now().minusDays(1));
        dto.setUpdatedAt(LocalDateTime.now());
        dto.setCostAmount(250);
        dto.setSalesAmount(1_800);
        dto.setDiscountAmount(100);
        dto.setQuantiyAvoir(1);
        dto.setQuantityUg(1);
        dto.setTaxValue(18);
        dto.setAmountToBeTakenIntoAccount(1_700);
        dto.setEffectiveUpdateDate(LocalDateTime.now());
        Produit product = createProduit(100, "Produit importé", 250);
        when(produitRepository.findOneByLibelle("Produit importé")).thenReturn(Optional.of(product));
        when(saleLineIdGeneratorService.nextId()).thenReturn(88L);

        SalesLine result = salesLineService.buildSaleLineFromDTO(dto);

        assertEquals(88L, result.getId().getId());
        assertEquals(3, result.getQuantitySold());
        assertEquals(1, result.getQuantityAvoir());
        assertEquals(1_700, result.getAmountToBeTakenIntoAccount());

        when(produitRepository.findOneByLibelle("Inconnu")).thenReturn(Optional.empty());
        dto.setProduitLibelle("Inconnu");
        assertThrows(java.util.NoSuchElementException.class,
            () -> salesLineService.buildSaleLineFromDTO(dto));
    }

    @Test
    void updatesExistingLineCumulativelyAndValidatesStock() {
        SalesLine line = completeLine();
        SaleLineDTO dto = basicDto(3, 700);
        dto.setCodeScan("SCAN");
        when(stockProduitRepository.findOneByProduitIdAndStockageId(100, 1))
            .thenReturn(createStockProduit(20, 0));

        salesLineService.updateSaleLine(dto, line, 1);

        assertEquals(5, line.getQuantityRequested());
        assertEquals(3_500, line.getSalesAmount());
        assertEquals("SCAN", line.getCodeScan());

        when(stockProduitRepository.findPointVenteStock(100, 1)).thenReturn(1);
        when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(null);
        SaleLineDTO excessive = basicDto(10, 700);
        assertThrows(StockException.class,
            () -> salesLineService.updateSaleLine(excessive, line, 1));
        Produit parent = new Produit();
        parent.setId(200);
        line.getProduit().setParent(parent);
        assertThrows(DeconditionnementStockOut.class,
            () -> salesLineService.updateSaleLine(excessive, line, 1));
    }

    @Test
    void appliesThirdPartyProductDiscount() {
        SalesLine line = completeLine();
        ThirdPartySales sale = new ThirdPartySales();
        RemiseProduit remise = new RemiseProduit();
        GrilleRemise grid = new GrilleRemise();
        line.getProduit().setCodeRemise(CodeRemise.CODE_0);
        grid.setCode(CodeRemise.CODE_0.getCodeVo());
        grid.setTauxRemise(0.15f);
        remise.setGrilles(List.of(grid));
        sale.setRemise(remise);
        line.setSales(sale);

        salesLineService.processProductDiscount(line);

        assertEquals(150, line.getDiscountAmount());
        assertEquals(0.15f, line.getTauxRemise());
    }

    @Test
    void allocatesScannedLotFirstThenRemainingQuantityByFefo() {
        SalesLine line = completeLine();
        line.setQuantitySold(5);
        line.setQuantityRequested(5);
        line.setCodeScan("DATAMATRIX");
        line.setLots(new java.util.ArrayList<>());
        CashSale sale = new CashSale();
        sale.setCustomer(null);
        line.setSales(sale);
        Storage storage = new Storage();
        storage.setId(1);
        StockProduit stock = createStockProduit(10, 0);
        stock.setStorage(storage);
        Lot scanned = new Lot().setId(1).setNumLot("LOT-A").setCurrentQuantity(2)
            .setExpiryDate(LocalDate.now().plusMonths(2));
        Lot fefo = new Lot().setId(2).setNumLot("LOT-B").setCurrentQuantity(3)
            .setExpiryDate(LocalDate.now().plusMonths(1));
        DataMatrixInfo info = DataMatrixInfo.builder().batchNumber("LOT-A").build();
        when(stockProduitRepository.findOneByProduitIdAndStockageId(100, 1)).thenReturn(stock);
        when(dataMatrixParserService.parse("DATAMATRIX")).thenReturn(Optional.of(info));
        when(lotService.findByProduitIdAndNumLot(100, "LOT-A")).thenReturn(Optional.of(scanned));
        when(lotService.findByProduitId(100)).thenReturn(List.of(scanned, fefo));
        when(stockUpdateService.updateStock(line, stock))
            .thenReturn(new StockUpdateService.StockUpdateResult(10, 5));

        salesLineService.save(Set.of(line), new AppUser(), 1);

        assertEquals(2, line.getLots().size());
        verify(lotStockLocationService).debit(scanned, storage, 2);
        verify(lotStockLocationService).debit(fefo, storage, 3);
        verify(lotService).updateLots(line.getLots());
    }

    @Test
    void rejectsSoldQuantityAboveAvailableStock() {
        SalesLine line = completeLine();
        line.setQuantityRequested(5);
        SaleLineDTO dto = basicDto(5, 500);
        dto.setQuantitySold(3);
        when(stockProduitRepository.findPointVenteStock(100, 1)).thenReturn(2);
        when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(null);

        assertThrows(StockException.class,
            () -> salesLineService.updateItemQuantitySold(line, dto, 1));
    }

    @Test
    void forcedCreationWithNoStockCreatesZeroQuantitySoldAndEmptyCloneIsAllowed() {
        SaleLineDTO dto = basicDto(2, 500);
        dto.setForceStock(true);
        Produit product = createProduit(100, "Produit", 200);
        when(produitRepository.getReferenceById(100)).thenReturn(product);
        when(stockProduitRepository.findPointVenteStock(100, 1)).thenReturn(0);
        when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(null);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(100, 1))
            .thenReturn(createStockProduit(0, 0));

        SalesLine result = salesLineService.createSaleLineFromDTO(dto, 1);

        assertEquals(0, result.getQuantitySold());
        assertTrue(salesLineService.cloneSalesLine(Set.of(), new CashSale()).isEmpty());
    }

    @Test
    void rejectsCreationWhenRayonAndReserveCannotFulfilRequest() {
        SaleLineDTO dto = basicDto(5, 500);
        Produit product = createProduit(100, "Produit", 200);
        when(produitRepository.getReferenceById(100)).thenReturn(product);
        when(stockProduitRepository.findPointVenteStock(100, 1)).thenReturn(2);
        when(storageService.getDefaultConnectedUserReserveStorage()).thenReturn(null);

        assertThrows(StockException.class,
            () -> salesLineService.createSaleLineFromDTO(dto, 1));

        Produit parent = new Produit();
        parent.setId(200);
        product.setParent(parent);
        assertThrows(DeconditionnementStockOut.class,
            () -> salesLineService.createSaleLineFromDTO(dto, 1));
    }

    @Test
    void ignoresProductDiscountWhenGridIsEmpty() {
        SalesLine line = completeLine();
        CashSale sale = new CashSale();
        RemiseProduit remise = new RemiseProduit();
        remise.setGrilles(List.of());
        sale.setRemise(remise);
        line.setSales(sale);

        salesLineService.processProductDiscount(line);

        assertEquals(0, line.getDiscountAmount());
    }

    @Test
    void skipsLotAllocationWhenNothingWasSold() {
        SalesLine line = completeLine();
        line.setQuantityRequested(0);
        line.setQuantitySold(0);
        line.setLots(new java.util.ArrayList<>());
        CashSale sale = new CashSale();
        line.setSales(sale);
        Storage storage = new Storage();
        storage.setId(1);
        StockProduit stock = createStockProduit(0, 0);
        stock.setStorage(storage);
        when(stockProduitRepository.findOneByProduitIdAndStockageId(100, 1)).thenReturn(stock);
        when(stockUpdateService.updateStock(line, stock))
            .thenReturn(new StockUpdateService.StockUpdateResult(0, 0));

        salesLineService.save(Set.of(line), new AppUser(), 1);

        verify(lotService, never()).findByProduitId(anyInt());
        verify(lotService, never()).updateLots(any());
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

    private SaleLineDTO basicDto(int quantity, int price) {
        SaleLineDTO dto = new SaleLineDTO();
        dto.setProduitId(100);
        dto.setQuantityRequested(quantity);
        dto.setQuantitySold(quantity);
        dto.setRegularUnitPrice(price);
        return dto;
    }

    private SalesLine completeLine() {
        SalesLine line = new SalesLine();
        line.setId(4L);
        line.setSaleDate(LocalDate.now());
        line.setProduit(createProduit(100, "Produit", 200));
        line.setQuantityRequested(2);
        line.setQuantitySold(2);
        line.setRegularUnitPrice(500);
        line.setNetUnitPrice(500);
        line.setSalesAmount(1_000);
        line.setCostAmount(200);
        line.setTaxValue(0);
        line.setDiscountAmount(0);
        line.setCreatedAt(LocalDateTime.now());
        line.setUpdatedAt(LocalDateTime.now());
        return line;
    }
}

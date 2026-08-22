package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourClient;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.ModeReglementRetour;
import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.StatutLegal;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.RetourClientRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest.RetourLineRequest;
import com.kobe.warehouse.service.sale.dto.RetourClientResultDTO;
import com.kobe.warehouse.service.sale.dto.SaleForRetourDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class RetourClientServiceImplTest {

    @Mock private RetourClientRepository retourRepository;
    @Mock private SalesRepository salesRepository;
    @Mock private SalesLineRepository lineRepository;
    @Mock private StockProduitRepository stockRepository;
    @Mock private AvoirClientRepository avoirRepository;
    @Mock private ReferenceService referenceService;
    @Mock private StorageService storageService;
    @Mock private InventoryTransactionService inventoryService;
    @Mock private AppConfigurationService configurationService;

    private RetourClientServiceImpl service;
    private LocalDate saleDate;
    private AppUser user;
    private Storage storage;

    @BeforeEach
    void setUp() {
        saleDate = LocalDate.now().minusDays(5);
        user = new AppUser();
        user.setId(1);
        user.setFirstName("Alice");
        user.setLastName("Martin");
        storage = new Storage();
        storage.setId(4);
        service = new RetourClientServiceImpl(
            retourRepository, salesRepository, lineRepository, stockRepository, avoirRepository,
            referenceService, storageService, inventoryService, configurationService
        );
    }

    @Test
    void findsSaleByReferenceAndMapsEligibleLines() {
        Sales sale = sale(NatureVente.COMPTANT, 1_000, 600);
        UninsuredCustomer customer = new UninsuredCustomer();
        customer.setFirstName("Jean");
        customer.setLastName(null);
        sale.setCustomer(customer);
        SalesLine eligible = line(10L, StatutLegal.SANS_LISTE, false, 2, 500);
        SalesLine ignored = line(11L, StatutLegal.SANS_LISTE, false, 0, 100);
        when(salesRepository.findByNumberTransaction("V-1")).thenReturn(List.of(sale));
        when(lineRepository.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(1L, saleDate))
            .thenReturn(List.of(eligible, ignored));
        when(configurationService.getDelaiRetourClient()).thenReturn(30);

        SaleForRetourDTO result = service.findSaleByRef("V-1");

        assertEquals("Jean", result.customerName());
        assertFalse(result.hasTiersPayant());
        assertFalse(result.depasseDelai());
        assertEquals(1, result.lines().size());
        assertEquals(600, result.lines().getFirst().montantRemboursableClient());
        assertEquals(400, result.lines().getFirst().montantTp());
    }

    @Test
    void findsOldThirdPartySaleByCompositeIdAndUsesProductFallbacks() {
        saleDate = LocalDate.now().minusDays(40);
        Sales sale = sale(NatureVente.ASSURANCE, 0, null);
        SalesLine line = line(10L, null, true, 1, 500);
        line.getProduit().setFournisseurProduitPrincipal(null);
        line.getProduit().setCodeEanLaboratoire("EAN-1");
        when(salesRepository.findById(new SaleId(1L, saleDate))).thenReturn(Optional.of(sale));
        when(lineRepository.findBySalesIdAndSalesSaleDateOrderByProduitLibelle(1L, saleDate))
            .thenReturn(List.of(line));
        when(configurationService.getDelaiRetourClient()).thenReturn(30);

        SaleForRetourDTO result = service.findSaleById(1L, saleDate);

        assertTrue(result.hasTiersPayant());
        assertTrue(result.depasseDelai());
        assertTrue(result.lines().getFirst().thermosensible());
        assertEquals("EAN-1", result.lines().getFirst().codeCip());
        assertEquals(500, result.lines().getFirst().montantRemboursableClient());
    }

    @Test
    void rejectsUnknownSalesAndEmptyRequests() {
        when(salesRepository.findByNumberTransaction("UNKNOWN")).thenReturn(List.of());
        when(salesRepository.findById(new SaleId(99L, saleDate))).thenReturn(Optional.empty());

        assertThrows(GenericError.class, () -> service.findSaleByRef("UNKNOWN"));
        assertThrows(GenericError.class, () -> service.findSaleById(99L, saleDate));
        assertThrows(GenericError.class, () -> service.validerRetour(request(List.of(), false)));
    }

    @Test
    void rejectsForbiddenProductsAndFailsWhenNothingCanBeReturned() {
        SalesLine forbidden = line(10L, StatutLegal.STUPEFIANTS, false, 2, 500);
        when(lineRepository.findById(new SaleLineId(10L, saleDate))).thenReturn(Optional.of(forbidden));
        useCurrentUserAndStorage();

        GenericError error = assertThrows(GenericError.class,
            () -> service.validerRetour(request(List.of(requestLine(10L, 1, true, true, true)), false)));

        assertTrue(error.getMessage().contains("Aucune ligne retournable"));
        verify(stockRepository, never()).save(any());
    }

    @Test
    void rejectsZeroAndExcessiveQuantities() {
        SalesLine sold = line(10L, StatutLegal.SANS_LISTE, false, 2, 500);
        when(lineRepository.findById(new SaleLineId(10L, saleDate))).thenReturn(Optional.of(sold));
        useCurrentUserAndStorage();

        assertThrows(GenericError.class,
            () -> service.validerRetour(request(List.of(requestLine(10L, 0, true, true, true)), false)));
        assertThrows(GenericError.class,
            () -> service.validerRetour(request(List.of(requestLine(10L, 3, true, true, true)), false)));
    }

    @Test
    void validatesCashRefundAndRestocksEligibleProduct() {
        SalesLine sold = line(10L, StatutLegal.SANS_LISTE, false, 2, 500);
        StockProduit stock = new StockProduit();
        stock.setQtyStock(7);
        useCurrentUserAndStorage();
        when(referenceService.buildNumRetourClient()).thenReturn("RET-1");
        when(lineRepository.findById(new SaleLineId(10L, saleDate))).thenReturn(Optional.of(sold));
        when(stockRepository.findOneByProduitIdAndStockageId(100, 4)).thenReturn(stock);
        when(retourRepository.save(any(RetourClient.class))).thenAnswer(invocation ->
            ((RetourClient) invocation.getArgument(0)).setId(20));

        RetourClientResultDTO result = service.validerRetour(
            request(List.of(requestLine(10L, 1, null, null, null)), false));

        assertFalse(result.partiel());
        assertNull(result.echangeContext());
        assertEquals(300, result.retour().montantTotal());
        assertEquals(200, result.retour().montantTpTotal());
        assertEquals(8, stock.getQtyStock());
        verify(stockRepository).save(stock);
        verify(inventoryService).save(any(com.kobe.warehouse.domain.RetourClientLine.class));
        verify(avoirRepository, never()).saveAll(anyList());
    }

    @Test
    void validatesExchangeCreatesCreditAndReportsThermosensitiveAnomaly() {
        SalesLine sold = line(10L, StatutLegal.SANS_LISTE, true, 2, 500);
        StockProduit stock = new StockProduit();
        stock.setQtyStock(7);
        useCurrentUserAndStorage();
        when(referenceService.buildNumRetourClient()).thenReturn("RET-2");
        when(referenceService.buildNumAvoirClient()).thenReturn("AV-1");
        when(configurationService.getDelaiValiditeAvoir()).thenReturn(60);
        when(lineRepository.findById(new SaleLineId(10L, saleDate))).thenReturn(Optional.of(sold));
        when(stockRepository.findOneByProduitIdAndStockageId(100, 4)).thenReturn(stock);
        when(retourRepository.save(any(RetourClient.class))).thenAnswer(invocation ->
            ((RetourClient) invocation.getArgument(0)).setId(21));
        when(avoirRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        RetourClientResultDTO result = service.validerRetour(
            request(List.of(requestLine(10L, 1, true, true, true)), true));

        assertTrue(result.partiel());
        assertEquals(1, result.lignesNonRestockees().size());
        assertNotNull(result.echangeContext());
        assertEquals(List.of("AV-1"), result.echangeContext().avoirReferences());
        assertEquals(7, stock.getQtyStock());
        verify(stockRepository, never()).save(stock);
        verify(avoirRepository).saveAll(anyList());
    }

    @Test
    void marksDamagedNonThermosensitiveProductAsNotRestocked() {
        SalesLine sold = line(10L, StatutLegal.SANS_LISTE, false, 2, 500);
        StockProduit stock = new StockProduit();
        stock.setQtyStock(7);
        useCurrentUserAndStorage();
        when(lineRepository.findById(new SaleLineId(10L, saleDate))).thenReturn(Optional.of(sold));
        when(stockRepository.findOneByProduitIdAndStockageId(100, 4)).thenReturn(stock);
        when(retourRepository.save(any(RetourClient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RetourClientResultDTO result = service.validerRetour(
            request(List.of(requestLine(10L, 1, false, true, true)), false));

        assertTrue(result.partiel());
        assertTrue(result.lignesRejetees().isEmpty());
        assertEquals(1, result.lignesNonRestockees().size());
    }

    @Test
    void linksExchangeAndRejectsInvalidLinks() {
        RetourClient exchange = retour(true);
        when(retourRepository.findById(1)).thenReturn(Optional.of(exchange));
        when(retourRepository.save(exchange)).thenReturn(exchange);

        RetourClientDTO result = service.lierVenteEchange(1, "SALE-NEW");
        assertEquals("SALE-NEW", result.echangeSaleRef());

        RetourClient ordinary = retour(false);
        when(retourRepository.findById(2)).thenReturn(Optional.of(ordinary));
        when(retourRepository.findById(3)).thenReturn(Optional.empty());
        assertThrows(GenericError.class, () -> service.lierVenteEchange(2, "X"));
        assertThrows(GenericError.class, () -> service.lierVenteEchange(3, "X"));
    }

    @Test
    void findsReturnByIdAndListsMappedReturns() {
        RetourClient retour = retour(false);
        when(retourRepository.findById(1)).thenReturn(Optional.of(retour));
        when(retourRepository.findById(404)).thenReturn(Optional.empty());
        PageRequest pageable = PageRequest.of(0, 10);
        when(retourRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
            org.mockito.ArgumentMatchers.eq(pageable))).thenReturn(new PageImpl<>(List.of(retour)));

        assertEquals("RET", service.findById(1).reference());
        assertThrows(GenericError.class, () -> service.findById(404));
        assertEquals(1, service.findAll("RET", saleDate, LocalDate.now(), pageable).getTotalElements());
    }

    private void useCurrentUserAndStorage() {
        when(storageService.getUser()).thenReturn(user);
        when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(storage);
    }

    private RetourClientRequest request(List<RetourLineRequest> lines, boolean exchange) {
        return new RetourClientRequest(
            1L, saleDate, MotifRetourClient.ERREUR_DISPENSATION,
            ModeReglementRetour.REMBOURSEMENT_ESPECES, "commentaire", lines, exchange
        );
    }

    private RetourLineRequest requestLine(long id, int quantity, Boolean packaging, Boolean lot, Boolean expiry) {
        return new RetourLineRequest(id, saleDate, quantity, packaging, lot, expiry);
    }

    private Sales sale(NatureVente nature, Integer amount, Integer patientPart) {
        Sales sale = new com.kobe.warehouse.domain.CashSale();
        sale.setId(1L);
        sale.setSaleDate(saleDate);
        sale.setNumberTransaction("V-1");
        sale.setNatureVente(nature);
        sale.setSalesAmount(amount);
        sale.setAmountToBePaid(patientPart);
        return sale;
    }

    private SalesLine line(long id, StatutLegal legalStatus, boolean thermosensitive, int quantity, int price) {
        Produit product = new Produit();
        product.setId(100);
        product.setLibelle("Produit test");
        product.setStatutLegal(legalStatus);
        product.setThermosensible(thermosensitive);
        FournisseurProduit supplierProduct = new FournisseurProduit();
        supplierProduct.setCodeCip("CIP-1");
        product.setFournisseurProduitPrincipal(supplierProduct);
        Sales sale = sale(NatureVente.ASSURANCE, 1_000, 600);
        SalesLine line = new SalesLine();
        line.setId(id);
        line.setSaleDate(saleDate);
        line.setSales(sale);
        line.setProduit(product);
        line.setQuantitySold(quantity);
        line.setRegularUnitPrice(price);
        line.setNetUnitPrice(price);
        line.setCostAmount(200);
        return line;
    }

    private RetourClient retour(boolean exchange) {
        return new RetourClient()
            .setId(1)
            .setReference("RET")
            .setCreatedBy(user)
            .setModeReglement(ModeReglementRetour.REMBOURSEMENT_ESPECES)
            .setMotif(MotifRetourClient.AUTRE)
            .setAvecEchange(exchange)
            .setLines(List.of());
    }
}


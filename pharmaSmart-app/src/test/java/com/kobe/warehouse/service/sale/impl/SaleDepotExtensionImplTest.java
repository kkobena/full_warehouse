package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.repository.PosteRepository;
import com.kobe.warehouse.repository.RemiseRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.VenteDepotRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.MagasinDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.sale.SalesLineService;
import com.kobe.warehouse.service.sale.SalesManager;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.CustomerDisplayService;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleDepotExtensionImplTest {

    @Mock private RemiseRepository remiseRepository;
    @Mock private ReferenceService referenceService;
    @Mock private StorageService storageService;
    @Mock private UserRepository userRepository;
    @Mock private SaleLineServiceFactory lineServiceFactory;
    @Mock private CashRegisterService cashRegisterService;
    @Mock private PosteRepository posteRepository;
    @Mock private CustomerDisplayService customerDisplayService;
    @Mock private SaleIdGeneratorService idGeneratorService;
    @Mock private VenteDepotRepository repository;
    @Mock private StockUpdateService stockUpdateService;
    @Mock private InventoryTransactionService inventoryTransactionService;
    @Mock private SalesManager salesManager;
    @Mock private AppConfigurationService appConfigurationService;
    @Mock private SalesLineService lineService;

    private SaleDepotExtensionImpl service;
    private AppUser user;
    private Storage mainStorage;
    private LocalDate date;

    @BeforeEach
    void setUp() {
        date = LocalDate.of(2026, 8, 21);
        user = new AppUser();
        user.setId(1);
        user.setFirstName("Jean");
        user.setLastName("Test");
        user.setMagasin(new Magasin());
        mainStorage = new Storage();
        mainStorage.setId(3);
        lenient().when(lineServiceFactory.getService(TypeVente.VenteDepot)).thenReturn(lineService);
        lenient().when(storageService.getUser()).thenReturn(user);
        lenient().when(storageService.getDefaultConnectedUserMainStorage()).thenReturn(mainStorage);
        lenient().when(idGeneratorService.nextId()).thenReturn(50L);
        lenient().when(referenceService.buildNumPreventeSale()).thenReturn("PRE");
        lenient().when(referenceService.buildNumSale()).thenReturn("SALE");
        lenient().when(posteRepository.findFirstByAddressOrName(any(), any())).thenReturn(Optional.empty());
        service = new SaleDepotExtensionImpl(
            remiseRepository, referenceService, storageService, userRepository, lineServiceFactory,
            cashRegisterService, posteRepository, customerDisplayService, idGeneratorService,
            repository, stockUpdateService, inventoryTransactionService, new ObjectMapper(),
            salesManager, appConfigurationService
        );
    }

    @Test
    void createsDepotSaleAndItsOnlyLine() {
        DepotExtensionSaleDTO dto = new DepotExtensionSaleDTO();
        MagasinDTO magasin = new MagasinDTO();
        magasin.setId(7);
        dto.setMagasin(magasin);
        SaleLineDTO lineDto = new SaleLineDTO();
        dto.setSalesLines(List.of(lineDto));
        SalesLine line = validLine();
        when(lineService.createSaleLineFromDTO(lineDto, 3)).thenReturn(line);
        when(repository.save(any(VenteDepot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepotExtensionSaleDTO result = service.create(dto);

        assertEquals(50L, result.getSaleId().getId());
        assertEquals(7, result.getMagasin().getId());
        verify(lineService).saveSalesLine(line);
    }

    @Test
    void delegatesAllLineMutationsUsingCompositeSaleId() throws Exception {
        SaleId id = new SaleId(8L, date);
        VenteDepot sale = new VenteDepot();
        sale.setId(id.getId());
        sale.setSaleDate(id.getSaleDate());
        SaleLineDTO dto = new SaleLineDTO();
        dto.setSaleCompositeId(id);
        when(repository.getReferenceById(id)).thenReturn(sale);
        when(salesManager.updateItemQuantityRequested(dto, sale, true)).thenReturn(dto);
        when(salesManager.updateItemQuantitySold(dto, sale)).thenReturn(dto);
        when(salesManager.updateItemRegularPrice(dto, sale)).thenReturn(dto);
        when(salesManager.addOrUpdateSaleLine(dto, sale)).thenReturn(dto);

        assertSame(dto, service.updateItemQuantityRequested(dto, true));
        assertSame(dto, service.updateItemQuantitySold(dto));
        assertSame(dto, service.updateItemRegularPrice(dto));
        assertSame(dto, service.addOrUpdateSaleLine(dto));
    }

    @Test
    void finalizesSaleOpensRegisterAndCreatesDepotInventoryTransaction() {
        SaleId id = new SaleId(8L, date);
        DepotExtensionSaleDTO dto = new DepotExtensionSaleDTO();
        dto.setSaleId(id);
        dto.setPayrollAmount(500);
        dto.setRestToPay(0);
        Storage depotStorage = new Storage();
        depotStorage.setId(22);
        Magasin depot = new Magasin();
        depot.setId(7);
        depot.setPrimaryStorage(depotStorage);
        VenteDepot sale = activeSale(id, depot);
        CashRegister opened = new CashRegister();
        when(repository.findOneWithEagerSalesLines(8L, date)).thenReturn(Optional.of(sale));
        when(cashRegisterService.getLastOpiningUserCashRegisterByUser(user)).thenReturn(null);
        when(cashRegisterService.openCashRegister(user, user)).thenReturn(opened);
        when(stockUpdateService.updateStockDepot(any(SalesLine.class), eq(depotStorage)))
            .thenReturn(new StockUpdateService.StockUpdateResult(4, 6));

        FinalyseSaleDTO result = service.save(dto);

        assertEquals(id, result.saleId());
        assertEquals(SalesStatut.CLOSED, sale.getStatut());
        assertEquals(PaymentStatus.IMPAYE, sale.getPaymentStatus());
        assertSame(opened, sale.getCashRegister());
        verify(lineService).save(sale.getSalesLines(), user, 3);
        verify(inventoryTransactionService)
            .saveVenteDepotExtensionInventoryTransactions(eq(depot), any(List.class));
        verify(repository).save(sale);
    }

    @Test
    void rejectsFinalizationWhenDepotIsMissing() {
        SaleId id = new SaleId(8L, date);
        DepotExtensionSaleDTO dto = new DepotExtensionSaleDTO();
        dto.setSaleId(id);
        VenteDepot sale = activeSale(id, null);
        when(repository.findOneWithEagerSalesLines(8L, date)).thenReturn(Optional.of(sale));

        assertThrows(GenericError.class, () -> service.save(dto));
        verify(repository, never()).save(any());
    }

    @Test
    void deletesLineAndDraftSale() {
        SaleId id = new SaleId(8L, date);
        SaleLineId lineId = new SaleLineId(4L, date);
        SalesLine line = validLine();
        VenteDepot sale = activeSale(id, new Magasin());
        when(lineService.getOneById(lineId)).thenReturn(line);
        when(repository.getReferenceById(id)).thenReturn(sale);

        service.deleteSaleLineById(lineId);
        service.deleteSalePrevente(id);

        verify(salesManager).deleteSaleLineById(line);
        verify(lineService).deleteSaleLine(any(SalesLine.class));
        verify(repository).delete(sale);
    }

    @Test
    void cancelsBySavingOriginalAndCounterEntryThenCloningLines() {
        SaleId id = new SaleId(8L, date);
        VenteDepot sale = activeSale(id, new Magasin());
        when(repository.getReferenceById(id)).thenReturn(sale);

        service.cancel(id);

        assertEquals(true, sale.isCanceled());
        ArgumentCaptor<VenteDepot> captor = ArgumentCaptor.forClass(VenteDepot.class);
        verify(repository, times(2)).save(captor.capture());
        assertEquals(true, captor.getAllValues().get(1).isCanceled());
        verify(lineService).cloneSalesLine(eq(sale.getSalesLines()), any(VenteDepot.class), eq(user), eq(3));
    }

    @Test
    void appliesProductDiscountAndSupportsMissingSaleOrDiscount() {
        SaleId id = new SaleId(8L, date);
        VenteDepot sale = activeSale(id, new Magasin());
        RemiseProduit remise = new RemiseProduit();
        remise.setId(2);
        when(repository.findById(id)).thenReturn(Optional.of(sale));
        when(remiseRepository.findById(2)).thenReturn(Optional.of(remise));

        service.processDiscount(new UpdateSaleInfo(id, 2));

        assertSame(remise, sale.getRemise());
        verify(repository).save(sale);

        service.processDiscount(new UpdateSaleInfo(new SaleId(99L, date), 2));
        verify(repository, times(1)).save(any(VenteDepot.class));
    }

    @Test
    void removesDiscountAndChangesDepotOrRejectsUnknownSale() {
        SaleId id = new SaleId(8L, date);
        VenteDepot sale = activeSale(id, new Magasin());
        when(repository.getReferenceById(id)).thenReturn(sale);
        when(repository.findById(id)).thenReturn(Optional.of(sale));

        service.removeRemiseFromSale(id);
        service.changeDepot(id, 44);

        assertEquals(44, sale.getDepot().getId());
        verify(repository, times(2)).save(sale);

        SaleId missing = new SaleId(99L, date);
        when(repository.findById(missing)).thenReturn(Optional.empty());
        assertThrows(GenericError.class, () -> service.changeDepot(missing, 1));
    }

    private VenteDepot activeSale(SaleId id, Magasin depot) {
        VenteDepot sale = new VenteDepot();
        sale.setId(id.getId());
        sale.setSaleDate(id.getSaleDate());
        sale.setDepot(depot);
        sale.setStatut(SalesStatut.ACTIVE);
        sale.setSalesAmount(1_000);
        sale.setNetAmount(1_000);
        sale.setAmountToBePaid(1_000);
        sale.setSalesLines(new HashSet<>(List.of(validLine())));
        return sale;
    }

    private SalesLine validLine() {
        SalesLine line = new SalesLine();
        line.setId(4L);
        Produit produit = new Produit();
        produit.setId(10);
        line.setProduit(produit);
        line.setQuantityRequested(2);
        line.setQuantitySold(2);
        line.setRegularUnitPrice(500);
        line.setNetUnitPrice(500);
        line.setSalesAmount(1_000);
        line.setCostAmount(200);
        line.setDiscountAmount(0);
        line.setTaxValue(0);
        return line;
    }
}


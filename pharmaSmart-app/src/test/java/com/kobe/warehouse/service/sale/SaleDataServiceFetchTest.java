package com.kobe.warehouse.service.sale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.VenteDepot;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.ReceiptPrinterService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.report.SaleInvoiceReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SaleDataServiceFetchTest {

    @Mock private EntityManager entityManager;
    @Mock private SaleInvoiceReportService invoiceService;
    @Mock private SalesLineRepository lineRepository;
    @Mock private ThirdPartySaleLineRepository thirdPartyLineRepository;
    @Mock private ReceiptPrinterService printerService;
    @Mock private SalesRepository salesRepository;
    @Mock private StorageService storageService;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS) private CriteriaBuilder criteriaBuilder;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS) private CriteriaQuery<Sales> criteriaQuery;
    @Mock(answer = org.mockito.Answers.RETURNS_DEEP_STUBS) private Root<Sales> root;
    @Mock private TypedQuery<Sales> typedQuery;

    private SaleDataService service;
    private SaleId id;

    @BeforeEach
    void setUp() {
        id = new SaleId(7L, LocalDate.of(2026, 8, 21));
        service = new SaleDataService(entityManager, invoiceService, lineRepository,
            thirdPartyLineRepository, printerService, salesRepository, storageService);
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(Sales.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(Sales.class)).thenReturn(root);
        when(criteriaQuery.select(root)).thenReturn(criteriaQuery);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
    }

    @Test
    void getsEntityAndMapsCashPurchaseForReadAndEdit() {
        CashSale sale = cashSale();
        when(typedQuery.getSingleResult()).thenReturn(sale);

        assertSame(sale, service.getOne(id));
        assertEquals(7L, service.findOne(id).getId());
        assertTrue(service.fetchPurchaseForEditBy(7L, id.getSaleDate()).isPresent());
        assertEquals(7L, service.fetchPurchaseBy(7L, id.getSaleDate()).getId());
    }

    @Test
    void returnsEmptyOptionalAndThrowsForMissingPurchase() {
        when(typedQuery.getSingleResult()).thenReturn(null);

        assertFalse(service.fetchPurchaseForEditBy(7L, id.getSaleDate()).isPresent());
        assertThrows(RuntimeException.class, () -> service.fetchPurchaseBy(7L, id.getSaleDate()));
    }

    @Test
    void printsAndGeneratesCashReceiptAndInvoice() throws Exception {
        CashSale sale = cashSale();
        byte[] receipt = {4, 5};
        byte[] invoice = {8, 9};
        when(typedQuery.getSingleResult()).thenReturn(sale);
        when(printerService.generateEscPosReceipt(any(CashSaleDTO.class), eq(true))).thenReturn(receipt);
        when(invoiceService.printInvoice(any())).thenReturn(invoice);

        service.printReceipt(id, false);
        assertArrayEquals(receipt, service.generateEscPosReceipt(id, true));
        assertArrayEquals(invoice, service.printInvoice(id));

        verify(printerService).printCashSale(any(CashSaleDTO.class), eq(false));
    }

    @Test
    void unknownSalesSubtypeProducesEmptyReceipt() throws Exception {
        Sales sale = org.mockito.Mockito.mock(Sales.class);
        when(typedQuery.getSingleResult()).thenReturn(sale);

        service.printReceipt(id, false);
        assertArrayEquals(new byte[0], service.generateEscPosReceipt(id, false));
    }

    @Test
    void fetchesThirdPartySaleWithDetailedInsuranceInformation() {
        ThirdPartySales sale = thirdPartySale();
        ThirdPartySaleLine thirdPartyLine = sale.getThirdPartySaleLines().getFirst();
        when(typedQuery.getSingleResult()).thenReturn(sale);
        when(thirdPartyLineRepository.findAllBySaleIdAndSaleSaleDate(7L, id.getSaleDate()))
            .thenReturn(List.of(thirdPartyLine));

        ThirdPartySaleDTO result = assertInstanceOf(
            ThirdPartySaleDTO.class,
            service.fetchPurchaseBy(7L, id.getSaleDate())
        );

        assertEquals("BON-7", result.getNumBon());
        assertEquals(1, result.getThirdPartySaleLines().size());
        assertEquals(1, result.getTiersPayants().size());
        AssuredCustomerDTO customer = assertInstanceOf(AssuredCustomerDTO.class, result.getCustomer());
        assertEquals("MAT-7", customer.getNum());
        assertEquals(PrioriteTiersPayant.R0, customer.getPriorite());
        assertEquals(80, customer.getTaux());
        assertEquals(1, customer.getTiersPayants().size());
        assertEquals("Mutuelle principale", customer.getTiersPayants().getFirst().getTiersPayantFullName());
    }

    @Test
    void mapsThirdPartySaleWhenListingPreventes() {
        ThirdPartySales sale = thirdPartySale();
        ThirdPartySaleLine thirdPartyLine = sale.getThirdPartySaleLines().getFirst();
        AppUser connectedUser = new AppUser();
        connectedUser.setMagasin(new Magasin());
        when(storageService.getUser()).thenReturn(connectedUser);
        when(typedQuery.getResultList()).thenReturn(List.of(sale));
        when(thirdPartyLineRepository.findAllBySaleIdAndSaleSaleDate(7L, id.getSaleDate()))
            .thenReturn(List.of(thirdPartyLine));

        List<com.kobe.warehouse.service.dto.SaleDTO> result = service.allPreventes(
            null, null, Set.of(SalesStatut.ACTIVE), null, null, false
        );

        ThirdPartySaleDTO dto = assertInstanceOf(ThirdPartySaleDTO.class, result.getFirst());
        assertEquals("BON-7", dto.getNumBon());
        assertEquals("MAT-7", assertInstanceOf(AssuredCustomerDTO.class, dto.getCustomer()).getNum());
        assertEquals(1, dto.getThirdPartySaleLines().size());
    }

    private CashSale cashSale() {
        CashSale sale = new CashSale();
        sale.setId(7L);
        sale.setSaleDate(id.getSaleDate());
        sale.setNumberTransaction("SALE-7");
        sale.setSalesAmount(1_000);
        sale.setDiscountAmount(0);
        sale.setNetAmount(1_000);
        sale.setAmountToBePaid(1_000);
        sale.setRestToPay(0);
        sale.setPayrollAmount(1_000);
        sale.setStatut(SalesStatut.CLOSED);
        sale.setPaymentStatus(PaymentStatus.PAYE);
        sale.setNatureVente(NatureVente.COMPTANT);
        sale.setTypePrescription(TypePrescription.CONSEIL);
        sale.setCreatedAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());
        AppUser user = new AppUser();
        user.setId(1);
        user.setFirstName("Jean");
        user.setLastName("Test");
        user.setMagasin(new Magasin());
        sale.setUser(user);
        sale.setSeller(user);
        sale.setCaissier(user);
        sale.setSalesLines(new HashSet<>());
        sale.setPayments(new HashSet<>());
        return sale;
    }

    private ThirdPartySales thirdPartySale() {
        ThirdPartySales sale = new ThirdPartySales();
        sale.setId(7L);
        sale.setSaleDate(id.getSaleDate());
        sale.setNumberTransaction("VO-7");
        sale.setSalesAmount(1_000);
        sale.setDiscountAmount(0);
        sale.setNetAmount(1_000);
        sale.setAmountToBePaid(200);
        sale.setPartAssure(200);
        sale.setPartTiersPayant(800);
        sale.setRestToPay(0);
        sale.setPayrollAmount(1_000);
        sale.setStatut(SalesStatut.CLOSED);
        sale.setPaymentStatus(PaymentStatus.PAYE);
        sale.setNatureVente(NatureVente.ASSURANCE);
        sale.setTypePrescription(TypePrescription.CONSEIL);
        sale.setCreatedAt(LocalDateTime.now());
        sale.setUpdatedAt(LocalDateTime.now());
        AppUser user = new AppUser();
        user.setId(1);
        user.setFirstName("Jean");
        user.setLastName("Test");
        user.setMagasin(new Magasin());
        sale.setUser(user);
        sale.setSeller(user);
        sale.setCaissier(user);
        sale.setSalesLines(new HashSet<>());
        sale.setPayments(new HashSet<>());

        AssuredCustomer customer = new AssuredCustomer();
        customer.setId(11);
        customer.setFirstName("Alice");
        customer.setLastName("Assurée");
        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setId(21);
        tiersPayant.setName("Mutuelle");
        tiersPayant.setFullName("Mutuelle principale");
        tiersPayant.setPlafondConsoClient(50_000);
        tiersPayant.setPlafondJournalierClient(10_000);
        tiersPayant.setPlafondAbsolu(true);
        ClientTiersPayant client = new ClientTiersPayant();
        client.setId(31);
        client.setNum("MAT-7");
        client.setTaux((short) 80);
        client.setPriorite(PrioriteTiersPayant.R0);
        client.setTiersPayant(tiersPayant);
        client.setAssuredCustomer(customer);
        customer.setClientTiersPayants(Set.of(client));
        sale.setCustomer(customer);
        ThirdPartySaleLine line = new ThirdPartySaleLine();
        line.setId(41L);
        line.setSale(sale);
        line.setClientTiersPayant(client);
        line.setNumBon("BON-7");
        line.setMontant(800);
        line.setTaux((short) 80);
        line.setRepartitions(List.of());
        sale.setThirdPartySaleLines(new java.util.ArrayList<>(List.of(line)));
        return sale;
    }
}


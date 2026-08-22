package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.repository.AvoirClientUtilisationRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.sale.AvoirClientNotificationService;
import com.kobe.warehouse.service.sale.dto.AvoirClientDocumentDTO;
import com.kobe.warehouse.service.sale.dto.CloturerAvoirRequest;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AvoirClientDocumentServiceImplTest {

    @Mock private AvoirClientRepository repository;
    @Mock private SalesLineRepository lineRepository;
    @Mock private ReferenceService referenceService;
    @Mock private StorageService storageService;
    @Mock private StockProduitRepository stockRepository;
    @Mock private AvoirClientNotificationService notificationService;
    @Mock private AppConfigurationService configurationService;
    @Mock private AvoirClientUtilisationRepository utilisationRepository;

    private AvoirClientDocumentServiceImpl service;
    private AppUser user;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(1);
        user.setFirstName("Alice");
        user.setLastName("Martin");
        Magasin magasin = new Magasin();
        magasin.setId(2);
        user.setMagasin(magasin);
        service = new AvoirClientDocumentServiceImpl(
            repository, lineRepository, referenceService, storageService, stockRepository,
            notificationService, configurationService, utilisationRepository
        );
    }

    @Test
    void createsCreditFromSaleLineAndSkipsCompletelyEmptyRequest() {
        SalesLine line = saleLine();
        Customer customer = customer();
        when(configurationService.getDelaiValiditeAvoir()).thenReturn(30);
        when(referenceService.buildNumAvoirClient()).thenReturn("AV-1");
        when(storageService.getUser()).thenReturn(user);

        service.createAvoirsFromSale(line, customer);

        ArgumentCaptor<AvoirClient> captor = ArgumentCaptor.forClass(AvoirClient.class);
        verify(repository).save(captor.capture());
        AvoirClient saved = captor.getValue();
        assertEquals("AV-1", saved.getReference());
        assertEquals(1_000, saved.getMontant());
        assertEquals(LocalDate.now().plusDays(30), saved.getDateExpiration());
        assertSame(customer, saved.getCustomer());

        SalesLine empty = saleLine();
        empty.setQuantityAvoir(0);
        service.createAvoirsFromSale(empty, null);
    }

    @Test
    void cancelsExistingCreditAndIgnoresMissingOne() {
        AvoirClient avoir = avoir();
        when(repository.findBySalesLineId(10L)).thenReturn(Optional.of(avoir));
        when(repository.findBySalesLineId(99L)).thenReturn(Optional.empty());

        service.cancelAvoirsFromSale(10L);
        service.cancelAvoirsFromSale(99L);

        assertEquals(AvoirClientStatut.ANNULE, avoir.getStatut());
        verify(repository).save(avoir);
    }

    @Test
    void linksOpenCreditsToMatchingCommandeAndHandlesEarlyReturns() {
        Commande commande = org.mockito.Mockito.mock(Commande.class);
        OrderLine orderLine = org.mockito.Mockito.mock(OrderLine.class);
        FournisseurProduit supplier = org.mockito.Mockito.mock(FournisseurProduit.class);
        Produit product = new Produit();
        product.setId(100);
        when(commande.getOrderLines()).thenReturn(List.of(orderLine));
        when(orderLine.getFournisseurProduit()).thenReturn(supplier);
        when(supplier.getProduit()).thenReturn(product);
        AvoirClient avoir = avoir();
        when(repository.existsByStatutAndCommandeIsNull(AvoirClientStatut.OUVERT)).thenReturn(true);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
            .thenReturn(List.of(avoir));

        service.linkCommandeToAvoirs(commande);

        assertSame(commande, avoir.getCommande());
        verify(repository).saveAll(List.of(avoir));
    }

    @Test
    void doesNotQueryCreditsWhenNoOpenCreditExists() {
        Commande commande = org.mockito.Mockito.mock(Commande.class);
        when(repository.existsByStatutAndCommandeIsNull(AvoirClientStatut.OUVERT)).thenReturn(false);

        service.linkCommandeToAvoirs(commande);

        verify(commande, never()).getOrderLines();
        verify(repository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class));
    }

    @Test
    void rejectsMissingClosedInsufficientStockAndExcessiveAmount() {
        CloturerAvoirRequest request = new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, "x", 100);
        when(repository.findById(99)).thenReturn(Optional.empty());
        assertThrows(GenericError.class, () -> service.cloturerAvoir(99, request));

        AvoirClient closed = avoir().setStatut(AvoirClientStatut.CLOTURE);
        when(repository.findById(1)).thenReturn(Optional.of(closed));
        assertThrows(GenericError.class, () -> service.cloturerAvoir(1, request));

        AvoirClient open = avoir();
        when(repository.findById(2)).thenReturn(Optional.of(open));
        when(storageService.getUser()).thenReturn(user);
        when(stockRepository.findTotalQuantityByMagasinIdIdAndProduitId(2, 100)).thenReturn(1);
        assertThrows(GenericError.class, () -> service.cloturerAvoir(2, request));

        AvoirClient noProduct = avoir().setProduit(null);
        when(repository.findById(3)).thenReturn(Optional.of(noProduct));
        assertThrows(GenericError.class, () -> service.cloturerAvoir(3,
            new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, "x", 1_001)));
    }

    @Test
    void partiallyUsesCreditWithoutClosingIt() {
        AvoirClient avoir = avoir().setProduit(null);
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(repository.save(avoir)).thenReturn(avoir);

        AvoirClientDocumentDTO result = service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.COMPENSATION_VENTE, "partiel", 300));

        assertEquals(300, result.montantUtilise());
        assertEquals(700, result.montantRestant());
        assertEquals(AvoirClientStatut.OUVERT, result.statut());
        verify(utilisationRepository).save(any());
        verify(lineRepository, never()).save(any());
        verify(notificationService, never()).notifierProduitsDisponibles(any());
    }

    @Test
    void fullyClosesCreditResetsSaleLineAndNotifiesForProductReturn() {
        AvoirClient avoir = avoir();
        avoir.setMontantUtilise(200);
        SalesLine line = avoir.getSalesLine();
        when(repository.findById(1)).thenReturn(Optional.of(avoir));
        when(storageService.getUser()).thenReturn(user);
        when(stockRepository.findTotalQuantityByMagasinIdIdAndProduitId(2, 100)).thenReturn(null);
        avoir.setQuantite(0);
        when(repository.save(avoir)).thenReturn(avoir);

        AvoirClientDocumentDTO result = service.cloturerAvoir(1,
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, "complet", null));

        assertEquals(AvoirClientStatut.CLOTURE, result.statut());
        assertEquals(0, line.getQuantityAvoir());
        assertSame(user, avoir.getClosedBy());
        verify(lineRepository).save(line);
        verify(notificationService).notifierProduitsDisponibles(avoir);
    }

    @Test
    void mapsCustomerListsPagesAndNullableRelationships() {
        AvoirClient complete = avoir();
        complete.setDateExpiration(LocalDate.now().plusDays(3));
        Commande commande = org.mockito.Mockito.mock(Commande.class);
        when(commande.getReceiptReference()).thenReturn("CMD-1");
        complete.setCommande(commande);
        AvoirClient minimal = new AvoirClient()
            .setId(2).setReference("AV-2").setMontant(100).setQuantite(1)
            .setDateExpiration(LocalDate.now().plusDays(8));
        when(repository.findByCustomerIdOrderByCreatedAtDesc(8)).thenReturn(List.of(complete, minimal));
        PageRequest pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(org.springframework.data.jpa.domain.Specification.class),
            org.mockito.ArgumentMatchers.eq(pageable))).thenReturn(new PageImpl<>(List.of(complete)));

        List<AvoirClientDocumentDTO> customerCredits = service.findAllByCustomer(8);

        assertEquals(2, customerCredits.size());
        assertTrue(customerCredits.getFirst().procheExpiration());
        assertEquals("CMD-1", customerCredits.getFirst().commandeReference());
        assertNull(customerCredits.get(1).customerName());
        assertFalse(customerCredits.get(1).procheExpiration());
        assertEquals(1, service.findAll("AV", LocalDate.now().minusDays(1), LocalDate.now(),
            AvoirClientStatut.OUVERT, pageable).getTotalElements());
    }

    private AvoirClient avoir() {
        return new AvoirClient()
            .setId(1)
            .setReference("AV-1")
            .setStatut(AvoirClientStatut.OUVERT)
            .setQuantite(2)
            .setMontant(1_000)
            .setCustomer(customer())
            .setProduit(saleLine().getProduit())
            .setSalesLine(saleLine())
            .setCreatedBy(user)
            .setDateExpiration(LocalDate.now().plusDays(30));
    }

    private Customer customer() {
        UninsuredCustomer customer = new UninsuredCustomer();
        customer.setId(8);
        customer.setFirstName("Jean");
        customer.setLastName(null);
        return customer;
    }

    private SalesLine saleLine() {
        Produit product = new Produit();
        product.setId(100);
        product.setLibelle("Produit test");
        product.setCodeEanLaboratoire("EAN-1");
        Sales sale = new com.kobe.warehouse.domain.CashSale();
        sale.setId(5L);
        sale.setSaleDate(LocalDate.now());
        sale.setNumberTransaction("SALE-5");
        SalesLine line = new SalesLine();
        line.setId(10L);
        line.setSaleDate(sale.getSaleDate());
        line.setSales(sale);
        line.setProduit(product);
        line.setQuantityAvoir(2);
        line.setRegularUnitPrice(500);
        return line;
    }
}


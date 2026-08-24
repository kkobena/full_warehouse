package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.service.sale.dto.AvoirClientDTO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AvoirClientServiceImplTest {

    @Mock
    private SalesLineRepository repository;
    @Mock
    private Specification<SalesLine> baseSpec;
    @Mock
    private Specification<SalesLine> searchSpec;
    @Mock
    private Specification<SalesLine> dateSpec;
    @Mock
    private Specification<SalesLine> withSearchSpec;
    @Mock
    private Specification<SalesLine> finalSpec;

    private AvoirClientServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AvoirClientServiceImpl(repository);
    }

    @Test
    void findAvoirs_AvecTousLesFiltres_MappeLaLigne() {
        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 31);
        Pageable pageable = PageRequest.of(0, 20);
        SalesLine line = buildLine(true, true);
        when(repository.hasAvoir()).thenReturn(baseSpec);
        when(repository.filterBySearchTerm("PARA")).thenReturn(searchSpec);
        when(repository.filterByPeriode(from, to)).thenReturn(dateSpec);
        when(baseSpec.and(searchSpec)).thenReturn(withSearchSpec);
        when(withSearchSpec.and(dateSpec)).thenReturn(finalSpec);
        when(repository.findAll(finalSpec, pageable)).thenReturn(new PageImpl<>(List.of(line)));

        AvoirClientDTO result = service.findAvoirs("PARA", from, to, pageable)
            .getContent().getFirst();

        assertEquals(100L, result.saleId());
        assertEquals(200L, result.salesLineId());
        assertEquals("Alice Kouassi", result.customerName());
        assertEquals("Y.Paul", result.sellerName());
        assertEquals("CIP-FOURNISSEUR", result.codeCip());
        assertEquals(3_600, result.montantAvoir());
        verify(repository).findAll(finalSpec, pageable);
    }

    @Test
    void findAvoirs_SansFiltres_UtiliseLeCodeEanEtAccepteClientEtIdLigneNulls() {
        Pageable pageable = PageRequest.of(1, 10);
        SalesLine line = buildLine(false, false);
        when(repository.hasAvoir()).thenReturn(baseSpec);
        when(repository.findAll(baseSpec, pageable)).thenReturn(new PageImpl<>(List.of(line)));

        AvoirClientDTO result = service.findAvoirs(" ", null, null, pageable)
            .getContent().getFirst();

        assertNull(result.customerName());
        assertNull(result.salesLineId());
        assertEquals("EAN-123", result.codeCip());
        assertEquals(3_600, result.montantAvoir());
        verify(repository).findAll(baseSpec, pageable);
    }

    private SalesLine buildLine(boolean withCustomer, boolean withFournisseur) {
        AppUser seller = new AppUser();
        seller.setFirstName("Paul");
        seller.setLastName("Yao");

        CashSale sale = new CashSale();
        sale.setId(100L);
        sale.setSaleDate(LocalDate.of(2026, 8, 15));
        sale.setNumberTransaction("V-100");
        sale.setSeller(seller);
        if (withCustomer) {
            Customer customer = new Customer();
            customer.setFirstName("Alice");
            customer.setLastName("Kouassi");
            sale.setCustomer(customer);
        }

        Produit produit = new Produit();
        produit.setLibelle("Paracétamol");
        produit.setCodeEanLaboratoire("EAN-123");
        if (withFournisseur) {
            FournisseurProduit fournisseurProduit = new FournisseurProduit();
            fournisseurProduit.setCodeCip("CIP-FOURNISSEUR");
            produit.setFournisseurProduitPrincipal(fournisseurProduit);
        }

        SalesLine line = new SalesLine();
        if (withFournisseur) {
            line.setId(200L);
            line.setSaleDate(sale.getSaleDate());
        }
        line.setSales(sale);
        line.setProduit(produit);
        line.setQuantityAvoir(3);
        line.setRegularUnitPrice(1_500);
        line.setNetUnitPrice(1_200);
        line.setUpdatedAt(LocalDateTime.of(2026, 8, 15, 12, 0));
        return line;
    }
}


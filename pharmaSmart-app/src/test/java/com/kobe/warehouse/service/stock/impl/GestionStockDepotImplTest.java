package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.service.stock.ProduitService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestionStockDepotImpl")
class GestionStockDepotImplTest {

    @Mock
    private ProduitService produitService;

    @Mock
    private SaleDataService saleDataService;

    private GestionStockDepotImpl service;

    @BeforeEach
    void setUp() {
        service = new GestionStockDepotImpl(produitService, saleDataService);
    }

    @Test
    @DisplayName("findAll force le critere depot avant de deleguer au ProduitService")
    void findAllForceLeCritereDepot() {
        ProduitCriteria criteria = new ProduitCriteria();
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProduitDTO> expected = new PageImpl<>(List.of(new ProduitDTO()));
        when(produitService.findAll(criteria, pageable)).thenReturn(expected);

        Page<ProduitDTO> result = service.findAll(criteria, pageable);

        assertThat(result).isSameAs(expected);
        assertThat(criteria.isDepot()).isTrue();
        verify(produitService).findAll(criteria, pageable);
    }

    @Test
    @DisplayName("findAll ecrase un critere depot deja positionne a false")
    void findAllEcraseDepotFalse() {
        ProduitCriteria criteria = new ProduitCriteria().setDepot(false);
        Pageable pageable = Pageable.unpaged();
        when(produitService.findAll(criteria, pageable)).thenReturn(Page.empty());

        service.findAll(criteria, pageable);

        assertThat(criteria.isDepot()).isTrue();
    }

    @Test
    @DisplayName("getVenteDepot reordonne les arguments attendus par le SaleDataService")
    void getVenteDepotDelegue() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        Pageable pageable = PageRequest.of(2, 10);
        Page<DepotExtensionSaleDTO> expected = Page.empty(pageable);
        when(saleDataService.fetchVenteDepot("dolipra", from, to, 3, PaymentStatus.PAYE, 5, pageable)).thenReturn(expected);

        Page<DepotExtensionSaleDTO> result = service.getVenteDepot(PaymentStatus.PAYE, 5, "dolipra", from, to, 3, pageable);

        assertThat(result).isSameAs(expected);
        verify(saleDataService).fetchVenteDepot("dolipra", from, to, 3, PaymentStatus.PAYE, 5, pageable);
    }

    @Test
    @DisplayName("getVenteDepot transmet les filtres optionnels nuls tels quels")
    void getVenteDepotFiltresNuls() {
        Pageable pageable = Pageable.unpaged();
        when(saleDataService.fetchVenteDepot(null, null, null, null, null, null, pageable)).thenReturn(Page.empty());

        service.getVenteDepot(null, null, null, null, null, null, pageable);

        verify(saleDataService).fetchVenteDepot(null, null, null, null, null, null, pageable);
    }
}

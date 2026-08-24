package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThirdPartySaleLineServiceTest {

    @Mock
    private AssuranceItemIdGeneratorService idGeneratorService;
    @Mock
    private ThirdPartySaleLineRepository repository;

    private ThirdPartySaleLineService service;

    @BeforeEach
    void setUp() {
        service = new ThirdPartySaleLineService(idGeneratorService, repository);
    }

    @Test
    void createThirdPartySaleLine_InitialiseTousLesChamps() {
        ClientTiersPayant client = new ClientTiersPayant();
        client.setId(5);
        when(idGeneratorService.nextId()).thenReturn(42L);

        ThirdPartySaleLine result = service.createThirdPartySaleLine("BON-42", client, 3_500);

        assertEquals(42L, result.getId().getId());
        assertEquals("BON-42", result.getNumBon());
        assertSame(client, result.getClientTiersPayant());
        assertEquals(3_500, result.getMontant());
        assertEquals(result.getCreated(), result.getUpdated());
        assertEquals(result.getCreated(), result.getEffectiveUpdateDate());
    }

    @Test
    void operationsRepository_RespectentLaCleComposite() {
        SaleId saleId = new SaleId(15L, LocalDate.of(2026, 8, 20));
        ThirdPartySaleLine line = new ThirdPartySaleLine();
        List<ThirdPartySaleLine> lines = List.of(line);
        when(repository.findAllBySaleIdAndSaleSaleDate(15L, saleId.getSaleDate()))
            .thenReturn(lines);
        when(repository.findFirstByClientTiersPayantIdAndSaleIdAndSaleSaleDate(
            8, 15L, saleId.getSaleDate())).thenReturn(Optional.of(line));
        when(repository.save(line)).thenReturn(line);

        assertSame(lines, service.findAllBySaleId(saleId));
        assertSame(line, service.findFirstByClientTiersPayantIdAndSaleId(8, saleId).orElseThrow());
        assertSame(line, service.save(line));
        service.saveAll(lines);
        service.deleteAll(lines);
        service.delete(line);

        verify(repository).saveAll(lines);
        verify(repository).deleteAll(lines);
        verify(repository).delete(line);
    }

    @Test
    void compteNumeroBon_AvecEtSansVente_UtiliseUneFenetreDeTroisMois() {
        LocalDate dateMinAvant = LocalDate.now().minusMonths(3);
        when(repository.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
            "BON", 4, SalesStatut.CLOSED, dateMinAvant)).thenReturn(2L);
        when(repository.countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
            "BON", 10L, 4, SalesStatut.CLOSED, dateMinAvant)).thenReturn(1L);

        assertEquals(2L, service.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
            "BON", 4, SalesStatut.CLOSED));
        assertEquals(1L, service.countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
            "BON", 10L, 4, SalesStatut.CLOSED));
        assertTrue(!LocalDate.now().minusMonths(3).isBefore(dateMinAvant));
    }
}


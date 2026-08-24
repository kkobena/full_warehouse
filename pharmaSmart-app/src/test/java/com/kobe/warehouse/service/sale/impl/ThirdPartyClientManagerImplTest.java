package com.kobe.warehouse.service.sale.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.errors.NumBonAlreadyUseException;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import com.kobe.warehouse.service.sale.ThirdPartyCalculationManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThirdPartyClientManagerImplTest {

    @Mock private ThirdPartySaleLineService lineService;
    @Mock private ClientTiersPayantRepository clientRepository;
    @Mock private TiersPayantRepository tiersPayantRepository;
    @Mock private ThirdPartySaleRepository saleRepository;
    @Mock private ConsommationService consommationService;
    @Mock private StorageService storageService;
    @Mock private AssuranceItemIdGeneratorService idGeneratorService;
    @Mock private ThirdPartyCalculationManager calculationManager;

    private ThirdPartyClientManagerImpl manager;
    private ThirdPartySales sale;
    private ClientTiersPayant client;
    private ThirdPartySaleLine line;
    private SaleId saleId;

    @BeforeEach
    void setUp() {
        manager = new ThirdPartyClientManagerImpl(lineService, clientRepository,
            tiersPayantRepository, saleRepository, consommationService, storageService,
            idGeneratorService, calculationManager);
        saleId = new SaleId(10L, LocalDate.of(2026, 8, 20));
        sale = new ThirdPartySales();
        sale.setId(saleId.getId());
        sale.setSaleDate(saleId.getSaleDate());
        sale.setThirdPartySaleLines(new ArrayList<>());
        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setId(30);
        tiersPayant.setFullName("Mutuelle");
        client = new ClientTiersPayant();
        client.setId(20);
        client.setTiersPayant(tiersPayant);
        client.setTaux(80);
        client.setPriorite(PrioriteTiersPayant.R0);
        line = new ThirdPartySaleLine();
        line.setId(50L);
        line.setSaleDate(saleId.getSaleDate());
        line.setClientTiersPayant(client);
        line.setSale(sale);
        line.setMontant(800);
        line.setTauxVente((short) 80);
    }

    @Test
    void saveTiersPayantLines_SansDto_Refuse() {
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setTiersPayants(new ArrayList<>());
        assertThrows(GenericError.class, () -> manager.saveTiersPayantLines(dto, sale));
    }

    @Test
    void saveTiersPayantLines_ClientsIntrouvables_Refuse() {
        ThirdPartySaleDTO dto = dtoTiersPayant("BON-1", 75);
        when(clientRepository.findAllByIdIn(Set.of(20))).thenReturn(List.of());
        assertThrows(GenericError.class, () -> manager.saveTiersPayantLines(dto, sale));
    }

    @Test
    void saveTiersPayantLines_CreeLesLignesEtLanceLeCalcul() {
        ThirdPartySaleDTO dto = dtoTiersPayant("BON-1", 75);
        when(clientRepository.findAllByIdIn(Set.of(20))).thenReturn(List.of(client));
        when(lineService.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
            "BON-1", 20, SalesStatut.CLOSED)).thenReturn(0L);
        when(lineService.createThirdPartySaleLine("BON-1", client, 0)).thenReturn(line);
        when(calculationManager.upddateThirdPartySaleAmounts(eq(sale), eq(true), anyList()))
            .thenReturn("alerte");

        assertEquals("alerte", manager.saveTiersPayantLines(dto, sale));
        assertTrue(sale.getThirdPartySaleLines().contains(line));
        assertEquals(75, line.getTauxVente());
    }

    @Test
    void saveTiersPayantLines_NumeroBonDejaUtilise_Refuse() {
        ThirdPartySaleDTO dto = dtoTiersPayant("BON-1", 80);
        when(clientRepository.findAllByIdIn(Set.of(20))).thenReturn(List.of(client));
        when(lineService.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
            "BON-1", 20, SalesStatut.CLOSED)).thenReturn(1L);
        assertThrows(NumBonAlreadyUseException.class,
            () -> manager.saveTiersPayantLines(dto, sale));
        verify(calculationManager, never()).upddateThirdPartySaleAmounts(any(), eq(true), anyList());
    }

    @Test
    void saveTiersPayantLines_RefuseClientSansDtoCorrespondant() {
        ThirdPartySaleDTO dto = dtoTiersPayant("BON-1", 80);
        ClientTiersPayant unrelated = new ClientTiersPayant();
        unrelated.setId(21);
        when(clientRepository.findAllByIdIn(Set.of(20))).thenReturn(List.of(unrelated));

        assertThrows(GenericError.class, () -> manager.saveTiersPayantLines(dto, sale));
    }

    @Test
    void checkIfNumBonIsAlReadyUse_GereVideCreationEtEdition() {
        assertTrue(!manager.checkIfNumBonIsAlReadyUse("", 20, null));
        when(lineService.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
            "BON", 20, SalesStatut.CLOSED)).thenReturn(1L);
        when(lineService.countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
            "BON", 10L, 20, SalesStatut.CLOSED)).thenReturn(0L);
        assertTrue(manager.checkIfNumBonIsAlReadyUse("BON", 20, null));
        assertTrue(!manager.checkIfNumBonIsAlReadyUse("BON", 20, 10L));
    }

    @Test
    void addEtRemoveThirdPartySaleLine_RecalculentLaVente() {
        ClientTiersPayantDTO dto = dtoTiersPayant("BON-2", 70).getTiersPayants().getFirst();
        when(clientRepository.getReferenceById(20)).thenReturn(client);
        when(saleRepository.findOneById(10L)).thenReturn(sale);
        when(lineService.createThirdPartySaleLine("BON-2", client, 0)).thenReturn(line);
        when(calculationManager.reComputeAndApplyAmounts(sale, null, true)).thenReturn("ok");

        assertEquals("ok", manager.addThirdPartySaleLineToSales(dto, saleId));
        assertTrue(sale.getThirdPartySaleLines().contains(line));
        verify(lineService).save(line);

        when(lineService.findFirstByClientTiersPayantIdAndSaleId(20, saleId))
            .thenReturn(Optional.of(line));
        assertEquals("ok", manager.removeThirdPartySaleLineToSales(20, saleId));
        assertNull(line.getSale());
        verify(lineService).delete(line);
    }

    @Test
    void addThirdPartySaleLine_RefuseNumeroBonDejaUtilise() {
        ClientTiersPayantDTO dto = dtoTiersPayant("BON-DUP", 70).getTiersPayants().getFirst();
        when(clientRepository.getReferenceById(20)).thenReturn(client);
        when(saleRepository.findOneById(10L)).thenReturn(sale);
        when(lineService.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
            "BON-DUP", 20, SalesStatut.CLOSED)).thenReturn(1L);

        assertThrows(NumBonAlreadyUseException.class,
            () -> manager.addThirdPartySaleLineToSales(dto, saleId));
        verify(lineService, never()).createThirdPartySaleLine(any(), any(), any(Integer.class));
    }

    @Test
    void updatesClientAndTiersPayantConsumptionAccounts() {
        AppUser user = new AppUser();
        line.setUpdated(LocalDateTime.of(2026, 8, 20, 12, 0));
        when(storageService.getUser()).thenReturn(user);

        manager.updateClientTiersPayantAccount(line);
        manager.updateTiersPayantAccount(line);

        assertSame(user, client.getTiersPayant().getUser());
        verify(consommationService).updateConsommation(eq(client), eq(800), eq(line.getUpdated()), any());
        verify(consommationService).updateConsommation(eq(client.getTiersPayant()), eq(800), eq(line.getUpdated()), any());
    }

    @Test
    void removeThirdPartySaleLine_Absente_NeRecalculePas() {
        when(lineService.findFirstByClientTiersPayantIdAndSaleId(20, saleId))
            .thenReturn(Optional.empty());
        assertNull(manager.removeThirdPartySaleLineToSales(20, saleId));
        verify(calculationManager, never()).reComputeAndApplyAmounts(any(), any(), eq(true));
    }

    @Test
    void updateTiersPayantTaux_PresentEtAbsent() {
        when(lineService.findFirstByClientTiersPayantIdAndSaleId(20, saleId))
            .thenReturn(Optional.of(line));
        when(calculationManager.reComputeAndApplyAmounts(sale, null, true)).thenReturn("warning");

        var result = manager.updateTiersPayantTaux(20, saleId, 65);

        assertSame(sale, result.sale());
        assertEquals("warning", result.message());
        assertEquals(65, client.getTaux());
        assertEquals(65, line.getTauxVente());
        verify(clientRepository).save(client);
        verify(lineService).save(line);

        when(lineService.findFirstByClientTiersPayantIdAndSaleId(99, saleId))
            .thenReturn(Optional.empty());
        assertNull(manager.updateTiersPayantTaux(99, saleId, 50).sale());
    }

    @Test
    void clone_UnitaireCreeUneContrepassationEtSupprimeLOriginal() {
        ThirdPartySales copy = new ThirdPartySales();
        copy.setSaleDate(LocalDate.of(2026, 8, 21));
        when(idGeneratorService.nextId()).thenReturn(99L);

        ThirdPartySaleLine clone = manager.clone(line, copy);

        assertEquals(99L, clone.getId().getId());
        assertEquals(-800, clone.getMontant());
        assertEquals(ThirdPartySaleStatut.DELETE, clone.getStatut());
        assertSame(copy, clone.getSale());
        assertEquals(ThirdPartySaleStatut.DELETE, line.getStatut());
        verify(lineService).save(clone);
        verify(lineService).save(line);
    }

    @Test
    void cloneListeEtSaveAll_GerentListesVidesEtRemplies() {
        ThirdPartySales copy = new ThirdPartySales();
        copy.setSaleDate(LocalDate.of(2026, 8, 21));
        when(idGeneratorService.nextId()).thenReturn(99L);

        assertTrue(manager.clone(List.of(), copy).isEmpty());
        List<ThirdPartySaleLine> clones = manager.clone(List.of(line), copy);
        assertEquals(1, clones.size());
        assertSame(copy, clones.getFirst().getSale());
        assertEquals(copy.getSaleDate(), clones.getFirst().getSaleDate());
        manager.saveAll(List.of());
        manager.saveAll(clones);
        verify(lineService).saveAll(clones);
    }

    @Test
    void updateThirdPartySaleLine_ChangeClientBonEtMontant() {
        ClientTiersPayant replacement = new ClientTiersPayant();
        replacement.setId(21);
        when(clientRepository.getReferenceById(21)).thenReturn(replacement);
        when(lineService.countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
            "NOUVEAU", 10L, 21, SalesStatut.CLOSED)).thenReturn(0L);

        manager.updateThirdPartySaleLine("NOUVEAU", line, 21, 900);

        assertSame(replacement, line.getClientTiersPayant());
        assertEquals("NOUVEAU", line.getNumBon());
        assertEquals(900, line.getMontant());
        verify(lineService).save(line);
    }

    @Test
    void updateThirdPartySaleLine_RefuseNumeroBonDuplique() {
        when(lineService.countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
            "DUP", 10L, 20, SalesStatut.CLOSED)).thenReturn(1L);

        assertThrows(NumBonAlreadyUseException.class,
            () -> manager.updateThirdPartySaleLine("DUP", line, 20, null));
        verify(lineService, never()).save(line);
    }

    @Test
    void findAllEtFindSaleLine_DeleguentEtFiltrent() {
        when(lineService.findAllBySaleId(saleId)).thenReturn(List.of(line));
        sale.getThirdPartySaleLines().add(line);
        assertEquals(List.of(line), manager.findAllBySaleId(saleId));
        assertSame(line, manager.findSaleLineByClientTiersPayantId(sale, 20).orElseThrow());
        assertTrue(manager.findSaleLineByClientTiersPayantId(sale, 99).isEmpty());
    }

    @Test
    void saveTiersPayantLinesOnChangeCustomer_TrieEtRecalcule() {
        AssuredCustomer customer = new AssuredCustomer();
        ClientTiersPayant secondary = new ClientTiersPayant();
        secondary.setId(21);
        secondary.setTaux(20);
        secondary.setPriorite(PrioriteTiersPayant.R1);
        customer.setClientTiersPayants(Set.of(secondary, client));
        sale.setCustomer(customer);
        when(lineService.createThirdPartySaleLine(null, client, 0)).thenReturn(line);
        ThirdPartySaleLine secondaryLine = new ThirdPartySaleLine();
        when(lineService.createThirdPartySaleLine(null, secondary, 0)).thenReturn(secondaryLine);
        when(calculationManager.reComputeAndApplyAmounts(eq(sale), anyList(), eq(true)))
            .thenReturn(null);

        assertNull(manager.saveTiersPayantLinesOnChangeCustomer(sale));
        assertTrue(sale.getThirdPartySaleLines().contains(line));
        assertTrue(sale.getThirdPartySaleLines().contains(secondaryLine));
    }

    private ThirdPartySaleDTO dtoTiersPayant(String numBon, int taux) {
        ClientTiersPayantDTO tiersPayant = new ClientTiersPayantDTO();
        tiersPayant.setId(20);
        tiersPayant.setNumBon(numBon);
        tiersPayant.setTaux(taux);
        ThirdPartySaleDTO dto = new ThirdPartySaleDTO();
        dto.setTiersPayants(new ArrayList<>(List.of(tiersPayant)));
        return dto;
    }
}



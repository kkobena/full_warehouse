package com.kobe.warehouse.service.inventaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.HistoriqueInventaire;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.dto.records.ItemsCountRecord;
import com.kobe.warehouse.service.errors.InventoryException;
import com.kobe.warehouse.service.historique_inventaire.HistoriqueInventaireService;
import com.kobe.warehouse.service.inventaire.impl.InventoryCloseServiceImpl;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.persistence.Tuple;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

/**
 * La clôture est le point de non-retour de l'inventaire : elle écrit le stock, journalise les
 * mouvements et fige la valorisation dans l'entête. Deux garde-fous en dépendent — refuser de
 * clôturer tant qu'une ligne n'est pas comptée, et ne rien refaire sur un inventaire déjà clos —
 * puis, une fois passés, l'entête doit porter la valorisation calculée et l'événement de réassort
 * doit partir.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryCloseService — clôture d'un inventaire")
class InventoryCloseServiceImplTest {

    private static final long INVENTAIRE = 7L;

    @Mock
    private StoreInventoryLineRepository storeInventoryLineRepository;

    @Mock
    private StoreInventoryRepository storeInventoryRepository;

    @Mock
    private HistoriqueInventaireService historiqueInventaireService;

    @Mock
    private AppConfigurationService appConfigurationService;

    @Mock
    private EntityManager em;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private InventoryCloseService service;
    private StoreInventory inventaire;

    @BeforeEach
    void init() {
        service = new InventoryCloseServiceImpl(
            storeInventoryLineRepository,
            storeInventoryRepository,
            historiqueInventaireService,
            appConfigurationService,
            em,
            eventPublisher
        );
        inventaire = inventaire(InventoryStatut.CREATE);
    }

    @Test
    @DisplayName("Un inventaire déjà clos n'est pas reclôturé")
    void inventaireDejaClos() {
        when(storeInventoryRepository.getReferenceById(INVENTAIRE))
            .thenReturn(inventaire(InventoryStatut.CLOSED));

        ItemsCountRecord resultat = service.close(INVENTAIRE);

        assertEquals(0, resultat.count());
        verifyNoInteractions(em, eventPublisher, historiqueInventaireService);
        verify(storeInventoryLineRepository, never())
            .countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(anyLong());
    }

    @Test
    @DisplayName("Une seule ligne non comptée suffit à refuser la clôture")
    void ligneNonComptee() {
        when(storeInventoryRepository.getReferenceById(INVENTAIRE)).thenReturn(inventaire);
        when(storeInventoryLineRepository.countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(INVENTAIRE))
            .thenReturn(1L);

        assertThrows(InventoryException.class, () -> service.close(INVENTAIRE));

        assertEquals(InventoryStatut.CREATE, inventaire.getStatut(), "le statut n'a pas bougé");
        verifyNoInteractions(em, eventPublisher, historiqueInventaireService);
    }

    @Test
    @DisplayName("La clôture fige la valorisation dans l'entête et publie l'événement de réassort")
    void clotureNominale() {
        preparerLaCloture(12);

        ItemsCountRecord resultat = service.close(INVENTAIRE);

        assertEquals(12, resultat.count(), "le nombre de lignes vient de la procédure stockée");
        assertEquals(InventoryStatut.CLOSED, inventaire.getStatut());
        assertEquals(3_000L, inventaire.getInventoryValueCostBegin());
        assertEquals(2_800L, inventaire.getInventoryValueCostAfter());
        assertEquals(5_000L, inventaire.getInventoryAmountBegin());
        assertEquals(4_600L, inventaire.getInventoryAmountAfter());
        assertEquals(-200, inventaire.getGapCost());
        assertEquals(-400, inventaire.getGapAmount());

        verify(storeInventoryRepository).save(inventaire);
        ArgumentCaptor<HistoriqueInventaire> historique = ArgumentCaptor.forClass(HistoriqueInventaire.class);
        verify(historiqueInventaireService).save(historique.capture());
        assertEquals(INVENTAIRE, historique.getValue().getId().longValue());

        ArgumentCaptor<InventoryClosedEvent> evenement = ArgumentCaptor.forClass(InventoryClosedEvent.class);
        verify(eventPublisher).publishEvent(evenement.capture());
        InventoryClosedEvent publie = evenement.getValue();
        assertEquals(INVENTAIRE, publie.storeInventoryId().longValue());
        assertEquals(InventoryCategory.MAGASIN, publie.inventoryCategory());
        assertEquals(StorageType.PRINCIPAL, publie.storageType());
        assertEquals(1, publie.storageId().intValue());
        assertEquals(5, publie.magasinId().intValue());
        assertEquals(3, publie.userId().intValue());
    }

    @Test
    @DisplayName("Une valorisation vide est ramenée à zéro, pas à null")
    void valorisationVide() {
        preparerLaCloture(0);
        when(tuple.get(anyString())).thenReturn(null);

        service.close(INVENTAIRE);

        assertEquals(0L, inventaire.getInventoryValueCostBegin());
        assertEquals(0, inventaire.getGapCost());
    }

    @Test
    @DisplayName("Le mode de gestion de lot est transmis à la procédure de clôture")
    void modeGestionLotTransmis() {
        preparerLaCloture(4);
        when(appConfigurationService.useGestionLotInventaire()).thenReturn(true);

        service.close(INVENTAIRE);

        verify(procedure).setParameter("p_gestion_lot", true);
        verify(procedure).setParameter("p_store_inventory_id", INVENTAIRE);
    }

    // ===== jeu d'essai =====

    private Tuple tuple;
    private StoredProcedureQuery procedure;

    /** Branche l'{@code EntityManager} sur un résumé de valorisation et une procédure de clôture. */
    private void preparerLaCloture(int lignesTraitees) {
        tuple = Mockito.mock(Tuple.class);
        lenient().when(tuple.get("costValueBegin")).thenReturn(BigDecimal.valueOf(3_000));
        lenient().when(tuple.get("costValueAfter")).thenReturn(BigDecimal.valueOf(2_800));
        lenient().when(tuple.get("amountValueBegin")).thenReturn(BigDecimal.valueOf(5_000));
        lenient().when(tuple.get("amountValueAfter")).thenReturn(BigDecimal.valueOf(4_600));
        lenient().when(tuple.get("gapCost")).thenReturn(BigDecimal.valueOf(-200));
        lenient().when(tuple.get("gapAmount")).thenReturn(BigDecimal.valueOf(-400));

        Query resume = Mockito.mock(Query.class, Answers.RETURNS_SELF);
        lenient().when(resume.getSingleResult()).thenReturn(tuple);

        procedure = Mockito.mock(StoredProcedureQuery.class, Answers.RETURNS_SELF);
        lenient().when(procedure.getOutputParameterValue("p_nombre_ligne")).thenReturn(lignesTraitees);

        when(storeInventoryRepository.getReferenceById(INVENTAIRE)).thenReturn(inventaire);
        when(storeInventoryRepository.save(inventaire)).thenReturn(inventaire);
        when(storeInventoryLineRepository.countStoreInventoryLineByUpdatedIsFalseAndStoreInventoryId(INVENTAIRE))
            .thenReturn(0L);
        when(em.createNativeQuery(anyString(), eq(Tuple.class))).thenReturn(resume);
        when(em.createStoredProcedureQuery("proc_close_inventory_v2")).thenReturn(procedure);
    }

    private StoreInventory inventaire(InventoryStatut statut) {
        Magasin magasin = new Magasin();
        magasin.setId(5);
        Storage storage = new Storage();
        storage.setId(1);
        storage.setMagasin(magasin);
        storage.setStorageType(StorageType.PRINCIPAL);
        AppUser user = new AppUser();
        user.setId(3);

        StoreInventory inventaire = new StoreInventory();
        inventaire.setId(INVENTAIRE);
        inventaire.setStatut(statut);
        inventaire.setStorage(storage);
        inventaire.setUser(user);
        inventaire.setInventoryCategory(InventoryCategory.MAGASIN);
        inventaire.setCreatedAt(LocalDateTime.now());
        inventaire.setUpdatedAt(LocalDateTime.now());
        inventaire.setInventoryValueCostBegin(0L);
        inventaire.setInventoryAmountBegin(0L);
        inventaire.setInventoryValueCostAfter(0L);
        inventaire.setInventoryAmountAfter(0L);
        inventaire.setGapCost(0);
        inventaire.setGapAmount(0);
        return inventaire;
    }
}

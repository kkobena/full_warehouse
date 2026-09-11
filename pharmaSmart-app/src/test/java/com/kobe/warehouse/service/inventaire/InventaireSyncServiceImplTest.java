package com.kobe.warehouse.service.inventaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.records.BatchSyncResultRecord;
import com.kobe.warehouse.service.inventaire.impl.InventaireSyncServiceImpl;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La synchronisation batch est le chemin du comptage hors ligne : le terminal mobile renvoie
 * d'un coup tout ce qu'il a saisi. Ce qui compte ici, c'est qu'une ligne fautive — disparue,
 * ou recomptée entre-temps par un collègue — n'emporte pas le reste du lot, et que le compte
 * rendu distingue les deux : un échec technique se rejoue, un conflit de comptage s'arbitre.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventaireSyncService — synchronisation batch des comptages")
class InventaireSyncServiceImplTest {

    @Mock
    private StoreInventoryLineRepository storeInventoryLineRepository;

    @Mock
    private UserService userService;

    @Captor
    private ArgumentCaptor<List<StoreInventoryLine>> sauvegardees;

    private InventaireSyncService service;
    private AppUser operateur;

    @BeforeEach
    void init() {
        service = new InventaireSyncServiceImpl(storeInventoryLineRepository, userService);
        operateur = new AppUser();
        operateur.setId(4);
    }

    @Test
    @DisplayName("Un lot vide ne touche pas la base")
    void lotVide() {
        BatchSyncResultRecord resultat = service.synchronize(List.of());

        assertEquals(0, resultat.saved());
        assertEquals(0, resultat.failed());
        assertTrue(resultat.failedIds().isEmpty());
        verifyNoInteractions(storeInventoryLineRepository, userService);
    }

    @Test
    @DisplayName("Un lot nul est traité comme un lot vide")
    void lotNul() {
        assertEquals(0, service.synchronize(null).saved());
        verifyNoInteractions(storeInventoryLineRepository, userService);
    }

    @Test
    @DisplayName("Les lignes comptées sont enregistrées en une seule fois, avec écart et traçabilité")
    void comptageNominal() {
        StoreInventoryLine premiere = ligne(1L, 0L);
        StoreInventoryLine seconde = ligne(2L, 0L);
        when(userService.getUser()).thenReturn(operateur);
        when(storeInventoryLineRepository.findAllById(java.util.Set.of(1L, 2L)))
            .thenReturn(List.of(premiere, seconde));

        BatchSyncResultRecord resultat = service.synchronize(
            List.of(dto(1L, 0L, 100, 97), dto(2L, 0L, 50, 50)));

        assertEquals(2, resultat.saved());
        assertEquals(0, resultat.failed());
        verify(storeInventoryLineRepository).saveAll(sauvegardees.capture());
        assertEquals(2, sauvegardees.getValue().size(), "un seul aller-retour pour tout le lot");

        assertEquals(100, premiere.getQuantityInit());
        assertEquals(97, premiere.getQuantityOnHand());
        assertEquals(-3, premiere.getGap());
        assertTrue(premiere.getUpdated());
        assertSame(operateur, premiere.getCountedBy(), "l'utilisateur est résolu une fois pour tout le lot");
        assertEquals(0, seconde.getGap());
    }

    @Test
    @DisplayName("Une ligne disparue est signalée en échec sans interrompre les autres")
    void ligneIntrouvable() {
        StoreInventoryLine survivante = ligne(2L, 0L);
        when(userService.getUser()).thenReturn(operateur);
        when(storeInventoryLineRepository.findAllById(java.util.Set.of(1L, 2L)))
            .thenReturn(List.of(survivante));

        BatchSyncResultRecord resultat = service.synchronize(
            List.of(dto(1L, 0L, 10, 9), dto(2L, 0L, 20, 20)));

        assertEquals(1, resultat.saved());
        assertEquals(1, resultat.failed());
        assertEquals(List.of(1L), resultat.failedIds());
        assertTrue(resultat.conflictedIds().isEmpty(), "une ligne absente n'est pas un conflit de comptage");
        assertTrue(survivante.getUpdated(), "la ligne valide du lot est bien comptée");
    }

    @Test
    @DisplayName("Une ligne recomptée entre-temps est rejetée comme conflit, pas comme échec")
    void conflitDeComptage() {
        StoreInventoryLine enConflit = ligne(1L, 7L);
        StoreInventoryLine paisible = ligne(2L, 3L);
        when(userService.getUser()).thenReturn(operateur);
        when(storeInventoryLineRepository.findAllById(java.util.Set.of(1L, 2L)))
            .thenReturn(List.of(enConflit, paisible));

        BatchSyncResultRecord resultat = service.synchronize(
            List.of(dto(1L, 5L, 10, 8), dto(2L, 3L, 20, 21)));

        assertEquals(1, resultat.saved());
        assertEquals(1, resultat.failed(), "le total des rejets comprend les conflits");
        assertEquals(List.of(1L), resultat.conflictedIds());
        assertTrue(resultat.failedIds().isEmpty());
        assertNull(enConflit.getQuantityOnHand(), "la saisie périmée n'est pas appliquée");
        assertEquals(21, paisible.getQuantityOnHand(), "la version concordante passe");
    }

    @Test
    @DisplayName("Un client sans numéro de version n'est pas soumis au contrôle de concurrence")
    void appelantSansVersion() {
        StoreInventoryLine ligne = ligne(1L, 12L);
        when(userService.getUser()).thenReturn(operateur);
        when(storeInventoryLineRepository.findAllById(java.util.Set.of(1L))).thenReturn(List.of(ligne));

        BatchSyncResultRecord resultat = service.synchronize(List.of(dto(1L, null, 10, 4)));

        assertEquals(1, resultat.saved());
        assertEquals(4, ligne.getQuantityOnHand());
    }

    @Test
    @DisplayName("Une ligne sans identifiant est ignorée sans être comptée en échec")
    void ligneSansIdentifiant() {
        when(userService.getUser()).thenReturn(operateur);
        when(storeInventoryLineRepository.findAllById(java.util.Set.of())).thenReturn(List.of());

        BatchSyncResultRecord resultat = service.synchronize(List.of(dto(null, null, 10, 5)));

        assertEquals(0, resultat.saved());
        assertEquals(0, resultat.failed());
        verify(storeInventoryLineRepository).saveAll(sauvegardees.capture());
        assertTrue(sauvegardees.getValue().isEmpty());
    }

    @Test
    @DisplayName("Des quantités absentes valent zéro plutôt que d'échouer")
    void quantitesAbsentes() {
        StoreInventoryLine ligne = ligne(1L, 0L);
        when(userService.getUser()).thenReturn(operateur);
        when(storeInventoryLineRepository.findAllById(java.util.Set.of(1L))).thenReturn(List.of(ligne));

        service.synchronize(List.of(dto(1L, 0L, null, null)));

        assertEquals(0, ligne.getQuantityInit());
        assertEquals(0, ligne.getQuantityOnHand());
        assertEquals(0, ligne.getGap());
    }

    @Test
    @DisplayName("Les identifiants en double ne sont chargés qu'une fois")
    void identifiantsEnDouble() {
        StoreInventoryLine ligne = ligne(1L, 0L);
        when(userService.getUser()).thenReturn(operateur);
        when(storeInventoryLineRepository.findAllById(java.util.Set.of(1L))).thenReturn(List.of(ligne));

        BatchSyncResultRecord resultat = service.synchronize(
            Arrays.asList(dto(1L, 0L, 10, 8), dto(1L, 0L, 10, 9)));

        assertEquals(2, resultat.saved(), "les deux saisies sont appliquées à la même ligne");
        assertEquals(9, ligne.getQuantityOnHand(), "la dernière saisie l'emporte");
    }

    // ===== jeu d'essai =====

    private StoreInventoryLine ligne(Long id, Long version) {
        Produit produit = new Produit();
        produit.setId(id != null ? id.intValue() : 0);
        produit.setCostAmount(600);
        produit.setRegularUnitPrice(1_000);

        StoreInventoryLine ligne = new StoreInventoryLine();
        ligne.setId(id);
        ligne.setVersion(version);
        ligne.setProduit(produit);
        return ligne;
    }

    private StoreInventoryLineDTO dto(Long id, Long version, Integer quantiteInitiale, Integer quantiteComptee) {
        StoreInventoryLineDTO dto = new StoreInventoryLineDTO();
        dto.setId(id);
        dto.setVersion(version);
        dto.setQuantityInit(quantiteInitiale);
        dto.setQuantityOnHand(quantiteComptee);
        return dto;
    }
}

package com.kobe.warehouse.service.inventaire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.InventoryLot;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.repository.InventoryLotRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.records.InventoryLotRecord;
import com.kobe.warehouse.service.inventaire.impl.InventoryLotServiceImpl;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * En gestion de lot, la ligne produit n'est plus saisie directement : elle vaut la somme de ses
 * lots. Chaque écriture sur un lot doit donc reporter ce total sur la ligne parente, faute de
 * quoi la grille produit et la grille lot affichent deux chiffres différents pour le même
 * comptage, et la clôture s'appuie sur le mauvais.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryLotService — comptage par lot et report sur la ligne produit")
class InventoryLotServiceImplTest {

    private static final long LIGNE = 500L;

    @Mock
    private InventoryLotRepository inventoryLotRepository;

    @Mock
    private StoreInventoryLineRepository storeInventoryLineRepository;

    @Mock
    private StoreInventoryRepository storeInventoryRepository;

    @Mock
    private LotRepository lotRepository;

    @Mock
    private UserService userService;

    @Mock
    private InventoryStockService inventoryStockService;

    @Mock
    private EntityManager em;

    private InventoryLotService service;
    private StoreInventoryLine ligne;
    private Produit produit;
    private AppUser operateur;

    @BeforeEach
    void init() {
        service = new InventoryLotServiceImpl(
            inventoryLotRepository,
            storeInventoryLineRepository,
            storeInventoryRepository,
            lotRepository,
            userService,
            inventoryStockService,
            em
        );
        produit = new Produit();
        produit.setId(77);
        produit.setCostAmount(600);
        produit.setRegularUnitPrice(1_000);
        ligne = new StoreInventoryLine();
        ligne.setId(LIGNE);
        ligne.setProduit(produit);
        ligne.setQuantityInit(30);
        operateur = new AppUser();
        operateur.setId(3);
    }

    @Test
    @DisplayName("Compter un lot existant fige l'écart et reporte le total sur la ligne produit")
    void comptageDUnLotExistant() {
        Lot lot = lot(11, "LOT-A", 20);
        when(storeInventoryLineRepository.getReferenceById(LIGNE)).thenReturn(ligne);
        when(lotRepository.getReferenceById(11)).thenReturn(lot);
        when(inventoryLotRepository.saveAndFlush(any(InventoryLot.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryLotRepository.findAllByStoreInventoryLineId(LIGNE))
            .thenReturn(List.of(inventoryLot(18), inventoryLot(4)));
        when(userService.getUser()).thenReturn(operateur);

        InventoryLotRecord resultat = service.save(
            new InventoryLotRecord(null, LIGNE, 11, null, null, 18, null, null, false, 1_200));

        assertEquals(20, resultat.quantityInit(), "la quantité initiale du lot est son stock courant");
        assertEquals(-2, resultat.gap());
        assertTrue(resultat.updated());

        assertEquals(22, ligne.getQuantityOnHand(), "la ligne produit vaut la somme de ses lots");
        assertEquals(-8, ligne.getGap(), "22 comptés pour 30 théoriques");
        assertTrue(ligne.getUpdated());
        assertSame(operateur, ligne.getCountedBy());
        verify(storeInventoryLineRepository).saveAndFlush(ligne);
    }

    @Test
    @DisplayName("Un numéro de lot déjà connu du produit est réutilisé, pas recréé")
    void numeroDeLotConnu() {
        Lot existant = lot(11, "LOT-A", 20);
        when(storeInventoryLineRepository.getReferenceById(LIGNE)).thenReturn(ligne);
        when(lotRepository.findByNumLotAndProduitId("LOT-A", 77)).thenReturn(Optional.of(existant));
        when(inventoryLotRepository.saveAndFlush(any(InventoryLot.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryLotRepository.findAllByStoreInventoryLineId(LIGNE)).thenReturn(List.of(inventoryLot(20)));
        when(userService.getUser()).thenReturn(operateur);

        InventoryLotRecord resultat = service.save(
            new InventoryLotRecord(null, LIGNE, null, "LOT-A", null, 20, null, null, false, null));

        assertEquals(11, resultat.lotId());
        verify(lotRepository, never()).saveAndFlush(any(Lot.class));
    }

    @Test
    @DisplayName("Un numéro de lot inconnu crée un lot du produit de la ligne, à quantité nulle")
    void numeroDeLotInconnu() {
        LocalDate peremption = LocalDate.now().plusMonths(8);
        when(storeInventoryLineRepository.getReferenceById(LIGNE)).thenReturn(ligne);
        when(lotRepository.findByNumLotAndProduitId("LOT-NEUF", 77)).thenReturn(Optional.empty());
        when(lotRepository.saveAndFlush(any(Lot.class))).thenAnswer(i -> ((Lot) i.getArgument(0)).setId(99));
        when(inventoryLotRepository.saveAndFlush(any(InventoryLot.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryLotRepository.findAllByStoreInventoryLineId(LIGNE)).thenReturn(List.of(inventoryLot(5)));
        when(userService.getUser()).thenReturn(operateur);

        InventoryLotRecord resultat = service.save(
            new InventoryLotRecord(null, LIGNE, null, "LOT-NEUF", peremption, 5, null, null, false, null));

        ArgumentCaptor<Lot> cree = ArgumentCaptor.forClass(Lot.class);
        verify(lotRepository).saveAndFlush(cree.capture());
        assertSame(produit, cree.getValue().getProduit(), "le lot est rattaché au produit de la ligne");
        assertEquals(0, cree.getValue().getCurrentQuantity(), "un lot découvert au comptage part de zéro");
        assertEquals(peremption, cree.getValue().getExpiryDate());
        assertEquals(StatutLot.AVAILABLE, cree.getValue().getStatut());
        assertEquals(5, resultat.gap(), "tout le lot découvert est en écart positif");

        // Colonnes NOT NULL de `lot` : sans elles, l'insertion échoue en base.
        assertEquals(0, cree.getValue().getQuantity(), "rien n'a été reçu par le système");
        assertEquals(0, cree.getValue().getFreeQty());
        assertEquals(600, cree.getValue().getPrixAchat(), "faute de prix négocié, celui du produit");
        assertEquals(1_000, cree.getValue().getPrixUnit());
        assertNotNull(cree.getValue().getCreatedDate());
    }

    @Test
    @DisplayName("Un lot sans identifiant ni numéro est refusé avant toute écriture")
    void lotNonIdentifiable() {
        when(storeInventoryLineRepository.getReferenceById(LIGNE)).thenReturn(ligne);

        assertThrows(
            IllegalArgumentException.class,
            () -> service.save(new InventoryLotRecord(null, LIGNE, null, null, null, 3, null, null, false, null))
        );
        verify(inventoryLotRepository, never()).saveAndFlush(any(InventoryLot.class));
    }

    @Test
    @DisplayName("Créer un lot sans quantité ne touche pas à la ligne produit")
    void creationSansQuantite() {
        Lot lot = lot(11, "LOT-A", 20);
        when(storeInventoryLineRepository.getReferenceById(LIGNE)).thenReturn(ligne);
        when(lotRepository.getReferenceById(11)).thenReturn(lot);
        when(inventoryLotRepository.saveAndFlush(any(InventoryLot.class))).thenAnswer(i -> i.getArgument(0));

        InventoryLotRecord resultat = service.save(
            new InventoryLotRecord(null, LIGNE, 11, null, null, null, null, null, false, null));

        assertEquals(0, resultat.gap(), "sans quantité comptée, l'écart reste nul");
        assertFalse(resultat.updated());
        verify(storeInventoryLineRepository, never()).saveAndFlush(any(StoreInventoryLine.class));
    }

    @Test
    @DisplayName("Recompter un lot recalcule son écart sur la quantité initiale déjà figée")
    void recomptageDUnLot() {
        InventoryLot existant = inventoryLot(10);
        existant.setId(1L);
        existant.setQuantityInit(25);
        existant.setStoreInventoryLine(ligne);
        existant.setLot(lot(11, "LOT-A", 25));
        when(inventoryLotRepository.getReferenceById(1L)).thenReturn(existant);
        when(inventoryLotRepository.saveAndFlush(existant)).thenReturn(existant);
        when(storeInventoryLineRepository.getReferenceById(LIGNE)).thenReturn(ligne);
        when(inventoryLotRepository.findAllByStoreInventoryLineId(LIGNE)).thenReturn(List.of(existant));
        when(userService.getUser()).thenReturn(operateur);

        InventoryLotRecord resultat = service.update(
            new InventoryLotRecord(1L, LIGNE, 11, "LOT-A", null, 23, null, null, false, null));

        assertEquals(-2, resultat.gap(), "23 comptés pour 25 initialement en stock");
        assertTrue(resultat.updated());
        assertEquals(23, ligne.getQuantityOnHand());
        assertEquals(-7, ligne.getGap());
    }

    @Test
    @DisplayName("Supprimer un lot ramène la ligne produit à la somme des lots restants")
    void suppressionDUnLot() {
        InventoryLot supprime = inventoryLot(8);
        supprime.setId(1L);
        supprime.setStoreInventoryLine(ligne);
        when(inventoryLotRepository.findById(1L)).thenReturn(Optional.of(supprime));
        when(storeInventoryLineRepository.getReferenceById(LIGNE)).thenReturn(ligne);
        when(inventoryLotRepository.findAllByStoreInventoryLineId(LIGNE)).thenReturn(List.of(inventoryLot(12)));
        when(userService.getUser()).thenReturn(operateur);

        service.delete(1L);

        verify(inventoryLotRepository).deleteById(1L);
        assertEquals(12, ligne.getQuantityOnHand());
        assertEquals(-18, ligne.getGap());
    }

    @Test
    @DisplayName("Supprimer un lot déjà absent ne resynchronise rien")
    void suppressionDUnLotAbsent() {
        when(inventoryLotRepository.findById(1L)).thenReturn(Optional.empty());

        service.delete(1L);

        verify(inventoryLotRepository).deleteById(1L);
        verify(storeInventoryLineRepository, never()).saveAndFlush(any(StoreInventoryLine.class));
    }

    @Test
    @DisplayName("Un lot laissé sans quantité ne compte pas dans le total de la ligne")
    void lotNonCompteDansLeTotal() {
        Lot lot = lot(11, "LOT-A", 20);
        when(storeInventoryLineRepository.getReferenceById(LIGNE)).thenReturn(ligne);
        when(lotRepository.getReferenceById(11)).thenReturn(lot);
        when(inventoryLotRepository.saveAndFlush(any(InventoryLot.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryLotRepository.findAllByStoreInventoryLineId(LIGNE))
            .thenReturn(List.of(inventoryLot(6), inventoryLot(null)));
        when(userService.getUser()).thenReturn(operateur);

        service.save(new InventoryLotRecord(null, LIGNE, 11, null, null, 6, null, null, false, null));

        assertEquals(6, ligne.getQuantityOnHand(), "le lot non compté est neutre, il ne vaut pas zéro forcé");
    }

    @Test
    @DisplayName("Une ligne sans quantité initiale garde son écart en l'état")
    void ligneSansQuantiteInitiale() {
        ligne.setQuantityInit(null);
        Lot lot = lot(11, "LOT-A", 20);
        when(storeInventoryLineRepository.getReferenceById(LIGNE)).thenReturn(ligne);
        when(lotRepository.getReferenceById(11)).thenReturn(lot);
        when(inventoryLotRepository.saveAndFlush(any(InventoryLot.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryLotRepository.findAllByStoreInventoryLineId(LIGNE)).thenReturn(List.of(inventoryLot(6)));
        when(userService.getUser()).thenReturn(operateur);

        service.save(new InventoryLotRecord(null, LIGNE, 11, null, null, 6, null, null, false, null));

        assertEquals(6, ligne.getQuantityOnHand());
        assertNotNull(ligne.getUpdatedAt());
        assertEquals(0, ligne.getGap(), "faute de théorique, l'écart n'est pas inventé");
    }

    // ===== jeu d'essai =====

    private Lot lot(int id, String numLot, int quantiteCourante) {
        return new Lot()
            .setId(id)
            .setNumLot(numLot)
            .setProduit(produit)
            .setCurrentQuantity(quantiteCourante)
            .setExpiryDate(LocalDate.now().plusYears(1));
    }

    private InventoryLot inventoryLot(Integer quantiteComptee) {
        InventoryLot inventoryLot = new InventoryLot();
        inventoryLot.setQuantityOnHand(quantiteComptee);
        return inventoryLot;
    }
}

package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.InventoryLotRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLotLineRecord;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * En gestion de lot, la grille de comptage est une vue à plat pilotée par la ligne produit et non
 * par le lot : un produit du périmètre sans aucun lot doit tout de même s'y compter, sinon il
 * devient invisible tout en étant clôturé. C'est le genre de chose qu'un {@code INNER JOIN} casse
 * silencieusement, et qu'aucune doublure ne révèle.
 */
@DisplayName("InventoryLotService — grille des lots sur PostgreSQL")
class InventoryLotServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    private static final Pageable PREMIERE_PAGE = PageRequest.of(0, 20);

    @Test
    @DisplayName("Un produit sans lot figure dans la grille, au stock théorique de la ligne")
    void produitSansLotDansLaGrille() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit aLots = produitEnStock(unique("A LOTS"), 30);
        StoreInventoryLine ligneALots = ligne(inventaire, aLots);
        ligneDeLot(ligneALots, lot(aLots, unique("LOT-A"), LocalDate.now().plusYears(1), 20), null);
        Produit sansLot = produitEnStock(unique("SANS LOT"), 7);
        ligne(inventaire, sansLot);
        viderLeCache();

        Page<StoreInventoryLotLineRecord> page = services.inventoryLotService.findLotFlatPage(
            filtre(inventaire), PREMIERE_PAGE);

        assertEquals(2, page.getTotalElements());
        StoreInventoryLotLineRecord ligneSansLot = page.getContent().stream()
            .filter(r -> r.id() == null)
            .findFirst()
            .orElseThrow();
        assertEquals(sansLot.getId().intValue(), ligneSansLot.produitId());
        assertEquals(7, ligneSansLot.quantityInit(), "le stock théorique vient de la même source que la grille produit");
        assertNull(ligneSansLot.numLot());
        assertNotNull(ligneSansLot.storeInventoryLineId(), "la saisie est routée vers l'API ligne produit");
    }

    @Test
    @DisplayName("Un inventaire vide rend une page vide plutôt qu'une erreur")
    void inventaireVide() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        viderLeCache();

        assertTrue(services.inventoryLotService.findLotFlatPage(filtre(inventaire), PREMIERE_PAGE).isEmpty());
    }

    @Test
    @DisplayName("Compter un lot existant reporte le total sur la ligne produit")
    void comptageDUnLot() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit produit = produitEnStock(unique("A LOTS"), 30);
        Lot premier = lot(produit, unique("LOT-A"), LocalDate.now().plusYears(1), 20);
        Lot second = lot(produit, unique("LOT-B"), LocalDate.now().plusYears(2), 10);
        StoreInventoryLine ligne = ligne(inventaire, produit);
        ligne.setQuantityInit(30);
        viderLeCache();

        services.inventoryLotService.save(new InventoryLotRecord(null, ligne.getId(), premier.getId(),
            null, null, 18, null, null, false, 1_000));
        services.inventoryLotService.save(new InventoryLotRecord(null, ligne.getId(), second.getId(),
            null, null, 9, null, null, false, 1_000));
        viderLeCache();

        StoreInventoryLine relue = services.storeInventoryLineRepository.findById(ligne.getId()).orElseThrow();
        assertEquals(27, relue.getQuantityOnHand(), "la ligne produit vaut la somme de ses lots");
        assertEquals(-3, relue.getGap());
        assertTrue(relue.getUpdated());
        assertNotNull(relue.getCountedBy(), "le comptage est tracé");
    }

    @Test
    @DisplayName("Un numéro de lot inconnu crée le lot du produit sans toucher à son stock")
    void lotDecouvertAuComptage() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit produit = produit(unique("A LOTS"), 1_000, 600);
        stock(produit, rayon, 30);
        StoreInventoryLine ligne = ligne(inventaire, produit);
        viderLeCache();

        InventoryLotRecord cree = services.inventoryLotService.save(
            new InventoryLotRecord(null, ligne.getId(), null, "LOT-SURPRISE",
                LocalDate.now().plusMonths(6), 4, null, null, false, null));
        viderLeCache();

        assertNotNull(cree.lotId(), "le lot manquant est créé, pas refusé");
        assertEquals(0, cree.quantityInit(), "un lot découvert au comptage part d'un théorique nul");
        assertEquals(4, cree.gap());
        assertEquals(
            0,
            compter("SELECT current_quantity FROM lot WHERE id = " + cree.lotId()),
            "la quantité du lot n'est écrite qu'à la clôture"
        );
        assertEquals(600, compter("SELECT prixachat FROM lot WHERE id = " + cree.lotId()),
            "les colonnes obligatoires du lot sont reprises du produit");
        assertEquals(1_000, compter("SELECT prixunit FROM lot WHERE id = " + cree.lotId()));
    }

    @Test
    @DisplayName("Un numéro de lot déjà porté par un autre produit n'est pas récupéré")
    void numeroDeLotPartageEntreProduits() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit premier = produitEnStock(unique("PREMIER"), 10);
        Produit second = produitEnStock(unique("SECOND"), 10);
        Lot lotDuPremier = lot(premier, "LOT-COMMUN", LocalDate.now().plusYears(1), 10);
        StoreInventoryLine ligneDuSecond = ligne(inventaire, second);
        viderLeCache();

        InventoryLotRecord cree = services.inventoryLotService.save(
            new InventoryLotRecord(null, ligneDuSecond.getId(), null, "LOT-COMMUN",
                LocalDate.now().plusYears(1), 5, null, null, false, null));
        viderLeCache();

        assertFalse(lotDuPremier.getId().equals(cree.lotId()),
            "le numéro de lot n'est unique que par produit");
        assertEquals(
            second.getId().intValue(),
            ((Number) em.createNativeQuery("SELECT produit_id FROM lot WHERE id = " + cree.lotId())
                .getSingleResult()).intValue()
        );
    }

    @Test
    @DisplayName("Recompter un lot recalcule son écart et remet la ligne produit à jour")
    void recomptage() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit produit = produitEnStock(unique("A LOTS"), 20);
        Lot lot = lot(produit, unique("LOT-A"), LocalDate.now().plusYears(1), 20);
        StoreInventoryLine ligne = ligne(inventaire, produit);
        ligne.setQuantityInit(20);
        var ligneDeLot = ligneDeLot(ligne, lot, 18);
        viderLeCache();

        InventoryLotRecord recompte = services.inventoryLotService.update(
            new InventoryLotRecord(ligneDeLot.getId(), ligne.getId(), lot.getId(), null, null, 15,
                null, null, false, null));
        viderLeCache();

        assertEquals(-5, recompte.gap());
        assertTrue(recompte.updated());
        assertEquals(15, services.storeInventoryLineRepository.findById(ligne.getId()).orElseThrow().getQuantityOnHand());
    }

    @Test
    @DisplayName("Supprimer un lot ramène la ligne produit à la somme des lots restants")
    void suppression() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit produit = produitEnStock(unique("A LOTS"), 30);
        Lot premier = lot(produit, unique("LOT-A"), LocalDate.now().plusYears(1), 20);
        Lot second = lot(produit, unique("LOT-B"), LocalDate.now().plusYears(2), 10);
        StoreInventoryLine ligne = ligne(inventaire, produit);
        ligne.setQuantityInit(30);
        var aSupprimer = ligneDeLot(ligne, premier, 18);
        ligneDeLot(ligne, second, 9);
        viderLeCache();

        services.inventoryLotService.delete(aSupprimer.getId());
        viderLeCache();

        assertEquals(1, services.inventoryLotService.findByStoreInventoryLineId(ligne.getId()).size());
        assertEquals(9, services.storeInventoryLineRepository.findById(ligne.getId()).orElseThrow().getQuantityOnHand());
    }

    @Test
    @DisplayName("Les lots d'une ligne sont rendus avec leur numéro et leur péremption")
    void lectureDesLotsDUneLigne() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit produit = produitEnStock(unique("A LOTS"), 20);
        LocalDate peremption = LocalDate.now().plusMonths(9);
        Lot lot = lot(produit, "LOT-LISIBLE", peremption, 20);
        StoreInventoryLine ligne = ligne(inventaire, produit);
        ligneDeLot(ligne, lot, 17);
        viderLeCache();

        List<InventoryLotRecord> lots = services.inventoryLotService.findByStoreInventoryLineId(ligne.getId());

        assertEquals(1, lots.size());
        assertEquals("LOT-LISIBLE", lots.getFirst().numLot());
        assertEquals(peremption, lots.getFirst().expiryDate());
        assertEquals(17, lots.getFirst().quantityOnHand());
        assertEquals(-3, lots.getFirst().gap());
    }

    private StoreInventoryLineFilterRecord filtre(StoreInventory inventaire) {
        return new StoreInventoryLineFilterRecord(inventaire.getId(), null, null, null, null);
    }
}

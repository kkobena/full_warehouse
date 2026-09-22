package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.service.dto.CreateInventoryFromProduitIds;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * « Créer un inventaire à partir d'une sélection de produits » est le geste que propose le récap
 * des produits vendus : on liste des produits, on en fait un inventaire à compter.
 *
 * <p>Les lignes y étaient construites à la main, sans emplacement — alors que {@code storage_id}
 * est {@code NOT NULL} en base. Aucun test ne couvrait ce chemin.
 */
@DisplayName("Inventaire créé depuis une sélection de produits")
class InventaireCreationDepuisProduitsIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("chaque produit sélectionné donne une ligne à compter")
    void uneLigneParProduit() {
        Produit premier = produitEnStock(unique("DOLIPRANE SELECTION"), 30);
        Produit second = produitEnStock(unique("EFFERALGAN SELECTION"), 12);

        int cree = creerDepuis(Set.of(premier.getId(), second.getId()));
        viderLeCache();

        assertEquals(2, cree);
        assertEquals(2L, compter(
            "SELECT count(*) FROM store_inventory_line WHERE produit_id IN ("
                + premier.getId() + ", " + second.getId() + ")"));
    }

    /**
     * L'emplacement porte la colonne {@code NOT NULL} qui faisait échouer l'enregistrement, et
     * c'est aussi lui qui rattache la ligne à la grille de saisie : sans lui, la ligne existerait
     * sans jamais s'afficher.
     */
    @Test
    @DisplayName("chaque ligne porte l'emplacement de son inventaire")
    void chaqueLignePorteSonEmplacement() {
        Produit produit = produitEnStock(unique("DOLIPRANE EMPLACEMENT"), 30);

        creerDepuis(Set.of(produit.getId()));
        viderLeCache();

        StoreInventoryLine ligne = ligneDe(produit);
        assertNotNull(ligne.getStorage(), "storage_id est NOT NULL en base");
        assertEquals(ligne.getStoreInventory().getStorage().getId(), ligne.getStorage().getId());
    }

    /**
     * Le stock théorique est lu au comptage, pas à la création : c'est ce que fait le chemin
     * standard, dont les requêtes d'insertion laissent {@code quantity_init} vide. Poser 0 ici
     * ferait croire à un stock théorique nul et transformerait chaque comptage en écart positif.
     */
    @Test
    @DisplayName("la ligne est créée non comptée, sans quantité théorique figée")
    void ligneCreeeNonComptee() {
        Produit produit = produitEnStock(unique("DOLIPRANE NON COMPTE"), 30);

        creerDepuis(Set.of(produit.getId()));
        viderLeCache();

        StoreInventoryLine ligne = ligneDe(produit);
        assertTrue(Boolean.FALSE.equals(ligne.getUpdated()), "la ligne reste à compter");
        assertNull(ligne.getQuantityInit(), "le théorique est lu au comptage");
    }

    // ===== utilitaires =====

    private int creerDepuis(Set<Integer> produitIds) {
        return services.inventaireService.createInventoryFromFrom(
            new CreateInventoryFromProduitIds(
                produitIds,
                new StoreInventoryRecord(null, rayon.getId(), null,
                    InventoryCategory.MAGASIN.name(), null, "Inventaire depuis sélection")
            )
        );
    }

    private StoreInventoryLine ligneDe(Produit produit) {
        List<StoreInventoryLine> lignes = em
            .createQuery(
                "SELECT l FROM StoreInventoryLine l WHERE l.produit.id = :id",
                StoreInventoryLine.class)
            .setParameter("id", produit.getId())
            .getResultList();
        assertEquals(1, lignes.size(), "une ligne et une seule pour ce produit");
        return lignes.getFirst();
    }
}

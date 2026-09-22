package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les lectures d'{@code InventaireService} : ce que l'écran ouvre, ce qu'il liste, ce qu'il
 * enregistre à la saisie. Aucune n'était couverte — le service lui-même n'était câblé dans aucune
 * fixture d'intégration.
 */
@DisplayName("InventaireService — lectures et saisie sur PostgreSQL")
class InventaireServiceLectureIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("un inventaire se relit par son identifiant")
    void lectureParIdentifiant() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        viderLeCache();

        assertEquals(inventaire.getId(),
            services.inventaireService.getStoreInventory(inventaire.getId()).orElseThrow().getId());
    }

    /**
     * L'écran de saisie n'ouvre que ce qui est encore ouvrable : un inventaire clôturé ne se
     * rouvre pas, et la méthode rend un vide plutôt qu'un objet qu'on croirait modifiable.
     */
    @Test
    @DisplayName("un inventaire clôturé ne s'ouvre pas en saisie")
    void inventaireClotureNeSOuvrePas() {
        StoreInventory enCours = inventaire(InventoryCategory.MAGASIN);
        StoreInventory cloture = inventaire(InventoryCategory.MAGASIN);
        cloture.setStatut(InventoryStatut.CLOSED);
        em.flush();
        viderLeCache();

        assertTrue(services.inventaireService.getProccessingStoreInventory(enCours.getId()).isPresent());
        assertTrue(services.inventaireService.getProccessingStoreInventory(cloture.getId()).isEmpty());
    }

    @Test
    @DisplayName("la liste des inventaires actifs laisse les clôturés dehors")
    void listeDesActifs() {
        StoreInventory enCours = inventaire(InventoryCategory.MAGASIN);
        StoreInventory cloture = inventaire(InventoryCategory.MAGASIN);
        cloture.setStatut(InventoryStatut.CLOSED);
        em.flush();
        viderLeCache();

        List<Long> actifs = services.inventaireService.fetchActifs().stream()
            .map(dto -> dto.getId()).toList();

        assertTrue(actifs.contains(enCours.getId()));
        assertFalse(actifs.contains(cloture.getId()), "un inventaire clôturé n'est plus actif");
    }

    @Test
    @DisplayName("les lignes d'un inventaire se listent toutes")
    void listeDesLignes() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligne(inventaire, produitEnStock(unique("DOLIPRANE LISTE"), 30));
        ligne(inventaire, produitEnStock(unique("EFFERALGAN LISTE"), 12));
        viderLeCache();

        assertEquals(2, services.inventaireService.getAllItems(inventaire.getId()).size());
    }

    @Test
    @DisplayName("les rayons représentés dans un inventaire se listent sans doublon")
    void listeDesRayons() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        Rayon antibiotiques = rayon(rayon, unique("ANTIBIO RAYONS"));
        Produit premier = produitEnStock(unique("DOLIPRANE R1"), 30);
        Produit second = produitEnStock(unique("DOLIPRANE R2"), 20);
        ranger(premier, antibiotiques);
        ranger(second, antibiotiques);
        ligne(inventaire, premier);
        ligne(inventaire, second);
        viderLeCache();

        List<Long> rayons = services.inventaireService
            .fetchRayonsByStoreInventoryId(inventaire.getId()).stream()
            .map(r -> (long) r.id()).toList();

        assertEquals(1, rayons.size(), "deux produits, un seul rayon");
        assertEquals(antibiotiques.getId().longValue(), rayons.getFirst());
    }

    @Test
    @DisplayName("compter une ligne enregistre la quantité et l'écart")
    void comptageDUneLigne() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        StoreInventoryLine ligne = ligne(inventaire, produitEnStock(unique("DOLIPRANE COMPTE"), 30));
        viderLeCache();

        StoreInventoryLineDTO dto = new StoreInventoryLineDTO(em.find(StoreInventoryLine.class, ligne.getId()));
        dto.setQuantityInit(30);
        dto.setQuantityOnHand(27);

        var resultat = services.inventaireService.updateQuantityOnHand(dto);
        viderLeCache();

        assertEquals(27, resultat.quantityOnHand());
        assertEquals(-3, resultat.gap());
        assertTrue(em.find(StoreInventoryLine.class, ligne.getId()).getUpdated());
    }

    @Test
    @DisplayName("la saisie multiple enregistre chaque ligne du lot")
    void saisieMultiple() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        StoreInventoryLine premiere = ligne(inventaire, produitEnStock(unique("DOLIPRANE MULTI"), 30));
        StoreInventoryLine seconde = ligne(inventaire, produitEnStock(unique("EFFERALGAN MULTI"), 12));
        viderLeCache();

        services.inventaireService.synchronizeStoreInventoryLine(
            List.of(comptage(premiere, 30, 29), comptage(seconde, 12, 10)));
        viderLeCache();

        assertEquals(-1, em.find(StoreInventoryLine.class, premiere.getId()).getGap());
        assertEquals(-2, em.find(StoreInventoryLine.class, seconde.getId()).getGap());
    }

    /** Les deux exports délèguent au service de requête ; on vérifie que le relais est branché. */
    @Test
    @DisplayName("les exports passent par le service de requête")
    void exportsDelegues() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produitEnStock(unique("DOLIPRANE EXPORT"), 30), 30, 28);
        viderLeCache();

        // Le relais est branché : l'export remonte la ligne comptée, même sans lot saisi.
        assertEquals(1, services.inventaireService.getLotGroupsForExport(inventaire.getId()).size());
    }

    private StoreInventoryLineDTO comptage(StoreInventoryLine ligne, int theorique, int compte) {
        StoreInventoryLineDTO dto =
            new StoreInventoryLineDTO(em.find(StoreInventoryLine.class, ligne.getId()));
        dto.setQuantityInit(theorique);
        dto.setQuantityOnHand(compte);
        return dto;
    }
}

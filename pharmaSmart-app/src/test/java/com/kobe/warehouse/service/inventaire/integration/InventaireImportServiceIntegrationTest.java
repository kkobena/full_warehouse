package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.service.dto.records.ImportResultRecord;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * L'import CSV compte des lignes par code CIP. Deux choses ne se vérifient qu'en base : que la
 * requête ne ramène que les lignes de l'inventaire visé — un même code appartient à autant de
 * lignes qu'il y a d'inventaires ouverts — et qu'une ligne importée sort aussi complète qu'une
 * ligne saisie à l'écran, valorisation comprise, faute de quoi la clôture échouera bien plus tard.
 */
@DisplayName("InventaireImportService — import CSV sur PostgreSQL")
class InventaireImportServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("Une ligne importée est comptée aussi complètement qu'une ligne saisie")
    void ligneImporteeComplete() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit produit = produit(unique("MANQUANT"), 1_000, 600);
        stock(produit, rayon, 10);
        StoreInventoryLine ligne = ligne(inventaire, produit);
        viderLeCache();

        ImportResultRecord resultat = services.inventaireImportService.importDetail(
            inventaire.getId(), fichier(codeCip(produit) + ";7"));
        viderLeCache();

        assertEquals(1, resultat.imported());
        StoreInventoryLine relue = services.storeInventoryLineRepository.findById(ligne.getId()).orElseThrow();
        assertEquals(10, relue.getQuantityInit(), "la quantité initiale est relue du stock théorique");
        assertEquals(7, relue.getQuantityOnHand());
        assertEquals(-3, relue.getGap());
        assertEquals(600, relue.getInventoryValueCost(), "sans valorisation, la clôture échouerait");
        assertEquals(1_000, relue.getLastUnitPrice());
        assertTrue(relue.getUpdated());
        assertNotNull(relue.getCountedBy());
    }

    @Test
    @DisplayName("L'import ne touche qu'à l'inventaire visé, même si un autre partage le code CIP")
    void cloisonnementEntreInventaires() {
        Produit produit = produit(unique("PARTAGE"), 1_000, 600);
        stock(produit, rayon, 10);
        StoreInventory vise = inventaire(InventoryCategory.MAGASIN, rayon);
        StoreInventory voisin = inventaire(InventoryCategory.MAGASIN, rayon);
        StoreInventoryLine ligneVisee = ligne(vise, produit);
        StoreInventoryLine ligneVoisine = ligne(voisin, produit);
        viderLeCache();

        services.inventaireImportService.importDetail(vise.getId(), fichier(codeCip(produit) + ";4"));
        viderLeCache();

        assertEquals(4, services.storeInventoryLineRepository.findById(ligneVisee.getId()).orElseThrow().getQuantityOnHand());
        assertFalse(services.storeInventoryLineRepository.findById(ligneVoisine.getId()).orElseThrow().getUpdated(),
            "l'inventaire voisin n'a pas été compté à l'insu de son opérateur");
    }

    @Test
    @DisplayName("La portée du stock théorique suit celle de l'inventaire")
    void porteeDuStockTheorique() {
        Produit produit = produit(unique("DOUBLE EMPLACEMENT"), 1_000, 600);
        stock(produit, rayon, 12);
        stock(produit, reserve, 40);
        StoreInventory inventaireDeRayon = inventaire(InventoryCategory.RAYON, rayon);
        StoreInventoryLine ligne = ligne(inventaireDeRayon, produit);
        viderLeCache();

        services.inventaireImportService.importDetail(inventaireDeRayon.getId(), fichier(codeCip(produit) + ";12"));
        viderLeCache();

        StoreInventoryLine relue = services.storeInventoryLineRepository.findById(ligne.getId()).orElseThrow();
        assertEquals(12, relue.getQuantityInit(), "un inventaire de rayon ne consolide pas la réserve");
        assertEquals(0, relue.getGap());
    }

    @Test
    @DisplayName("Un fichier dont les codes n'appartiennent pas à l'inventaire est ignoré sans échouer")
    void codesEtrangers() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit dansLInventaire = produitEnStock(unique("DEDANS"), 10);
        ligne(inventaire, dansLInventaire);
        Produit dehors = produitEnStock(unique("DEHORS"), 10);
        viderLeCache();

        ImportResultRecord resultat = services.inventaireImportService.importDetail(
            inventaire.getId(), fichier(codeCip(dehors) + ";5"));

        assertEquals(0, resultat.imported());
        assertEquals(1, resultat.ignored());
        assertEquals(0, resultat.rejected());
    }

    @Test
    @DisplayName("Un fichier mêlant lignes valides et lignes fautives compte ce qu'il peut")
    void fichierPartiellementValide() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit premier = produitEnStock(unique("PREMIER"), 10);
        Produit second = produitEnStock(unique("SECOND"), 20);
        ligne(inventaire, premier);
        ligne(inventaire, second);
        viderLeCache();

        ImportResultRecord resultat = services.inventaireImportService.importDetail(
            inventaire.getId(),
            fichier("%s;9\n%s;beaucoup\n%s;18".formatted(codeCip(premier), codeCip(second), codeCip(second)))
        );
        viderLeCache();

        assertEquals(2, resultat.imported());
        assertEquals(1, resultat.rejected());
        assertEquals(1, resultat.errors().size(), "une seule ligne du fichier a été rejetée");
        assertEquals(
            2,
            compter("SELECT count(*) FROM store_inventory_line WHERE store_inventory_id = %d AND updated"
                .formatted(inventaire.getId()))
        );
    }

    private MultipartFile fichier(String contenu) {
        return new MockMultipartFile("file", "inventaire.csv", "text/csv", contenu.getBytes(StandardCharsets.UTF_8));
    }
}

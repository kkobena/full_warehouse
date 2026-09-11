package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.service.dto.records.InventoryProgressRecord;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La progression est ce que l'opérateur regarde pour savoir s'il peut clôturer. Elle doit être
 * exprimée dans l'unité qu'il compte réellement : la ligne produit sur un inventaire ordinaire,
 * le lot dès qu'il y en a. Une seule requête sert les deux cas — la vérifier revient à vérifier
 * que la branche « lot » de l'union se referme d'elle-même quand il n'y a pas de lot.
 */
@DisplayName("InventaireProgressService — avancement du comptage sur PostgreSQL")
class InventaireProgressServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("Sur un inventaire sans lot, la progression se compte en lignes produit")
    void progressionParLigne() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produitEnStock(unique("COMPTE SANS ECART"), 10), 10, 10);
        ligneComptee(inventaire, produitEnStock(unique("COMPTE AVEC ECART"), 10), 10, 7);
        ligne(inventaire, produitEnStock(unique("NON COMPTE"), 10));
        ligne(inventaire, produitEnStock(unique("NON COMPTE BIS"), 10));
        viderLeCache();

        InventoryProgressRecord progression = services.inventaireProgressService.getProgress(inventaire.getId());

        assertEquals(4, progression.totalLines());
        assertEquals(2, progression.updatedLines());
        assertEquals(1, progression.linesWithGap());
        assertEquals(50, progression.progressPercent());
    }

    @Test
    @DisplayName("Dès qu'une ligne porte des lots, ce sont les lots qui sont comptés")
    void progressionParLot() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        Produit aLots = produitEnStock(unique("A LOTS"), 30);
        Lot premier = lot(aLots, unique("LOT-A"), LocalDate.now().plusYears(1), 20);
        Lot second = lot(aLots, unique("LOT-B"), LocalDate.now().plusYears(2), 10);
        StoreInventoryLine ligneALots = ligne(inventaire, aLots);
        ligneDeLot(ligneALots, premier, 18);
        ligneDeLot(ligneALots, second, null);
        ligne(inventaire, produitEnStock(unique("SANS LOT"), 5));
        viderLeCache();

        InventoryProgressRecord progression = services.inventaireProgressService.getProgress(inventaire.getId());

        assertEquals(3, progression.totalLines(), "deux lots et une ligne sans lot");
        assertEquals(1, progression.updatedLines(), "la ligne parente ne compte pas en plus de ses lots");
        assertEquals(1, progression.linesWithGap());
        assertEquals(33, progression.progressPercent());
    }

    @Test
    @DisplayName("Un inventaire vide ne renvoie pas une progression indéfinie")
    void inventaireVide() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        viderLeCache();

        InventoryProgressRecord progression = services.inventaireProgressService.getProgress(inventaire.getId());

        assertEquals(0, progression.totalLines());
        assertEquals(0, progression.progressPercent(), "pas de division par zéro");
    }

    @Test
    @DisplayName("La progression d'un inventaire ne compte pas les lignes de ses voisins")
    void cloisonnementEntreInventaires() {
        StoreInventory premier = inventaire(InventoryCategory.MAGASIN);
        StoreInventory second = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(premier, produitEnStock(unique("CHEZ LE PREMIER"), 10), 10, 10);
        ligne(second, produitEnStock(unique("CHEZ LE SECOND"), 10));
        viderLeCache();

        assertEquals(1, services.inventaireProgressService.getProgress(premier.getId()).totalLines());
        assertEquals(100, services.inventaireProgressService.getProgress(premier.getId()).progressPercent());
        assertEquals(0, services.inventaireProgressService.getProgress(second.getId()).progressPercent());
    }
}

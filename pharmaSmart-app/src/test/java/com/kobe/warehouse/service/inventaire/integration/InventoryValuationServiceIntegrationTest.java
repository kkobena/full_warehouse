package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryByGroupRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La valorisation est le chiffre que le pharmacien porte à sa comptabilité : la valeur du stock
 * avant et après comptage, et l'écart entre les deux. Elle n'existe qu'en SQL, et deux pièges s'y
 * cachent — un {@code integer * integer} qui déborde silencieusement au-delà de deux milliards, et
 * un {@code SUM} que PostgreSQL renvoie dans un type que le mapping ne doit pas présumer.
 */
@DisplayName("InventoryValuationService — valorisation d'un inventaire sur PostgreSQL")
class InventoryValuationServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("Le résumé global chiffre le stock avant, après et l'écart")
    void resumeGlobal() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produit(unique("MANQUANT"), 1_000, 600), 10, 8);
        ligneComptee(inventaire, produit(unique("EXCEDENT"), 2_000, 1_500), 5, 6);
        viderLeCache();

        StoreInventorySummaryRecord resume = services.inventoryValuationService.getGlobalSummary(inventaire.getId());

        assertEquals(0, BigDecimal.valueOf(13_500).compareTo(resume.costValueBegin()), "10×600 + 5×1500");
        assertEquals(0, BigDecimal.valueOf(13_800).compareTo(resume.costValueAfter()), "8×600 + 6×1500");
        assertEquals(0, BigDecimal.valueOf(20_000).compareTo(resume.amountValueBegin()), "10×1000 + 5×2000");
        assertEquals(0, BigDecimal.valueOf(20_000).compareTo(resume.amountValueAfter()), "8×1000 + 6×2000");
        assertEquals(0, BigDecimal.valueOf(300).compareTo(resume.gapCost()), "−2×600 + 1×1500");
        assertEquals(0, BigDecimal.ZERO.compareTo(resume.gapAmount()), "−2×1000 + 1×2000");
    }

    @Test
    @DisplayName("Un inventaire sans ligne comptée vaut zéro, pas null")
    void inventaireSansComptage() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligne(inventaire, produitEnStock(unique("NON COMPTE"), 10));
        viderLeCache();

        StoreInventorySummaryRecord resume = services.inventoryValuationService.getGlobalSummary(inventaire.getId());

        assertEquals(0, BigDecimal.ZERO.compareTo(resume.costValueBegin()));
        assertEquals(0, BigDecimal.ZERO.compareTo(resume.gapAmount()));
    }

    @Test
    @DisplayName("Une valorisation au-delà de deux milliards ne déborde pas")
    void valorisationHorsBornesDuEntier() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produit(unique("TRES CHER"), 2_000_000, 1_500_000), 2_000, 2_000);
        viderLeCache();

        StoreInventorySummaryRecord resume = services.inventoryValuationService.getGlobalSummary(inventaire.getId());

        assertEquals(
            0,
            BigDecimal.valueOf(4_000_000_000L).compareTo(resume.amountValueAfter()),
            "2 000 × 2 000 000 dépasse la capacité d'un entier signé"
        );
    }

    @Test
    @DisplayName("La ventilation par emplacement regroupe les lignes sous le nom de l'emplacement")
    void ventilationParEmplacement() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produit(unique("A"), 1_000, 600), 10, 9);
        ligneComptee(inventaire, produit(unique("B"), 1_000, 600), 10, 10);
        viderLeCache();

        List<StoreInventorySummaryByGroupRecord> groupes =
            services.inventoryValuationService.getSummaryByGroup(inventaire.getId(), "STORAGE");

        assertEquals(1, groupes.size());
        assertEquals(rayon.getName(), groupes.getFirst().groupLabel());
        assertEquals(2, groupes.getFirst().lineCount());
        assertEquals(0, BigDecimal.valueOf(-600).compareTo(groupes.getFirst().gapCost()));
    }

    @Test
    @DisplayName("La ventilation par famille sépare les familles de produits")
    void ventilationParFamille() {
        List<FamilleProduit> familles = em
            .createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class)
            .setMaxResults(2)
            .getResultList();
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produit(unique("FAMILLE 1"), 1_000, 600, familles.get(0)), 10, 9);
        ligneComptee(inventaire, produit(unique("FAMILLE 2"), 1_000, 600, familles.get(1)), 10, 10);
        viderLeCache();

        List<StoreInventorySummaryByGroupRecord> groupes =
            services.inventoryValuationService.getSummaryByGroup(inventaire.getId(), "FAMILLE");

        assertEquals(2, groupes.size());
        assertTrue(groupes.stream().allMatch(g -> g.lineCount() == 1));
    }

    @Test
    @DisplayName("Les produits qu'aucun rayon ne réclame sont regroupés à part")
    void ventilationParRayon() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        Rayon antibiotiques = rayon(rayon, "ANTIBIOTIQUES");
        Produit range = produit(unique("RANGE"), 1_000, 600);
        ranger(range, antibiotiques);
        ligneComptee(inventaire, range, 10, 9);
        ligneComptee(inventaire, produit(unique("SANS RAYON"), 1_000, 600), 10, 10);
        viderLeCache();

        List<StoreInventorySummaryByGroupRecord> groupes =
            services.inventoryValuationService.getSummaryByGroup(inventaire.getId(), "RAYON");

        assertEquals(2, groupes.size());
        assertTrue(
            groupes.stream().anyMatch(g -> "Sans rayon".equals(g.groupLabel())),
            "les lignes hors rayon ne disparaissent pas de la ventilation"
        );
    }

    @Test
    @DisplayName("Un axe de ventilation inconnu retombe sur l'emplacement")
    void axeInconnu() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produit(unique("A"), 1_000, 600), 10, 9);
        viderLeCache();

        assertEquals(
            services.inventoryValuationService.getSummaryByGroup(inventaire.getId(), "STORAGE"),
            services.inventoryValuationService.getSummaryByGroup(inventaire.getId(), "N IMPORTE QUOI")
        );
        assertEquals(
            services.inventoryValuationService.getSummaryByGroup(inventaire.getId(), "STORAGE"),
            services.inventoryValuationService.getSummaryByGroup(inventaire.getId(), null)
        );
    }
}

package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.service.dto.InventoryExportWrapper;
import com.kobe.warehouse.service.dto.StoreInventoryLotGroupExport;
import com.kobe.warehouse.service.dto.enumeration.StoreInventoryExportGroupBy;
import com.kobe.warehouse.service.dto.enumeration.StoreInventoryLineEnum;
import com.kobe.warehouse.service.dto.filter.StoreInventoryExportRecord;
import com.kobe.warehouse.service.dto.filter.StoreInventoryLineFilterRecord;
import com.kobe.warehouse.service.dto.records.StoreInventoryLineRecord;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * La grille de saisie est construite par du SQL assemblé à la volée : filtres, jointures
 * optionnelles de lot et de classe Pareto, pagination. Trois choses ne se voient qu'en base — que
 * la requête reste valide quelles que soient les options retenues, que le stock théorique injecté
 * dans chaque ligne suive bien la portée de l'inventaire, et que les filtres d'écart s'appuient
 * sur des colonnes réellement renseignées au comptage.
 */
@DisplayName("InventaireQueryService — grille de saisie et export sur PostgreSQL")
class InventaireQueryServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    private static final Pageable PREMIERE_PAGE = PageRequest.of(0, 20);

    @Test
    @DisplayName("La grille rend les lignes avec leur stock théorique, pris dans la portée de l'inventaire")
    void grilleAvecStockTheorique() {
        StoreInventory inventaire = inventaire(InventoryCategory.RAYON, rayon);
        Produit produit = produit(unique("DOUBLE EMPLACEMENT"));
        stock(produit, rayon, 12);
        stock(produit, reserve, 40);
        ligne(inventaire, produit);
        viderLeCache();

        Page<StoreInventoryLineRecord> page = page(filtre(inventaire));

        assertEquals(1, page.getTotalElements());
        StoreInventoryLineRecord ligne = page.getContent().getFirst();
        assertEquals(12, ligne.quantityInit(), "un inventaire de rayon ignore la réserve");
        assertEquals(codeCip(produit), ligne.produitCip());
        assertEquals(0, ligne.lotCount(), "sans gestion de lot, aucun lot n'est annoncé");
    }

    @Test
    @DisplayName("Sur un inventaire magasin, la même ligne part du stock consolidé")
    void grilleSurInventaireMagasin() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit produit = produit(unique("DOUBLE EMPLACEMENT"));
        stock(produit, rayon, 12);
        stock(produit, reserve, 40);
        ligne(inventaire, produit);
        viderLeCache();

        assertEquals(52, page(filtre(inventaire)).getContent().getFirst().quantityInit());
    }

    @Test
    @DisplayName("Un inventaire clôturé ne rouvre pas sa grille de saisie")
    void inventaireCloture() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produitEnStock(unique("COMPTE"), 10), 10, 10);
        inventaire.setStatut(InventoryStatut.CLOSED);
        viderLeCache();

        assertTrue(services.inventaireQueryService.getInventoryPage(filtre(inventaire), PREMIERE_PAGE, true).isEmpty());
        assertEquals(
            1,
            services.inventaireQueryService.getInventoryPage(filtre(inventaire), PREMIERE_PAGE, false).getTotalElements(),
            "la consultation d'un inventaire clos reste possible"
        );
    }

    @Test
    @DisplayName("Le filtre d'écart ne retient que les lignes comptées qui s'écartent du théorique")
    void filtreEcart() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produitEnStock(unique("AVEC ECART"), 10), 10, 7);
        ligneComptee(inventaire, produitEnStock(unique("SANS ECART"), 10), 10, 10);
        ligne(inventaire, produitEnStock(unique("NON COMPTE"), 10));
        viderLeCache();

        assertEquals(1, page(filtre(inventaire, StoreInventoryLineEnum.GAP)).getTotalElements());
        assertEquals(1, page(filtre(inventaire, StoreInventoryLineEnum.GAP_NEGATIF)).getTotalElements());
        assertEquals(1, page(filtre(inventaire, StoreInventoryLineEnum.GAP_POSITIF)).getTotalElements(),
            "une ligne sans écart est comptée comme positive ou nulle");
        assertEquals(1, page(filtre(inventaire, StoreInventoryLineEnum.NOT_UPDATED)).getTotalElements());
        assertEquals(2, page(filtre(inventaire, StoreInventoryLineEnum.UPDATED)).getTotalElements());
        assertEquals(3, page(filtre(inventaire, StoreInventoryLineEnum.NONE)).getTotalElements());
    }

    @Test
    @DisplayName("La recherche porte sur le libellé comme sur le code CIP")
    void recherche() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        Produit doliprane = produit("DOLIPRANE 1000MG");
        ligne(inventaire, doliprane);
        ligne(inventaire, produit("EFFERALGAN 500MG"));
        viderLeCache();

        assertEquals(1, page(new StoreInventoryLineFilterRecord(inventaire.getId(), "DOLIPRANE", null, null, null))
            .getTotalElements());
        assertEquals(1, page(new StoreInventoryLineFilterRecord(inventaire.getId(), codeCip(doliprane), null, null, null))
            .getTotalElements());
        assertEquals(0, page(new StoreInventoryLineFilterRecord(inventaire.getId(), "INTROUVABLE", null, null, null))
            .getTotalElements());
    }

    @Test
    @DisplayName("Le filtre par rayon restreint la grille aux produits qui y sont rangés")
    void filtreParRayon() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        Rayon antibiotiques = rayon(rayon, "ANTIBIOTIQUES");
        Produit range = produit(unique("RANGE"));
        ranger(range, antibiotiques);
        ligne(inventaire, range);
        ligne(inventaire, produit(unique("HORS RAYON")));
        viderLeCache();

        Page<StoreInventoryLineRecord> page = page(
            new StoreInventoryLineFilterRecord(inventaire.getId(), null, null, antibiotiques.getId().longValue(), null));

        assertEquals(1, page.getTotalElements());
        assertEquals(range.getId().intValue(), page.getContent().getFirst().produitId());
    }

    @Test
    @DisplayName("La pagination s'arrête au nombre réel de lignes")
    void pagination() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        for (int i = 0; i < 5; i++) {
            ligne(inventaire, produitEnStock(unique("PRODUIT"), 10));
        }
        viderLeCache();

        Page<StoreInventoryLineRecord> premiere = services.inventaireQueryService
            .getInventoryPage(filtre(inventaire), PageRequest.of(0, 2), true);

        assertEquals(5, premiere.getTotalElements());
        assertEquals(2, premiere.getContent().size());
        assertEquals(3, premiere.getTotalPages());
    }

    @Test
    @DisplayName("Avec la gestion de lot, chaque ligne annonce le nombre de lots à compter")
    void nombreDeLotsAnnonce() {
        when(services.appConfigurationService.useGestionLotInventaire()).thenReturn(true);
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        Produit produit = produitEnStock(unique("A LOTS"), 30);
        StoreInventoryLine ligne = ligne(inventaire, produit);
        ligneDeLot(ligne, lot(produit, unique("LOT-A"), LocalDate.now().plusYears(1), 20), null);
        ligneDeLot(ligne, lot(produit, unique("LOT-B"), LocalDate.now().plusYears(2), 10), null);
        viderLeCache();

        assertEquals(2, page(filtre(inventaire)).getContent().getFirst().lotCount());
    }

    @Test
    @DisplayName("Un inventaire de classe ABC ramène la classe Pareto de chaque produit")
    void classeParetoSurInventaireAbc() {
        StoreInventory inventaire = inventaire(InventoryCategory.ABC, rayon);
        ligne(inventaire, produitEnStock(unique("JAMAIS VENDU"), 10));
        viderLeCache();

        Page<StoreInventoryLineRecord> page = page(filtre(inventaire));

        assertEquals(1, page.getTotalElements(),
            "la jointure sur la vue ABC ne fait pas disparaître les lignes d'un produit jamais vendu");
    }

    @Test
    @DisplayName("Un export sans ligne ne fabrique pas d'enveloppe vide")
    void exportSansLigne() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        viderLeCache();

        assertNull(services.inventaireQueryService.exportInventory(export(inventaire)));
    }

    @Test
    @DisplayName("L'export totalise la valeur du stock avant et après comptage")
    void exportValorise() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        ligneComptee(inventaire, produit(unique("MANQUANT"), 1_000, 600), 10, 8);
        viderLeCache();

        InventoryExportWrapper enveloppe = services.inventaireQueryService.exportInventory(export(inventaire));

        assertNotNull(enveloppe);
        assertEquals(1, enveloppe.getInventoryGroups().size());
        assertEquals(6_000, enveloppe.getInventoryExportSummaries().get("achatAvant").getValue(), "10 × 600");
        assertEquals(4_800, enveloppe.getInventoryExportSummaries().get("achatApres").getValue(), "8 × 600");
        assertEquals(-1_200, enveloppe.getInventoryExportSummaries().get("achatEcart").getValue());
    }

    @Test
    @DisplayName("L'export par lot fait figurer les produits sans lot, au stock théorique")
    void exportParLot() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN, rayon);
        Produit aLots = produitEnStock(unique("A LOTS"), 30);
        StoreInventoryLine ligneALots = ligne(inventaire, aLots);
        ligneDeLot(ligneALots, lot(aLots, unique("LOT-A"), LocalDate.now().plusYears(1), 20), 18);
        Produit sansLot = produitEnStock(unique("SANS LOT"), 7);
        ligne(inventaire, sansLot);
        viderLeCache();

        List<StoreInventoryLotGroupExport> groupes =
            services.inventaireQueryService.getLotGroupsForExport(inventaire.getId());

        assertEquals(2, groupes.size(), "le produit sans lot ne disparaît pas de l'export");
        StoreInventoryLotGroupExport groupeSansLot = groupes.stream()
            .filter(g -> g.getCodeCip().equals(codeCip(sansLot)))
            .findFirst()
            .orElseThrow();
        assertEquals(7, groupeSansLot.getTotalInit(), "son stock initial vient de la même source que la grille produit");
        StoreInventoryLotGroupExport groupeALots = groupes.stream()
            .filter(g -> g.getCodeCip().equals(codeCip(aLots)))
            .findFirst()
            .orElseThrow();
        assertEquals(20, groupeALots.getTotalInit(), "le lot porte sa propre quantité initiale, figée à la création");
        assertEquals(18, groupeALots.getTotalOnHand());
    }

    // ===== outils =====

    private Page<StoreInventoryLineRecord> page(StoreInventoryLineFilterRecord filtre) {
        return services.inventaireQueryService.getInventoryPage(filtre, PREMIERE_PAGE, true);
    }

    private StoreInventoryLineFilterRecord filtre(StoreInventory inventaire) {
        return new StoreInventoryLineFilterRecord(inventaire.getId(), null, null, null, null);
    }

    private StoreInventoryLineFilterRecord filtre(StoreInventory inventaire, StoreInventoryLineEnum selection) {
        return new StoreInventoryLineFilterRecord(inventaire.getId(), null, null, null, selection);
    }

    private StoreInventoryExportRecord export(StoreInventory inventaire) {
        return new StoreInventoryExportRecord(StoreInventoryExportGroupBy.NONE, filtre(inventaire), false);
    }
}

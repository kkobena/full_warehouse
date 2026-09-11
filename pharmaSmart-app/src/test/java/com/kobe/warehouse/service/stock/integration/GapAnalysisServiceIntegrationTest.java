package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.CauseEcart;
import com.kobe.warehouse.service.dto.records.GapEntryRecord;
import com.kobe.warehouse.service.dto.records.GapLineRecord;
import com.kobe.warehouse.service.dto.records.GapSummaryRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * {@link com.kobe.warehouse.service.stock.GapAnalysisService} sur un vrai PostgreSQL.
 *
 * <p>Qualifier les écarts d'un inventaire, c'est écrire dans une table annexe, ligne par ligne,
 * depuis un écran paginé. Trois choses ne se prouvent que sur une vraie base : le tri figé par
 * écart absolu décroissant, qui décide de la stabilité de la pagination ; l'« upsert », qui ne doit
 * pas effacer les qualifications des pages non visitées ni recréer une ligne déjà saisie ; et le
 * garde-fou qui interdit d'écrire sur la ligne d'un autre inventaire.
 */
@DisplayName("GapAnalysisService — qualification des écarts d'inventaire sur PostgreSQL")
class GapAnalysisServiceIntegrationTest extends AbstractStockIntegrationTest {

    @Test
    @DisplayName("Seules les lignes comptées avec un écart remontent, le plus gros écart en tête")
    void lignesAvecEcart() {
        StoreInventory inventaire = inventaire();
        ligne(inventaire, "PETIT ECART", 100, 97, 500);
        StoreInventoryLine grosse = ligne(inventaire, "GROS ECART", 100, 130, 500);
        ligne(inventaire, "SANS ECART", 100, 100, 500);
        ligneNonComptee(inventaire, "NON COMPTEE", 100, 80);
        viderLeCache();

        List<GapLineRecord> lignes = services.gapAnalysisService.getLinesWithGap(inventaire.getId(), page()).getContent();

        assertEquals(2, lignes.size(), "ni la ligne sans écart, ni celle qui n'a pas été comptée");
        assertEquals(grosse.getId(), lignes.getFirst().lineId(), "l'écart le plus grand en valeur absolue vient en tête");
        assertEquals(30, lignes.getFirst().gap());
        assertEquals(15_000, lignes.getFirst().valeurEcart(), "30 unités à 500 : la valeur de l'écart est calculée en base");
        assertNull(lignes.getFirst().existingCause(), "rien n'est encore qualifié");
    }

    @Test
    @DisplayName("Qualifier une ligne enregistre la cause, le commentaire et la quantité en écart")
    void qualificationDUneLigne() {
        StoreInventory inventaire = inventaire();
        StoreInventoryLine ligne = ligne(inventaire, "CASSE VO", 50, 44, 1_000);
        viderLeCache();

        services.gapAnalysisService.saveAnalysis(
            inventaire.getId(),
            List.of(new GapEntryRecord(ligne.getId(), CauseEcart.CASSE.name(), "carton tombé à la réception"))
        );
        viderLeCache();

        GapLineRecord relue = services.gapAnalysisService.getLinesWithGap(inventaire.getId(), page()).getContent().getFirst();
        assertEquals(CauseEcart.CASSE, relue.existingCause());
        assertEquals("carton tombé à la réception", relue.existingComment());
        assertEquals(
            6,
            ((Number) em
                    .createNativeQuery("SELECT quantity FROM inventory_gap_analysis WHERE store_inventory_line_id = " + ligne.getId())
                    .getSingleResult()).intValue(),
            "la quantité enregistrée est l'écart en valeur absolue"
        );
    }

    @Test
    @DisplayName("Une seconde qualification met à jour la ligne au lieu d'en créer une autre")
    void requalification() {
        StoreInventory inventaire = inventaire();
        StoreInventoryLine ligne = ligne(inventaire, "REQUALIFIE", 50, 40, 1_000);
        services.gapAnalysisService.saveAnalysis(
            inventaire.getId(),
            List.of(new GapEntryRecord(ligne.getId(), CauseEcart.INCONNU.name(), "à vérifier"))
        );
        viderLeCache();

        services.gapAnalysisService.saveAnalysis(
            inventaire.getId(),
            List.of(new GapEntryRecord(ligne.getId(), CauseEcart.VOL.name(), "confirmé après visionnage"))
        );
        viderLeCache();

        assertEquals(
            1,
            compter("SELECT count(*) FROM inventory_gap_analysis WHERE store_inventory_line_id = " + ligne.getId()),
            "la qualification est réécrite, pas dupliquée"
        );
        assertEquals(
            CauseEcart.VOL,
            services.gapAnalysisService.getLinesWithGap(inventaire.getId(), page()).getContent().getFirst().existingCause()
        );
    }

    @Test
    @DisplayName("Une cause vide efface la qualification de la ligne")
    void effacementDUneQualification() {
        StoreInventory inventaire = inventaire();
        StoreInventoryLine ligne = ligne(inventaire, "ANNULE", 30, 25, 800);
        services.gapAnalysisService.saveAnalysis(
            inventaire.getId(),
            List.of(new GapEntryRecord(ligne.getId(), CauseEcart.PEREMPTION.name(), null))
        );
        viderLeCache();

        services.gapAnalysisService.saveAnalysis(inventaire.getId(), List.of(new GapEntryRecord(ligne.getId(), "  ", null)));
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM inventory_gap_analysis WHERE store_inventory_line_id = " + ligne.getId()));
        assertFalse(services.gapAnalysisService.hasAnalysis(inventaire.getId()));
    }

    @Test
    @DisplayName("Une ligne absente de la charge utile garde sa qualification")
    void qualificationPartielle() {
        StoreInventory inventaire = inventaire();
        StoreInventoryLine premiere = ligne(inventaire, "PAGE 1", 40, 30, 700);
        StoreInventoryLine seconde = ligne(inventaire, "PAGE 2", 40, 35, 700);
        services.gapAnalysisService.saveAnalysis(
            inventaire.getId(),
            List.of(
                new GapEntryRecord(premiere.getId(), CauseEcart.VOL.name(), null),
                new GapEntryRecord(seconde.getId(), CauseEcart.CASSE.name(), null)
            )
        );
        viderLeCache();

        // L'écran est paginé : la page suivante ne soumet que ses propres lignes.
        services.gapAnalysisService.saveAnalysis(
            inventaire.getId(),
            List.of(new GapEntryRecord(seconde.getId(), CauseEcart.ERREUR_SAISIE.name(), null))
        );
        viderLeCache();

        assertEquals(2, compter("SELECT count(*) FROM inventory_gap_analysis"), "la première qualification survit");
    }

    @Test
    @DisplayName("Une ligne appartenant à un autre inventaire n'est pas qualifiée")
    void ligneDUnAutreInventaire() {
        StoreInventory inventaire = inventaire();
        StoreInventory autre = inventaire();
        StoreInventoryLine ligneDeLAutre = ligne(autre, "AILLEURS", 40, 20, 900);
        viderLeCache();

        services.gapAnalysisService.saveAnalysis(
            inventaire.getId(),
            List.of(new GapEntryRecord(ligneDeLAutre.getId(), CauseEcart.VOL.name(), null))
        );
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM inventory_gap_analysis"), "le filtre par inventaire tient");
    }

    @Test
    @DisplayName("Le résumé agrège les écarts par cause")
    void resumeParCause() {
        StoreInventory inventaire = inventaire();
        StoreInventoryLine premiere = ligne(inventaire, "VOL 1", 50, 40, 1_000);
        StoreInventoryLine seconde = ligne(inventaire, "VOL 2", 50, 45, 1_000);
        StoreInventoryLine troisieme = ligne(inventaire, "CASSE 1", 50, 47, 1_000);
        services.gapAnalysisService.saveAnalysis(
            inventaire.getId(),
            List.of(
                new GapEntryRecord(premiere.getId(), CauseEcart.VOL.name(), null),
                new GapEntryRecord(seconde.getId(), CauseEcart.VOL.name(), null),
                new GapEntryRecord(troisieme.getId(), CauseEcart.CASSE.name(), null)
            )
        );
        viderLeCache();

        List<GapSummaryRecord> resume = services.gapAnalysisService.getSummary(inventaire.getId());

        assertEquals(2, resume.size());
        GapSummaryRecord vols = resume.stream().filter(r -> r.cause().equals(CauseEcart.VOL.name())).findFirst().orElseThrow();
        assertEquals(2L, vols.nbProduits());
        assertEquals(15L, vols.quantiteTotale(), "10 + 5 unités volées");
        assertTrue(services.gapAnalysisService.hasAnalysis(inventaire.getId()));
    }

    @Test
    @DisplayName("Une charge utile vide ne touche pas la base")
    void chargeUtileVide() {
        StoreInventory inventaire = inventaire();
        StoreInventoryLine ligne = ligne(inventaire, "INTACTE", 20, 15, 600);
        services.gapAnalysisService.saveAnalysis(inventaire.getId(), List.of(new GapEntryRecord(ligne.getId(), CauseEcart.VOL.name(), null)));
        viderLeCache();

        services.gapAnalysisService.saveAnalysis(inventaire.getId(), List.of());
        viderLeCache();

        assertEquals(1, compter("SELECT count(*) FROM inventory_gap_analysis"));
        assertEquals(1, services.gapAnalysisService.getSummary(inventaire.getId()).size());
    }

    // ===== outils =====

    private Pageable page() {
        return PageRequest.of(0, 20);
    }

    private StoreInventory inventaire() {
        StoreInventory inventaire = new StoreInventory();
        inventaire.setCreatedAt(LocalDateTime.now());
        inventaire.setUpdatedAt(LocalDateTime.now());
        inventaire.setInventoryValueCostBegin(0L);
        inventaire.setInventoryAmountBegin(0L);
        inventaire.setInventoryValueCostAfter(0L);
        inventaire.setInventoryAmountAfter(0L);
        inventaire.setUser(utilisateur);
        inventaire.setStorage(rayon);
        em.persist(inventaire);
        em.flush();
        return inventaire;
    }

    /** Une ligne comptée : {@code updated = true} est ce qui la fait entrer dans l'analyse. */
    private StoreInventoryLine ligne(StoreInventory inventaire, String libelle, int theorique, int compte, int prixUnitaire) {
        StoreInventoryLine ligne = ligneNonComptee(inventaire, libelle, theorique, compte);
        ligne.setUpdated(true);
        ligne.setLastUnitPrice(prixUnitaire);
        em.flush();
        return ligne;
    }

    private StoreInventoryLine ligneNonComptee(StoreInventory inventaire, String libelle, int theorique, int compte) {
        Produit produit = produitEnStock(unique(libelle), theorique);
        StoreInventoryLine ligne = new StoreInventoryLine();
        ligne.setStoreInventory(inventaire);
        ligne.setProduit(produit);
        ligne.setStorage(rayon);
        ligne.setQuantityInit(theorique);
        ligne.setQuantityOnHand(compte);
        ligne.setGap(compte - theorique);
        ligne.setUpdated(false);
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        em.flush();
        return ligne;
    }
}

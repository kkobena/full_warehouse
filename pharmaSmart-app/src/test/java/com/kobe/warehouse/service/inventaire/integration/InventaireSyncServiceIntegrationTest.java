package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import com.kobe.warehouse.service.dto.records.BatchSyncResultRecord;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La saisie mobile remonte ses comptages par lots : le poste compte hors ligne, puis synchronise.
 * Ce service n'était couvert par aucun test.
 *
 * <p>Sa promesse est qu'un lot n'est pas tout-ou-rien — une ligne introuvable ou recomptée entre
 * temps par un autre opérateur est écartée, et les autres sont enregistrées quand même. C'est ce
 * qui permet à un poste de resynchroniser sans repartir de zéro.
 */
@DisplayName("InventaireSyncService — synchronisation par lots sur PostgreSQL")
class InventaireSyncServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("un lot de comptages est enregistré et l'écart calculé")
    void lotEnregistre() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        StoreInventoryLine premiere = ligne(inventaire, produitEnStock(unique("DOLIPRANE SYNC"), 30));
        StoreInventoryLine seconde = ligne(inventaire, produitEnStock(unique("EFFERALGAN SYNC"), 12));
        viderLeCache();

        BatchSyncResultRecord resultat = services.inventaireSyncService.synchronize(
            List.of(comptage(premiere, 30, 28), comptage(seconde, 12, 12)));
        viderLeCache();

        assertEquals(2, resultat.saved());
        assertEquals(0, resultat.failed());
        assertEquals(-2, relire(premiere).getGap(), "28 comptés pour 30 théoriques");
        assertEquals(0, relire(seconde).getGap());
        assertTrue(relire(premiere).getUpdated(), "la ligne passe à l'état comptée");
    }

    @Test
    @DisplayName("une ligne introuvable est écartée sans emporter le reste du lot")
    void ligneIntrouvableEcartee() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        StoreInventoryLine valide = ligne(inventaire, produitEnStock(unique("DOLIPRANE VALIDE"), 30));
        viderLeCache();

        StoreInventoryLineDTO fantome = comptage(valide, 30, 25);
        fantome.setId(999_999L);

        BatchSyncResultRecord resultat = services.inventaireSyncService.synchronize(
            List.of(comptage(valide, 30, 25), fantome));
        viderLeCache();

        assertEquals(1, resultat.saved());
        assertEquals(1, resultat.failed());
        assertEquals(List.of(999_999L), resultat.failedIds());
        assertEquals(-5, relire(valide).getGap(), "la ligne valide est bien enregistrée");
    }

    /**
     * Verrou optimiste : si un autre opérateur a recompté la ligne depuis que le poste l'a lue, la
     * saisie du poste est rejetée — mais isolément, pas en emportant le lot.
     */
    @Test
    @DisplayName("une ligne recomptée entre-temps part en conflit, le reste passe")
    void ligneEnConflit() {
        StoreInventory inventaire = inventaire(InventoryCategory.MAGASIN);
        StoreInventoryLine disputee = ligne(inventaire, produitEnStock(unique("DOLIPRANE CONFLIT"), 30));
        StoreInventoryLine paisible = ligne(inventaire, produitEnStock(unique("EFFERALGAN PAISIBLE"), 12));
        viderLeCache();

        StoreInventoryLineDTO saisiePerimee = comptage(disputee, 30, 20);
        saisiePerimee.setVersion(relire(disputee).getVersion() + 1);

        BatchSyncResultRecord resultat = services.inventaireSyncService.synchronize(
            List.of(saisiePerimee, comptage(paisible, 12, 11)));
        viderLeCache();

        assertEquals(1, resultat.saved());
        assertEquals(1, resultat.failed());
        assertEquals(List.of(disputee.getId()), resultat.conflictedIds());
        assertTrue(relire(paisible).getUpdated(), "la ligne sans conflit est enregistrée");
        assertTrue(Boolean.FALSE.equals(relire(disputee).getUpdated()),
            "la ligne en conflit reste à compter");
    }

    @Test
    @DisplayName("un lot vide ne déclenche aucune écriture")
    void lotVide() {
        BatchSyncResultRecord resultat = services.inventaireSyncService.synchronize(List.of());

        assertEquals(0, resultat.saved());
        assertEquals(0, resultat.failed());
        assertTrue(resultat.failedIds().isEmpty());
    }

    // ===== utilitaires =====

    private StoreInventoryLineDTO comptage(StoreInventoryLine ligne, int theorique, int compte) {
        StoreInventoryLineDTO dto = new StoreInventoryLineDTO(relire(ligne));
        dto.setQuantityInit(theorique);
        dto.setQuantityOnHand(compte);
        return dto;
    }

    private StoreInventoryLine relire(StoreInventoryLine ligne) {
        return em.find(StoreInventoryLine.class, ligne.getId());
    }
}

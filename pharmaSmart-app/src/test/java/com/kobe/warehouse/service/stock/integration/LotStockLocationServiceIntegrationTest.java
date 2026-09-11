package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.domain.Produit;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link com.kobe.warehouse.service.stock.LotStockLocationService} sur un vrai PostgreSQL.
 *
 * <p>Ce service tient la table {@code lot_stock_location} : combien d'unités de chaque lot se
 * trouvent à chaque emplacement. Deux choses n'existent nulle part ailleurs que dans la base et
 * doivent donc s'y vérifier — l'ordre FEFO, qui est un {@code ORDER BY} sur la date de péremption
 * avec les lots sans date rejetés en fin, et l'unicité {@code (lot, emplacement)}, qui fait de
 * chaque crédit un « upsert » plutôt qu'une insertion.
 */
@DisplayName("LotStockLocationService — répartition des lots par emplacement sur PostgreSQL")
class LotStockLocationServiceIntegrationTest extends AbstractStockIntegrationTest {

    @Test
    @DisplayName("Créditer un lot inconnu de l'emplacement y crée sa ligne")
    void creditPremierPassage() {
        Produit produit = produitEnStock("DOLIPRANE", 100);
        Lot lot = lot(produit, unique("L"), LocalDate.now().plusMonths(6), 40);

        services.lotStockLocationService.credit(lot, rayon, 40);
        viderLeCache();

        assertEquals(40, quantiteSurEmplacement(lot, rayon));
        assertEquals(1, compter("SELECT count(*) FROM lot_stock_location WHERE lot_id = " + lot.getId()));
    }

    @Test
    @DisplayName("Créditer un lot déjà présent cumule sur la même ligne")
    void creditCumule() {
        Produit produit = produitEnStock("AMOXICILLINE", 100);
        Lot lot = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(6), rayon, 30);

        services.lotStockLocationService.credit(lot, rayon, 25);
        viderLeCache();

        assertEquals(55, quantiteSurEmplacement(lot, rayon));
        assertEquals(
            1,
            compter("SELECT count(*) FROM lot_stock_location WHERE lot_id = " + lot.getId()),
            "la contrainte d'unicité (lot, emplacement) interdit une seconde ligne"
        );
    }

    @Test
    @DisplayName("Un crédit nul ou négatif ne touche pas la base")
    void creditIgnore() {
        Produit produit = produitEnStock("IBUPROFENE", 100);
        Lot lot = lot(produit, unique("L"), LocalDate.now().plusMonths(6), 10);

        services.lotStockLocationService.credit(lot, rayon, 0);
        services.lotStockLocationService.credit(lot, rayon, -5);
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM lot_stock_location WHERE lot_id = " + lot.getId()));
    }

    @Test
    @DisplayName("Débiter partiellement laisse la ligne avec son reliquat")
    void debitPartiel() {
        Produit produit = produitEnStock("PARACETAMOL", 100);
        Lot lot = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(6), rayon, 50);

        services.lotStockLocationService.debit(lot, rayon, 20);
        viderLeCache();

        assertEquals(30, quantiteSurEmplacement(lot, rayon));
    }

    @Test
    @DisplayName("Débiter jusqu'à épuisement efface la ligne plutôt que de la laisser à zéro")
    void debitTotal() {
        Produit produit = produitEnStock("EFFERALGAN", 100);
        Lot lot = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(6), rayon, 50);

        services.lotStockLocationService.debit(lot, rayon, 50);
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM lot_stock_location WHERE lot_id = " + lot.getId()));
    }

    @Test
    @DisplayName("Le débit FEFO entame d'abord le lot qui périme le plus tôt")
    void debitFefoRespecteLOrdreDePeremption() {
        Produit produit = produitEnStock("AUGMENTIN", 100);
        Lot tardif = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(12), rayon, 30);
        Lot proche = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(2), rayon, 20);

        services.lotStockLocationService.debitFefo(produit, rayon, 25);
        viderLeCache();

        assertEquals(-1, quantiteSurEmplacement(proche, rayon), "le lot proche est épuisé puis effacé");
        assertEquals(25, quantiteSurEmplacement(tardif, rayon), "les 5 unités manquantes viennent du lot tardif");
    }

    @Test
    @DisplayName("Un lot sans date de péremption est servi en dernier")
    void debitFefoRejetteLesLotsSansDate() {
        Produit produit = produitEnStock("SMECTA", 100);
        Lot sansDate = lotSurEmplacement(produit, unique("L"), null, rayon, 40);
        Lot date = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(10), rayon, 40);

        services.lotStockLocationService.debitFefo(produit, rayon, 40);
        viderLeCache();

        assertEquals(-1, quantiteSurEmplacement(date, rayon), "le lot daté part en premier");
        assertEquals(40, quantiteSurEmplacement(sansDate, rayon), "le lot sans date est intact");
    }

    @Test
    @DisplayName("Un débit FEFO plus gros que le stock s'arrête sans descendre sous zéro")
    void debitFefoAuDelaDuStock() {
        Produit produit = produitEnStock("SPASFON", 100);
        Lot lot = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(3), rayon, 10);

        services.lotStockLocationService.debitFefo(produit, rayon, 500);
        viderLeCache();

        assertEquals(0, compter("SELECT count(*) FROM lot_stock_location WHERE lot_id = " + lot.getId()));
        assertEquals(
            0,
            compter("SELECT count(*) FROM lot_stock_location WHERE qty < 0"),
            "aucune quantité négative ne subsiste"
        );
    }

    @Test
    @DisplayName("Le transfert FEFO déplace du rayon vers la réserve dans l'ordre de péremption")
    void transfertFefo() {
        Produit produit = produitEnStock("VOGALENE", 100);
        Lot tardif = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(12), rayon, 30);
        Lot proche = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(1), rayon, 20);

        services.lotStockLocationService.transferFefo(produit, rayon, reserve, 25);
        viderLeCache();

        assertEquals(-1, quantiteSurEmplacement(proche, rayon));
        assertEquals(20, quantiteSurEmplacement(proche, reserve), "le lot proche est passé en entier");
        assertEquals(25, quantiteSurEmplacement(tardif, rayon));
        assertEquals(5, quantiteSurEmplacement(tardif, reserve), "le complément vient du lot tardif");
    }

    @Test
    @DisplayName("Un transfert demandant plus que disponible déplace le maximum possible")
    void transfertPlafonneAuDisponible() {
        Produit produit = produitEnStock("MAALOX", 100);
        Lot lot = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(4), rayon, 15);

        services.lotStockLocationService.transferFefo(produit, rayon, reserve, 60);
        viderLeCache();

        assertEquals(15, quantiteSurEmplacement(lot, reserve));
        assertEquals(-1, quantiteSurEmplacement(lot, rayon), "la ligne source épuisée est effacée");
    }

    @Test
    @DisplayName("Le crédit du dernier lot reçu vise celui qui est entré le plus récemment")
    void creditDuDernierLotRecu() {
        Produit produit = produitEnStock("GAVISCON", 100);
        Lot ancien = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(6), rayon, 10);
        // createdDate est posé par la fabrique à l'instant de l'appel : le second lot est le plus récent.
        Lot recent = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(9), rayon, 10);

        services.lotStockLocationService.creditLastLot(produit, rayon, 7);
        viderLeCache();

        assertEquals(17, quantiteSurEmplacement(recent, rayon));
        assertEquals(10, quantiteSurEmplacement(ancien, rayon));
    }

    @Test
    @DisplayName("La restauration après annulation de vente recrée la ligne d'un lot épuisé")
    void restaurationDUnLotEpuise() {
        Produit produit = produitEnStock("BETADINE", 100);
        Lot lot = lot(produit, unique("L"), LocalDate.now().plusMonths(6), 12);

        services.lotStockLocationService.creditFromSold(List.of(new LotSold(lot.getId(), lot.getNumLot(), 12, lot.getExpiryDate())), rayon);
        viderLeCache();

        assertEquals(12, quantiteSurEmplacement(lot, rayon), "la ligne est recréée pour le lot qui n'en avait plus");
    }

    @Test
    @DisplayName("La restauration cumule sur la ligne existante et ignore les quantités nulles")
    void restaurationCumulee() {
        Produit produit = produitEnStock("HEXTRIL", 100);
        Lot servi = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(6), rayon, 5);
        Lot intact = lotSurEmplacement(produit, unique("L"), LocalDate.now().plusMonths(7), rayon, 5);

        services.lotStockLocationService.creditFromSold(
            List.of(new LotSold(servi.getId(), servi.getNumLot(), 3, servi.getExpiryDate()), new LotSold(intact.getId(), intact.getNumLot(), 0, intact.getExpiryDate())),
            rayon
        );
        viderLeCache();

        assertEquals(8, quantiteSurEmplacement(servi, rayon));
        assertEquals(5, quantiteSurEmplacement(intact, rayon), "une quantité nulle ne déclenche aucune écriture");
    }

    @Test
    @DisplayName("Les emplacements d'un lot se relisent triés, le rayon avant la réserve")
    void lecturesDesEmplacementsDUnLot() {
        Produit produit = produitEnStock("DAFALGAN", 100);
        Lot lot = lot(produit, unique("L"), LocalDate.now().plusMonths(6), 30);
        emplacement(lot, reserve, 20);
        emplacement(lot, rayon, 10);
        viderLeCache();

        var emplacements = services.lotStockLocationRepository.findAvailableByLotId(lot.getId());

        assertEquals(2, emplacements.size());
        assertEquals(STORAGE_RAYON_ID, emplacements.getFirst().getStorage().getId(), "le rayon (PRINCIPAL) vient en tête");
        assertEquals(30, services.lotStockLocationRepository.sumQtyByLot(lot.getId()));
        assertTrue(emplacements.stream().allMatch(e -> e.getQty() > 0));
    }
}

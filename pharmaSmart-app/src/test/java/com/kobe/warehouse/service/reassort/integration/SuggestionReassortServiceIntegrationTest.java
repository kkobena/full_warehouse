package com.kobe.warehouse.service.reassort.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.LigneReassort;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.SuggestionReassort;
import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.service.reassort.dto.LigneReassortDto;
import com.kobe.warehouse.service.reassort.dto.ReassortRecord;
import com.kobe.warehouse.service.reassort.dto.SuggestionReassortDto;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Une suggestion de réassort est un document vivant : la même suggestion ouverte accueille toutes
 * les détections du jour, et la table {@code ligne_reassort} porte une contrainte d'unicité sur
 * le couple (suggestion, stock). Deux détections successives du même produit doivent donc mettre
 * à jour la ligne existante — une seconde insertion ferait remonter une violation de contrainte
 * au beau milieu d'une réception de commande.
 *
 * <p>Le reste tient à la numérotation, qui passe par la séquence {@code reference} en base, et à
 * l'enchaînement détection → validation → mouvement de stock, que seul un vrai schéma permet
 * d'observer de bout en bout.
 */
@DisplayName("SuggestionReassortService — suggestions de réassort sur PostgreSQL")
class SuggestionReassortServiceIntegrationTest extends AbstractReassortIntegrationTest {

    // ===== détection à la réception =====

    @Test
    @DisplayName("Une réception sous le seuil de réserve ouvre une suggestion numérotée")
    void detectionALaReception() {
        Produit produit = produit(unique("DOLIPRANE"));
        stock(produit, rayon, 60);
        StockProduit stockReserve = stock(produit, reserve, 4);
        stockReserve.setSeuilMini(20);
        viderLeCache();

        services.suggestionReassortService.createLigneReassort(
            List.of(new ReassortRecord(services.stockProduitRepository.findById(stockReserve.getId()).orElseThrow(), 50)),
            utilisateur
        );
        viderLeCache();

        SuggestionReassort suggestion = suggestionOuverteEnBase(TypeReassort.RESERVE);
        assertNotNull(suggestion.getReference(), "la suggestion est numérotée par le service de référence");
        assertEquals(MAGASIN_ID, suggestion.getMagasin().getId());
        assertEquals(utilisateur.getId(), suggestion.getLastUserEdit().getId());

        List<LigneReassort> lignes = List.copyOf(suggestion.getLigneReassorts());
        assertEquals(1, lignes.size());
        assertEquals(20 - 4, lignes.getFirst().getQuantity());
        assertEquals(stockReserve.getId(), lignes.getFirst().getStockProduit().getId());
    }

    @Test
    @DisplayName("Une seconde détection du même stock met à jour la ligne au lieu d'en insérer une seconde")
    void secondeDetectionSurLaMemeLigne() {
        Produit produit = produit(unique("DOLIPRANE"));
        stock(produit, rayon, 60);
        StockProduit stockReserve = stock(produit, reserve, 4);
        stockReserve.setSeuilMini(20);
        viderLeCache();

        services.suggestionReassortService.createLigneReassort(
            List.of(new ReassortRecord(services.stockProduitRepository.findById(stockReserve.getId()).orElseThrow(), 50)),
            utilisateur
        );
        viderLeCache();
        services.suggestionReassortService.createLigneReassort(
            List.of(new ReassortRecord(services.stockProduitRepository.findById(stockReserve.getId()).orElseThrow(), 6)),
            utilisateur
        );
        viderLeCache();

        assertEquals(
            1,
            compter("SELECT COUNT(*) FROM ligne_reassort WHERE stock_produit_id = " + stockReserve.getId()),
            "la contrainte d'unicité (suggestion, stock) tient"
        );
        assertEquals(
            6,
            compter("SELECT quantity FROM ligne_reassort WHERE stock_produit_id = " + stockReserve.getId()),
            "la quantité est ramenée à ce que la seconde réception apporte"
        );
    }

    @Test
    @DisplayName("Un stock de réserve déjà au-dessus de son seuil n'ajoute aucune ligne")
    void reserveDejaSuffisante() {
        Produit produit = produit(unique("BIEN APPROVISIONNE"));
        stock(produit, rayon, 60);
        StockProduit stockReserve = stock(produit, reserve, 30);
        stockReserve.setSeuilMini(20);
        viderLeCache();

        services.suggestionReassortService.createLigneReassort(
            List.of(new ReassortRecord(services.stockProduitRepository.findById(stockReserve.getId()).orElseThrow(), 50)),
            utilisateur
        );
        viderLeCache();

        assertTrue(suggestionOuverteEnBase(TypeReassort.RESERVE).getLigneReassorts().isEmpty());
    }

    // ===== détection depuis le stock =====

    @Test
    @DisplayName("Un rayon au-dessus de son stock maxi propose son surplus à une réserve dégarnie")
    void detectionDuSurplusRayon() {
        Produit produit = produit(unique("SURPLUS"));
        StockProduit stockRayon = stock(produit, rayon, 80);
        stockRayon.setStockMaxi(50);
        StockProduit stockReserve = stock(produit, reserve, 2);
        stockReserve.setSeuilMini(20);
        viderLeCache();

        services.suggestionReassortService.createReserveSuggestionReassort(
            services.stockProduitRepository.findById(stockRayon.getId()).orElseThrow()
        );
        viderLeCache();

        SuggestionReassort suggestion = suggestionOuverteEnBase(TypeReassort.RESERVE);
        LigneReassort ligne = List.copyOf(suggestion.getLigneReassorts()).getFirst();
        assertEquals(30, ligne.getQuantity(), "80 - 50");
        assertEquals(stockReserve.getId(), ligne.getStockProduit().getId(), "la ligne porte le stock à recompléter");
    }

    @Test
    @DisplayName("Un rayon sous son seuil de réassort demande du renfort à la réserve")
    void detectionDuRayonADegarni() {
        Produit produit = produit(unique("A RECOMPLETER"));
        StockProduit stockRayon = stock(produit, rayon, 3);
        stockRayon.setStockReassort(30);
        stock(produit, reserve, 12);
        viderLeCache();

        services.suggestionReassortService.createRayonSuggestionReassort(
            services.stockProduitRepository.findById(stockRayon.getId()).orElseThrow()
        );
        viderLeCache();

        SuggestionReassort suggestion = suggestionOuverteEnBase(TypeReassort.RAYON);
        LigneReassort ligne = List.copyOf(suggestion.getLigneReassorts()).getFirst();
        assertEquals(12, ligne.getQuantity(), "la réserve ne contient que 12");
        assertEquals(stockRayon.getId(), ligne.getStockProduit().getId());
    }

    // ===== consultation =====

    @Test
    @DisplayName("Les suggestions ouvertes remontent avec le détail lisible de chaque ligne")
    void consultationDesSuggestionsOuvertes() {
        Produit produit = produit(unique("A CONSULTER"));
        stock(produit, rayon, 60);
        StockProduit stockReserve = stock(produit, reserve, 4);
        stockReserve.setSeuilMini(20);
        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RESERVE);
        ligne(suggestion, stockReserve, 16);
        viderLeCache();

        List<SuggestionReassortDto> reserve = services.suggestionReassortService.getOpenningSuggestions(TypeReassort.RESERVE);

        assertEquals(1, reserve.size());
        SuggestionReassortDto dto = reserve.getFirst();
        assertEquals(suggestion.getReference(), dto.getReference());
        assertEquals(StatutReassort.OPEN, dto.getStatut());
        assertNotNull(dto.getUserFullName());

        LigneReassortDto ligne = dto.getLigneReassorts().getFirst();
        assertEquals(16, ligne.getQuantity());
        assertEquals("Stock réserve", ligne.getStorageName());
        assertEquals(produit.getLibelle(), ligne.getProduitLibelle());
        assertEquals(produit.getFournisseurProduitPrincipal().getCodeCip(), ligne.getProduitCode());
        assertEquals(20, ligne.getSeuilMini());
        assertEquals(4, ligne.getStockActuel());

        assertTrue(
            services.suggestionReassortService.getOpenningSuggestions(TypeReassort.RAYON).isEmpty(),
            "le filtre par type ne mélange pas les deux sens de réassort"
        );
        assertEquals(1, services.suggestionReassortService.getOpenningSuggestions(null).size());
    }

    @Test
    @DisplayName("Une suggestion close ne remonte plus dans les suggestions ouvertes")
    void suggestionCloseInvisible() {
        Produit produit = produit(unique("DEJA TRAITE"));
        stock(produit, rayon, 60);
        StockProduit stockReserve = stock(produit, reserve, 4);
        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RESERVE);
        ligne(suggestion, stockReserve, 16);
        suggestion.setStatut(StatutReassort.CLOSED);
        viderLeCache();

        assertTrue(services.suggestionReassortService.getOpenningSuggestions(null).isEmpty());
    }

    // ===== cycle de vie =====

    @Test
    @DisplayName("Corriger puis supprimer une ligne se répercute en base")
    void correctionEtSuppressionDuneLigne() {
        Produit produit = produit(unique("A CORRIGER"));
        stock(produit, rayon, 60);
        StockProduit stockReserve = stock(produit, reserve, 4);
        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RESERVE);
        LigneReassort ligne = ligne(suggestion, stockReserve, 16);
        viderLeCache();

        services.suggestionReassortService.updateLigneReassort(ligne.getId(), 25);
        viderLeCache();
        assertEquals(25, compter("SELECT quantity FROM ligne_reassort WHERE id = " + ligne.getId()));

        services.suggestionReassortService.deleteLigneReassort(ligne.getId());
        viderLeCache();
        assertEquals(0, compter("SELECT COUNT(*) FROM ligne_reassort WHERE id = " + ligne.getId()));
    }

    @Test
    @DisplayName("Supprimer une suggestion emporte ses lignes")
    void suppressionEnCascade() {
        Produit produit = produit(unique("A JETER"));
        stock(produit, rayon, 60);
        StockProduit stockReserve = stock(produit, reserve, 4);
        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RESERVE);
        ligne(suggestion, stockReserve, 16);
        viderLeCache();

        services.suggestionReassortService.deleteSuggestionReassort(suggestion.getId());
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM suggestion_reassort WHERE id = " + suggestion.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM ligne_reassort WHERE reassort_id = " + suggestion.getId()));
    }

    @Test
    @DisplayName("Valider une suggestion réserve déplace le stock du rayon et clôt le document")
    void validationDUneSuggestionReserve() {
        Produit produit = produit(unique("A VALIDER"));
        stock(produit, rayon, 80);
        StockProduit stockReserve = stock(produit, reserve, 2);
        lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(5), rayon, 80);
        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RESERVE);
        ligne(suggestion, stockReserve, 30);
        viderLeCache();

        services.suggestionReassortService.validateSuggestionReassort(suggestion.getId());
        viderLeCache();

        assertEquals(50, stockEnBase(produit, rayon));
        assertEquals(32, stockEnBase(produit, reserve));
        assertEquals(2, mouvementsJournalises(produit));
        assertEquals(StatutReassort.CLOSED, services.suggestionReassortRepository.findById(suggestion.getId()).orElseThrow().getStatut());
    }

    @Test
    @DisplayName("Valider une suggestion déjà close ne rejoue pas les mouvements")
    void validationIdempotente() {
        Produit produit = produit(unique("DEJA VALIDE"));
        stock(produit, rayon, 80);
        StockProduit stockReserve = stock(produit, reserve, 2);
        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RESERVE);
        ligne(suggestion, stockReserve, 30);
        suggestion.setStatut(StatutReassort.CLOSED);
        viderLeCache();

        services.suggestionReassortService.validateSuggestionReassort(suggestion.getId());
        viderLeCache();

        assertEquals(80, stockEnBase(produit, rayon));
        assertEquals(2, stockEnBase(produit, reserve));
        assertEquals(0, mouvementsJournalises(produit));
    }

    // ===== exécution automatique du surplus de réception =====

    @Test
    @DisplayName("À la réception, le surplus des produits reçus descend seul en réserve et la ligne disparaît")
    void executionAutomatiqueDuSurplus() {
        Produit recu = produit(unique("RECU"));
        stock(recu, rayon, 80);
        StockProduit reserveRecu = stock(recu, reserve, 2);
        lotSurEmplacement(recu, unique("LOT"), LocalDate.now().plusMonths(5), rayon, 80);

        Produit absent = produit(unique("NON RECU"));
        stock(absent, rayon, 50);
        StockProduit reserveAbsent = stock(absent, reserve, 1);

        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RESERVE);
        ligne(suggestion, reserveRecu, 30);
        ligne(suggestion, reserveAbsent, 5);
        viderLeCache();

        services.suggestionReassortService.autoExecuteOverflowForProducts(Set.of(recu.getId()));
        viderLeCache();

        assertEquals(50, stockEnBase(recu, rayon));
        assertEquals(32, stockEnBase(recu, reserve));
        assertEquals(50, stockEnBase(absent, rayon), "le produit non reçu n'a pas bougé");
        assertEquals(1, stockEnBase(absent, reserve));

        assertEquals(
            1,
            compter("SELECT COUNT(*) FROM ligne_reassort WHERE reassort_id = " + suggestion.getId()),
            "la ligne exécutée quitte la suggestion"
        );
        SuggestionReassort relue = services.suggestionReassortRepository.findById(suggestion.getId()).orElseThrow();
        assertEquals(StatutReassort.OPEN, relue.getStatut(), "il reste une ligne à traiter");
        assertFalse(relue.getLigneReassorts().isEmpty());
    }

    @Test
    @DisplayName("Une suggestion entièrement exécutée se clôt d'elle-même")
    void suggestionVideeEstClose() {
        Produit recu = produit(unique("RECU"));
        stock(recu, rayon, 80);
        StockProduit reserveRecu = stock(recu, reserve, 2);
        lotSurEmplacement(recu, unique("LOT"), LocalDate.now().plusMonths(5), rayon, 80);
        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RESERVE);
        ligne(suggestion, reserveRecu, 30);
        viderLeCache();

        services.suggestionReassortService.autoExecuteOverflowForProducts(Set.of(recu.getId()));
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM ligne_reassort WHERE reassort_id = " + suggestion.getId()));
        assertEquals(StatutReassort.CLOSED, services.suggestionReassortRepository.findById(suggestion.getId()).orElseThrow().getStatut());
    }

    @Test
    @DisplayName("Sans produit reçu, l'exécution automatique ne touche à rien")
    void executionAutomatiqueSansProduit() {
        Produit produit = produit(unique("INTACT"));
        stock(produit, rayon, 80);
        StockProduit stockReserve = stock(produit, reserve, 2);
        SuggestionReassort suggestion = suggestionOuverte(TypeReassort.RESERVE);
        ligne(suggestion, stockReserve, 30);
        viderLeCache();

        services.suggestionReassortService.autoExecuteOverflowForProducts(Set.of());
        viderLeCache();

        assertEquals(80, stockEnBase(produit, rayon));
        assertEquals(1, compter("SELECT COUNT(*) FROM ligne_reassort WHERE reassort_id = " + suggestion.getId()));
        assertEquals(StatutReassort.OPEN, services.suggestionReassortRepository.findById(suggestion.getId()).orElseThrow().getStatut());
    }

    // ===== outils =====

    private SuggestionReassort suggestionOuverteEnBase(TypeReassort typeReassort) {
        return services.suggestionReassortRepository
            .findOneByStatutAndMagasinIdAndTypeReassort(StatutReassort.OPEN, MAGASIN_ID, typeReassort)
            .orElseThrow(() -> new AssertionError("aucune suggestion ouverte de type " + typeReassort));
    }
}

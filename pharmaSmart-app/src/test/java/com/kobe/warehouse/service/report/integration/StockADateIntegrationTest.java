package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Produit;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Stock et valorisation à un instant T (migration V2.1.33) : départ d'une photo antérieure, sinon remontée depuis le stock
 * courant ; tous les mouvements comptent, inventaires compris ; le négatif est conservé ; les UG viennent de la photo.
 */
@DisplayName("Stock à date — journal des mouvements et photos")
class StockADateIntegrationTest extends AbstractReportIntegrationTest {

    // Les partitions de inventory_transaction du conteneur de test ne couvrent que l'année en cours.
    private static final LocalDateTime T = LocalDate.now().withMonth(1).withDayOfMonth(15).atTime(12, 0);

    private static final DateTimeFormatter INSTANT_SQL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Produit produit;

    @BeforeEach
    void creerLeProduit() {
        produit = produitAnalysable(unique("STOCK A DATE"), 1_000);
        referencement(produit, fournisseur(unique("FRS STOCK")));
    }

    @Test
    @DisplayName("sans photo antérieure, le stock à T se déduit du stock courant en retirant les mouvements postérieurs")
    void remonteDepuisLeStockCourant() {
        stock(produit, 10);
        mouvement("SALE", T.plusHours(1), 12, 10);

        assertThat(stockA(T)).containsExactly(12, 0);
    }

    @Test
    @DisplayName("depuis une photo antérieure, on ajoute les mouvements jusqu'à T ; les UG sont celles de la photo")
    void avanceDepuisUnePhoto() {
        stock(produit, 40);
        photo(T.minusDays(1), 20, 2);
        mouvement("ENTREE_STOCK", T.minusHours(1), 20, 25);
        mouvement("SALE", T.plusHours(1), 25, 40);

        assertThat(stockA(T)).containsExactly(25, 2);
    }

    @Test
    @DisplayName("un écart d'inventaire compte comme un mouvement")
    void compteLInventaire() {
        stock(produit, 7);
        mouvement("INVENTAIRE", T.plusHours(2), 10, 7);

        assertThat(stockA(T)).containsExactly(10, 0);
    }

    @Test
    @DisplayName("le stock négatif est conservé, pas ramené à zéro")
    void conserveLeNegatif() {
        stock(produit, 1);
        mouvement("ENTREE_STOCK", T.plusHours(1), -2, 1);

        assertThat(stockA(T)).containsExactly(-2, 0);
    }

    @Test
    @DisplayName("la valorisation porte sur stock + UG, au prix du dernier mouvement avant T")
    void valoriseStockEtUgAuDernierPrix() {
        stock(produit, 30);
        photo(T.minusDays(1), 10, 5);
        mouvement("ENTREE_STOCK", T.minusHours(1), 10, 30, 700);

        Object[] valeur = (Object[]) em
            .createNativeQuery(
                "SELECT qty_stock, qty_ug, prix_achat, valeur_achat FROM fn_stock_valuation_at_time(:produit, :storage, " + instantUtc(T) + ")"
            )
            .setParameter("produit", produit.getId())
            .setParameter("storage", rayon.getId())
            .getSingleResult();

        assertThat(entiers(valeur)).containsExactly(30, 5, 700, 35 * 700);
    }

    private int[] stockA(LocalDateTime instant) {
        em.flush();
        Object[] ligne = (Object[]) em
            .createNativeQuery(
                "SELECT qty_stock, qty_ug FROM fn_stock_quantites_at_time(" + instantUtc(instant) + ", NULL, ARRAY[CAST(:produit AS int)])"
            )
            .setParameter("produit", produit.getId())
            .getSingleResult();
        return entiers(ligne);
    }

    private void photo(LocalDateTime instant, int quantite, int ug) {
        em.flush();
        em
            .createNativeQuery(
                "INSERT INTO stock_produit_snapshot (produit_id, storage_id, snapshot_date, qty_stock, qty_ug, source_type) " +
                "VALUES (:produit, :storage, " + instantUtc(instant) + ", :quantite, :ug, 'BATCH_QUOTIDIEN')"
            )
            .setParameter("produit", produit.getId())
            .setParameter("storage", rayon.getId())
            .setParameter("quantite", quantite)
            .setParameter("ug", ug)
            .executeUpdate();
    }

    private void mouvement(String type, LocalDateTime instant, int avant, int apres) {
        mouvement(type, instant, avant, apres, produit.getCostAmount());
    }

    private void mouvement(String type, LocalDateTime instant, int avant, int apres, int prixAchat) {
        em.flush();
        em
            .createNativeQuery(
                "INSERT INTO inventory_transaction (id, transaction_date, created_at, mouvement_type, quantity, quantity_befor, " +
                "quantity_after, cost_amount, regular_unit_price, entity_id, produit_id, user_id, magasin_id, storage_id) " +
                "VALUES (nextval('id_mvt_produit_seq'), DATE '" + instant.toLocalDate() + "', TIMESTAMP '" + INSTANT_SQL.format(instant) +
                "', :type, :quantite, :avant, :apres, :prix, 1000, " +
                "nextval('id_mvt_produit_seq'), :produit, :user, :magasin, :storage)"
            )
            .setParameter("type", type)
            .setParameter("quantite", Math.abs(apres - avant))
            .setParameter("avant", avant)
            .setParameter("apres", apres)
            .setParameter("prix", prixAchat)
            .setParameter("produit", produit.getId())
            .setParameter("user", utilisateur.getId())
            .setParameter("magasin", magasin.getId())
            .setParameter("storage", rayon.getId())
            .executeUpdate();
    }

    /** Instant écrit en UTC dans la requête : created_at est stocké en UTC, sans fuseau. */
    private static String instantUtc(LocalDateTime instant) {
        return "TIMESTAMPTZ '" + INSTANT_SQL.format(instant) + "+00'";
    }

    private static int[] entiers(Object[] colonnes) {
        int[] valeurs = new int[colonnes.length];
        for (int i = 0; i < colonnes.length; i++) {
            valeurs[i] = ((Number) colonnes[i]).intValue();
        }
        return valeurs;
    }
}

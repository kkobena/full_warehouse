package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.service.dto.ProduitIndicateursDTO;
import com.kobe.warehouse.service.dto.VenteMoisDTO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les indicateurs d'une fiche article ne sont pas calculés en Java : ils sont lus dans deux vues
 * Postgres — {@code v_stock_rotation} pour la rotation et le chiffre d'affaires,
 * {@code v_abc_pareto_analysis} pour le classement — puis assemblés à partir de tableaux
 * {@code Object[]} positionnels. Rien de tout cela n'existe hors de la base : ni les vues, ni
 * l'ordre des colonnes dont dépend l'assemblage, ni les types que Postgres renvoie et qu'il faut
 * convertir.
 *
 * <p>C'est précisément le genre de lecture qui casse en silence quand une colonne est insérée au
 * milieu d'une vue : le DTO se remplit toujours, mais avec les mauvaises valeurs au mauvais
 * endroit.
 */
@DisplayName("ProduitIndicateursService — indicateurs de fiche article sur PostgreSQL")
class ProduitIndicateursServiceIntegrationTest extends AbstractStockIntegrationTest {

    @Test
    @DisplayName("Un produit sans historique de vente rend des indicateurs à zéro, pas une absence")
    void produitSansHistorique() {
        Produit produit = produit(unique("JAMAIS VENDU"), 10_000, 6_000);
        fournisseurProduit(produit);
        stock(produit, rayon, 25);
        viderLeCache();

        ProduitIndicateursDTO indicateurs = services.produitIndicateursService.getIndicateurs(produit.getId()).orElseThrow();

        assertEquals(produit.getId(), indicateurs.produitId());
        assertEquals(0, BigDecimal.ZERO.compareTo(indicateurs.cmm()), "aucune vente, consommation moyenne nulle");
        assertEquals(0, indicateurs.ca30Jours());
        assertEquals(0, indicateurs.ca12Mois());
        assertEquals(0, indicateurs.qteVendue12Mois());
        assertNull(indicateurs.rang(), "sans vente, le produit n'est pas classé au Pareto");
        assertNull(indicateurs.caCumulePct());
    }

    @Test
    @DisplayName("Le taux de marge se calcule sur le prix de vente, arrondi au centième")
    void tauxDeMarge() {
        Produit produit = produit(unique("AVEC MARGE"), 10_000, 6_000);
        fournisseurProduit(produit);
        stock(produit, rayon, 10);
        viderLeCache();

        ProduitIndicateursDTO indicateurs = services.produitIndicateursService.getIndicateurs(produit.getId()).orElseThrow();

        assertEquals(new BigDecimal("40.00"), indicateurs.tauxMarge(), "(10 000 - 6 000) / 10 000");
    }

    @Test
    @DisplayName("Un prix de vente nul ne donne pas de taux de marge plutôt qu'une division par zéro")
    void prixDeVenteNul() {
        Produit produit = produit(unique("GRATUIT"), 0, 0);
        fournisseurProduit(produit);
        stock(produit, rayon, 10);
        viderLeCache();

        assertNull(services.produitIndicateursService.getIndicateurs(produit.getId()).orElseThrow().tauxMarge());
    }

    @Test
    @DisplayName("La classification et les badges réglementaires sont relus depuis la fiche")
    void classificationEtBadges() {
        Produit produit = produit(unique("ESSENTIEL"), 10_000, 6_000);
        produit.setClasseCriticite(ClasseCriticite.A);
        produit.setEstMedicamentEssentiel(true);
        produit.setEstProduitGarde(true);
        fournisseurProduit(produit);
        stock(produit, rayon, 10);
        viderLeCache();

        ProduitIndicateursDTO indicateurs = services.produitIndicateursService.getIndicateurs(produit.getId()).orElseThrow();

        assertEquals(ClasseCriticite.A, indicateurs.classeCriticite());
        assertTrue(indicateurs.estMedicamentEssentiel());
        assertTrue(indicateurs.estProduitGarde());
    }

    @Test
    @DisplayName("Sans classe de criticité saisie, le produit est rangé en B par défaut")
    void classeParDefaut() {
        Produit produit = produit(unique("SANS CLASSE"), 10_000, 6_000);
        produit.setClasseCriticite(null);
        fournisseurProduit(produit);
        stock(produit, rayon, 10);
        viderLeCache();

        assertEquals(ClasseCriticite.B, services.produitIndicateursService.getIndicateurs(produit.getId()).orElseThrow().classeCriticite());
    }

    @Test
    @DisplayName("Un produit inconnu ne rend aucun indicateur")
    void produitInconnu() {
        assertTrue(services.produitIndicateursService.getIndicateurs(999_999).isEmpty());
    }

    // ===== historique mensuel =====

    @Test
    @DisplayName("L'historique mensuel remonte les N derniers mois, rendus du plus ancien au plus récent")
    void historiqueMensuel() {
        Produit produit = produit(unique("A HISTORIQUE"), 10_000, 6_000);
        fournisseurProduit(produit);
        stock(produit, rayon, 10);
        venteMensuelle(produit, "2026-01", 10, 100_000, 4);
        venteMensuelle(produit, "2026-02", 7, 70_000, 3);
        venteMensuelle(produit, "2026-03", 12, 120_000, 5);
        viderLeCache();

        List<VenteMoisDTO> historique = services.produitIndicateursService.getVentesMensuelles(produit.getId(), 12);

        assertEquals(List.of("2026-01", "2026-02", "2026-03"), historique.stream().map(VenteMoisDTO::anneeMois).toList());
        VenteMoisDTO janvier = historique.getFirst();
        assertEquals(10, janvier.quantiteVendue());
        assertEquals(100_000, janvier.montantCa());
        assertEquals(4, janvier.nombreVentes());
    }

    @Test
    @DisplayName("La fenêtre demandée garde les mois les plus récents, quel que soit l'ordre rendu")
    void fenetreLimiteeAuxMoisRecents() {
        Produit produit = produit(unique("TRONQUE"), 10_000, 6_000);
        fournisseurProduit(produit);
        stock(produit, rayon, 10);
        venteMensuelle(produit, "2026-01", 1, 1_000, 1);
        venteMensuelle(produit, "2026-02", 2, 2_000, 1);
        venteMensuelle(produit, "2026-03", 3, 3_000, 1);
        viderLeCache();

        List<VenteMoisDTO> deuxDerniers = services.produitIndicateursService.getVentesMensuelles(produit.getId(), 2);

        assertEquals(List.of("2026-02", "2026-03"), deuxDerniers.stream().map(VenteMoisDTO::anneeMois).toList());
    }

    @Test
    @DisplayName("Un produit sans agrégat mensuel rend un historique vide")
    void historiqueVide() {
        Produit produit = produit(unique("SANS HISTORIQUE"), 10_000, 6_000);
        fournisseurProduit(produit);
        stock(produit, rayon, 10);
        viderLeCache();

        assertTrue(services.produitIndicateursService.getVentesMensuelles(produit.getId(), 12).isEmpty());
    }

    // ===== fabriques =====

    private void venteMensuelle(Produit produit, String anneeMois, int quantite, int montant, int nombreVentes) {
        VentesMensuellesAgregees ventes = new VentesMensuellesAgregees();
        ventes.setProduit(produit);
        ventes.setAnneeMois(anneeMois);
        ventes.setQuantiteVendue(quantite);
        ventes.setMontantCa(montant);
        ventes.setNombreVentes(nombreVentes);
        em.persist(ventes);
        em.flush();
    }
}

package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import jakarta.persistence.Tuple;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les écrans de pilotage ne lisent presque jamais les tables : ils lisent des vues
 * matérialisées, dont le SQL vit dans les migrations Flyway et non dans le code Java. Rien côté
 * application ne garantit qu'une vue redéfinie continue de dire la même chose qu'une autre.
 *
 * <p>Ces identités ont été vérifiées à la main sur la base de démonstration, et elles tenaient.
 * Une vérification à la main ne prévient de rien : elle était vraie ce jour-là. Les voici
 * rejouées à chaque build, sur des données que le test crée lui-même — ce qui les rend
 * indépendantes du jeu de démonstration.
 */
@DisplayName("Vues du pilotage — identités comptables entre agrégats")
class CoherenceDesVuesIntegrationTest extends AbstractReportIntegrationTest {

    /**
     * La marge est ce qui reste du prix de vente une fois le coût déduit. Si la vue cessait de
     * calculer l'un des deux termes sur la même assiette, l'identité se romprait — et le
     * tableau de bord afficherait une marge que son propre chiffre d'affaires contredit.
     */
    @Test
    @DisplayName("marge brute + coût = CA brut, jour par jour")
    void margePlusCoutEgaleLeCaBrut() {
        Produit produit = produitAnalysable(unique("DOLIPRANE IDENTITE"), 1_000);
        vendu(produit, 7, hier());
        vendu(produit, 3, avantHier());
        rafraichir("mv_dashboard_ca_daily");

        for (Tuple jour : lignes(
            """
            SELECT sale_date, ca_total, cout_total, marge_brute
            FROM mv_dashboard_ca_daily
            WHERE sale_date BETWEEN :debut AND :fin
            """)) {
            assertThat(decimal(jour, "marge_brute").add(decimal(jour, "cout_total")))
                .as("journée %s", jour.get("sale_date"))
                .isEqualByComparingTo(decimal(jour, "ca_total"));
        }
    }

    /**
     * Deux vues décrivent les mêmes ventes sous deux angles : par journée et par famille de
     * produits. Elles partent de la même source, donc leurs totaux doivent coïncider. Sinon
     * l'écran « répartition par famille » et le chiffre d'affaires du jour se contredisent,
     * chacun paraissant juste pris isolément.
     */
    @Test
    @DisplayName("la vue par famille totalise ce que la vue journalière annonce")
    void familleEtJourneeSAccordent() {
        Produit produit = produitAnalysable(unique("DOLIPRANE FAMILLE"), 1_500);
        vendu(produit, 4, hier());
        rafraichir("mv_dashboard_ca_daily");
        rafraichir("mv_dashboard_ca_product_families");

        Tuple totaux = ligne(
            """
            SELECT
              (SELECT COALESCE(sum(ca_total), 0) FROM mv_dashboard_ca_daily
                WHERE sale_date BETWEEN :debut AND :fin)            AS ca_journalier,
              (SELECT COALESCE(sum(ca_total), 0) FROM mv_dashboard_ca_product_families
                WHERE sale_date BETWEEN :debut AND :fin)            AS ca_familles,
              (SELECT COALESCE(sum(marge_brute), 0) FROM mv_dashboard_ca_daily
                WHERE sale_date BETWEEN :debut AND :fin)            AS marge_journaliere,
              (SELECT COALESCE(sum(marge_brute), 0) FROM mv_dashboard_ca_product_families
                WHERE sale_date BETWEEN :debut AND :fin)            AS marge_familles
            """);

        assertThat(decimal(totaux, "ca_familles"))
            .as("chiffre d'affaires")
            .isEqualByComparingTo(decimal(totaux, "ca_journalier"));
        assertThat(decimal(totaux, "marge_familles"))
            .as("marge brute")
            .isEqualByComparingTo(decimal(totaux, "marge_journaliere"));
    }

    /**
     * La valorisation du stock annonce trois colonnes dont la troisième se déduit des deux
     * autres. C'est le chiffre qu'on présente à l'expert-comptable : une marge potentielle qui
     * ne serait pas l'écart entre la valeur de vente et la valeur d'achat n'aurait aucun sens,
     * et personne ne recalculerait à la main.
     */
    @Test
    @DisplayName("valeur de vente − valeur d'achat = marge potentielle")
    void margePotentielleEstLEcartDesValeurs() {
        Produit produit = produitAnalysable(unique("DOLIPRANE VALO"), 2_000);
        stock(produit, 25);
        rafraichir("mv_stock_valuation");

        for (Tuple ligne : lignes(
            """
            SELECT produit_id, total_sales_value, total_purchase_value, potential_margin
            FROM mv_stock_valuation
            """)) {
            assertThat(decimal(ligne, "total_sales_value").subtract(decimal(ligne, "total_purchase_value")))
                .as("produit %s", ligne.get("produit_id"))
                .isEqualByComparingTo(decimal(ligne, "potential_margin"));
        }
    }

    /**
     * La répartition par mode de règlement doit totaliser ce que la caisse a effectivement
     * encaissé. Deux lectures de la même réalité : la vue agrège, la table énumère. Si elles
     * divergent, l'écran des modes de paiement et le journal de caisse se contredisent.
     */
    @Test
    @DisplayName("la vue des modes de règlement totalise les encaissements")
    void modesDeReglementTotalisentLesEncaissements() {
        CashRegister caisse = caisseOuverte();
        CashSale vente = venteFermee(hier());
        reglement(vente, caisse, "CASH", 12_000);
        reglement(vente, caisse, "CB", 8_000);
        rafraichir("mv_dashboard_ca_payment_methods");

        Tuple totaux = ligne(
            """
            SELECT
              (SELECT COALESCE(sum(montant_total), 0) FROM mv_dashboard_ca_payment_methods
                WHERE payment_date BETWEEN :debut AND :fin)  AS vue,
              (SELECT COALESCE(sum(paid_amount), 0) FROM payment_transaction
                WHERE dtype = 'SalePayment'
                  AND transaction_date BETWEEN :debut AND :fin) AS table_source
            """);

        assertThat(decimal(totaux, "vue")).isEqualByComparingTo(decimal(totaux, "table_source"));
    }

    /**
     * Une facture de groupe ne porte pas de lignes : elle totalise ses filles. D'où deux
     * exigences — son montant égale leur somme, et un encours calculé naïvement sur toutes les
     * factures compterait deux fois les mêmes créances. C'est ce que prévient le filtre
     * {@code groupe_facture_tiers_payant_id IS NULL} qu'appliquent les écrans d'encours.
     */
    @Test
    @DisplayName("une facture de groupe totalise ses filles sans les compter deux fois")
    void factureDeGroupeTotaliseSesFilles() {
        GroupeTiersPayant groupe = groupeTiersPayant(unique("GROUPE"));
        FactureTiersPayant premiere =
            facture(tiersPayant(unique("TP-A"), groupe), hier(), 30_000, 0, InvoiceStatut.NOT_PAID);
        FactureTiersPayant seconde =
            facture(tiersPayant(unique("TP-B"), groupe), hier(), 20_000, 0, InvoiceStatut.NOT_PAID);
        FactureTiersPayant groupee = factureDeGroupe(groupe, hier(), premiere, seconde);

        Tuple totaux = ligne(
            """
            SELECT
              (SELECT COALESCE(sum(f.montant_ttc), 0) FROM facture_tiers_payant f
                WHERE f.groupe_facture_tiers_payant_id = %d)   AS somme_filles,
              (SELECT COALESCE(sum(f.montant_ttc), 0) FROM facture_tiers_payant f
                WHERE f.invoice_date = :debutSeul)             AS somme_naive,
              (SELECT COALESCE(sum(f.montant_ttc), 0) FROM facture_tiers_payant f
                WHERE f.invoice_date = :debutSeul
                  AND f.groupe_facture_tiers_payant_id IS NULL) AS somme_filtree
            """.formatted(groupee.getId().getId()));

        assertThat(decimal(totaux, "somme_filles"))
            .as("le groupe annonce ce que portent ses filles")
            .isEqualByComparingTo(groupee.getMontantTtc());
        assertThat(decimal(totaux, "somme_naive"))
            .as("sans filtre, les filles sont comptées deux fois — dans elles-mêmes et dans leur groupe")
            .isEqualByComparingTo(groupee.getMontantTtc().multiply(new java.math.BigDecimal(2)));
        assertThat(decimal(totaux, "somme_filtree"))
            .as("le filtre des écrans d'encours élimine le double compte")
            .isEqualByComparingTo(groupee.getMontantTtc());
    }

    // ===== utilitaires =====

    @SuppressWarnings("unchecked")
    private java.util.List<Tuple> lignes(String sql) {
        var requete = em.createNativeQuery(sql, Tuple.class);
        if (sql.contains(":debut ")) {
            requete.setParameter("debut", avantHier()).setParameter("fin", LocalDate.now());
        }
        if (sql.contains(":debutSeul")) {
            requete.setParameter("debutSeul", hier());
        }
        java.util.List<Tuple> resultats = requete.getResultList();
        assertThat(resultats).as("la vue doit porter au moins une ligne à vérifier").isNotEmpty();
        return resultats;
    }

    private Tuple ligne(String sql) {
        return lignes(sql).getFirst();
    }

    private java.math.BigDecimal decimal(Tuple tuple, String colonne) {
        Object valeur = tuple.get(colonne);
        return valeur == null
            ? java.math.BigDecimal.ZERO
            : new java.math.BigDecimal(valeur.toString());
    }

    private static LocalDate hier() {
        return LocalDate.now().minusDays(1);
    }

    private static LocalDate avantHier() {
        return LocalDate.now().minusDays(2);
    }
}

package com.kobe.warehouse.service.dashboard.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.dashboard.CommandesEnCoursDTO;
import com.kobe.warehouse.service.dto.dashboard.PeremptionsDTO;
import com.kobe.warehouse.service.dto.dashboard.RotationStockDTO;
import com.kobe.warehouse.service.dto.dashboard.SuggestionReapproDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Le tableau de bord du responsable des commandes écrit son SQL à la main, directement sur
 * l'{@link jakarta.persistence.EntityManager} : quatre tuiles, une douzaine de requêtes natives qui
 * nomment des colonnes, comparent des dates et divisent des quantités. Hibernate ne les regarde
 * pas ; seul PostgreSQL peut dire si elles tiennent.
 *
 * <p>Ce qui s'y joue n'est pas de l'affichage. Les <b>tranches de péremption</b> décident de ce
 * qu'on retourne au grossiste avant qu'il ne soit trop tard, et une borne mal placée fait passer un
 * produit d'« à surveiller » à « perdu ». La <b>rotation</b> est une division entière en base : le
 * rapide et le normal ne se séparent pas là où une lecture en nombres décimaux le croirait. Les
 * <b>suggestions de réapprovisionnement</b> lisent le seuil mini du produit et son référencement
 * fournisseur, deux données qui vivent dans des tables différentes.
 *
 * <p>Les trois tuiles déléguées aux services de rapport — alertes, ABC, performance fournisseur —
 * sont couvertes sans base par {@code ResponsableCommandeDashboardServiceImplTest} : leurs sources
 * sont des vues matérialisées rafraîchies par des jobs, hors du périmètre de ce paquet.
 */
@DisplayName("ResponsableCommandeDashboardService — SQL natif sur une vraie base")
class ResponsableCommandeDashboardServiceIntegrationTest extends AbstractDashboardIntegrationTest {

    // ===== commandes en cours =====

    @Nested
    @DisplayName("Commandes en cours")
    class CommandesEnCours {

        @Test
        @DisplayName("compte les commandes passées et celles en cours de réception, et totalise leur montant")
        void compteEtTotalise() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            commande(laborex, OrderStatut.REQUESTED, 1);
            commande(laborex, OrderStatut.REQUESTED, 1);
            commande(laborex, OrderStatut.RECEIVED, 1);
            viderLeCache();

            CommandesEnCoursDTO resultat = services.responsableCommandeDashboardService.getCommandesEnCours();

            assertThat(resultat.enAttente()).isEqualTo(2);
            assertThat(resultat.aReceptionner()).isEqualTo(1);
            assertThat(resultat.totalMontant()).isEqualTo(450_000L);
        }

        /** Une commande clôturée est livrée et soldée : elle n'est plus « en cours ». */
        @Test
        @DisplayName("une commande clôturée ne compte plus")
        void commandeClotureeExclue() {
            commande(fournisseur("A " + unique("")), OrderStatut.CLOSED, 1);
            viderLeCache();

            CommandesEnCoursDTO resultat = services.responsableCommandeDashboardService.getCommandesEnCours();

            assertThat(resultat.enAttente()).isZero();
            assertThat(resultat.aReceptionner()).isZero();
            assertThat(resultat.totalMontant()).isZero();
        }

        @Test
        @DisplayName("base vierge : tout à zéro")
        void baseVierge() {
            CommandesEnCoursDTO resultat = services.responsableCommandeDashboardService.getCommandesEnCours();

            assertThat(resultat.enAttente()).isZero();
            assertThat(resultat.aReceptionner()).isZero();
            assertThat(resultat.totalMontant()).isZero();
        }
    }

    // ===== péremptions =====

    @Nested
    @DisplayName("Péremptions")
    class Peremptions {

        /**
         * Les trois tranches sont ce qui déclenche une action différente : négocier un retour,
         * pousser le produit en avant, ou simplement surveiller. Un lot rangé dans la mauvaise
         * tranche fait perdre le délai de négociation avec le grossiste.
         */
        @Test
        @DisplayName("range chaque lot dans sa tranche d'échéance")
        void rangeParTranche() {
            Produit produit = produit(unique("DOLIPRANE"), 5);
            lot(produit, LocalDate.now().plusDays(20), 10);
            lot(produit, LocalDate.now().plusMonths(2), 10);
            lot(produit, LocalDate.now().plusMonths(4), 10);
            viderLeCache();

            PeremptionsDTO resultat = services.responsableCommandeDashboardService.getPeremptions();

            assertThat(resultat.unMois()).isEqualTo(1);
            assertThat(resultat.unATroisMois()).isEqualTo(1);
            assertThat(resultat.troisASixMois()).isEqualTo(1);
        }

        /** Les tranches comptent des <b>produits</b>, pas des lots : deux lots proches n'en font qu'un. */
        @Test
        @DisplayName("un produit à deux lots dans la même tranche n'est compté qu'une fois")
        void comptageParProduit() {
            Produit produit = produit(unique("DOLIPRANE"), 5);
            lot(produit, LocalDate.now().plusDays(10), 10);
            lot(produit, LocalDate.now().plusDays(20), 10);
            viderLeCache();

            assertThat(services.responsableCommandeDashboardService.getPeremptions().unMois()).isEqualTo(1);
        }

        @Test
        @DisplayName("un lot au-delà de six mois est hors de portée de l'écran")
        void auDelaDeSixMoisIgnore() {
            Produit produit = produit(unique("DOLIPRANE"), 5);
            lot(produit, LocalDate.now().plusMonths(9), 10);
            viderLeCache();

            PeremptionsDTO resultat = services.responsableCommandeDashboardService.getPeremptions();

            assertThat(resultat.unMois()).isZero();
            assertThat(resultat.unATroisMois()).isZero();
            assertThat(resultat.troisASixMois()).isZero();
            assertThat(resultat.valeurTotale()).isZero();
        }

        @Test
        @DisplayName("un lot épuisé ne vaut plus rien et ne périme plus")
        void lotEpuiseIgnore() {
            Produit produit = produit(unique("DOLIPRANE"), 5);
            lot(produit, LocalDate.now().plusDays(10), 0);
            viderLeCache();

            PeremptionsDTO resultat = services.responsableCommandeDashboardService.getPeremptions();

            assertThat(resultat.unMois()).isZero();
            assertThat(resultat.valeurTotale()).isZero();
        }

        @Test
        @DisplayName("la valeur totale est celle du stock menacé, au prix de vente")
        void valeurDuStockMenace() {
            Produit produit = produit(unique("DOLIPRANE"), 5); // prix de vente : 10 000
            lot(produit, LocalDate.now().plusDays(20), 3);
            lot(produit, LocalDate.now().plusMonths(4), 2);
            viderLeCache();

            assertThat(services.responsableCommandeDashboardService.getPeremptions().valeurTotale()).isEqualTo(50_000L);
        }

        @Test
        @DisplayName("base vierge : aucune péremption")
        void baseVierge() {
            PeremptionsDTO resultat = services.responsableCommandeDashboardService.getPeremptions();

            assertThat(resultat.unMois()).isZero();
            assertThat(resultat.valeurTotale()).isZero();
        }
    }

    // ===== rotation du stock =====

    @Nested
    @DisplayName("Rotation du stock")
    class Rotation {

        /**
         * La rotation se calcule en base par une division <b>entière</b> : quatre unités vendues
         * pour une en stock font un produit rapide, trois un produit normal. Raisonner en décimal
         * déplacerait les frontières et changerait ce que l'acheteur voit.
         */
        @Test
        @DisplayName("sépare les produits rapides, normaux et lents selon leurs ventes sur trente jours")
        void classeLesProduits() {
            venduRecemment(produitEnStock(1), 5); // 5 / 1 → rapide
            venduRecemment(produitEnStock(1), 3); // 3 / 1 → normal
            venduRecemment(produitEnStock(1), 1); // 1 / 1 → lent
            viderLeCache();

            RotationStockDTO resultat = services.responsableCommandeDashboardService.getRotationStock();

            assertThat(resultat.rapide()).isEqualTo(1);
            assertThat(resultat.normal()).isEqualTo(1);
            assertThat(resultat.lent()).isEqualTo(1);
        }

        @Test
        @DisplayName("un produit jamais vendu est un produit lent")
        void produitJamaisVenduEstLent() {
            produitEnStock(10);
            viderLeCache();

            RotationStockDTO resultat = services.responsableCommandeDashboardService.getRotationStock();

            assertThat(resultat.lent()).isEqualTo(1);
            assertThat(resultat.rapide()).isZero();
        }

        /** Un produit en rupture n'a pas de rotation : diviser par zéro n'a rien à afficher. */
        @Test
        @DisplayName("un produit sans stock est hors du classement")
        void produitSansStockExclu() {
            produitEnStock(0);
            viderLeCache();

            RotationStockDTO resultat = services.responsableCommandeDashboardService.getRotationStock();

            assertThat(resultat.rapide() + resultat.normal() + resultat.lent()).isZero();
        }

        @Test
        @DisplayName("une vente d'il y a trois mois ne compte plus dans la rotation")
        void venteHorsFenetreIgnoree() {
            Produit produit = produitEnStock(1);
            venduLe(produit, 10, LocalDate.now().minusDays(60));
            viderLeCache();

            RotationStockDTO resultat = services.responsableCommandeDashboardService.getRotationStock();

            assertThat(resultat.rapide()).isZero();
            assertThat(resultat.lent()).isEqualTo(1);
        }

        @Test
        @DisplayName("base vierge : rotation nulle, aucun produit classé")
        void baseVierge() {
            RotationStockDTO resultat = services.responsableCommandeDashboardService.getRotationStock();

            assertThat(resultat.rotationMoyenne()).isZero();
            assertThat(resultat.rapide()).isZero();
            assertThat(resultat.normal()).isZero();
            assertThat(resultat.lent()).isZero();
        }
    }

    // ===== suggestions de réapprovisionnement =====

    @Nested
    @DisplayName("Suggestions de réapprovisionnement")
    class SuggestionsReappro {

        /**
         * La suggestion ne se déclenche que sous le seuil mini : proposer une commande pour un
         * produit encore approvisionné ferait immobiliser de la trésorerie sans raison.
         */
        @Test
        @DisplayName("ne suggère que les produits passés sous leur seuil mini")
        void seulementSousLeSeuil() {
            Produit sousLeSeuil = produit(unique("SOUS SEUIL"), 20);
            stock(sousLeSeuil, 5);
            referencement(sousLeSeuil, fournisseur("LABOREX " + unique("")));

            Produit auDessus = produit(unique("AU DESSUS"), 5);
            stock(auDessus, 50);
            referencement(auDessus, fournisseur("DPCI " + unique("")));
            viderLeCache();

            List<SuggestionReapproDTO> suggestions = services.responsableCommandeDashboardService.getSuggestionsReappro();

            assertThat(suggestions).hasSize(1);
            assertThat(suggestions.getFirst().produitLibelle()).startsWith("SOUS SEUIL");
            assertThat(suggestions.getFirst().stockActuel()).isEqualTo(5);
        }

        @Test
        @DisplayName("rattache la suggestion au fournisseur qui référence le produit")
        void rattacheAuFournisseur() {
            Produit produit = produit(unique("SOUS SEUIL"), 20);
            stock(produit, 2);
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));
            referencement(produit, laborex);
            viderLeCache();

            SuggestionReapproDTO suggestion = services.responsableCommandeDashboardService.getSuggestionsReappro().getFirst();

            assertThat(suggestion.fournisseurId()).isEqualTo(laborex.getId().longValue());
            assertThat(suggestion.fournisseurName()).startsWith("LABOREX");
            assertThat(suggestion.codeCip()).isNotBlank();
        }

        /** Faute de consommation, la quantité proposée retombe sur le double du seuil mini. */
        @Test
        @DisplayName("sans historique de vente, propose le double du seuil mini")
        void quantiteParDefaut() {
            Produit produit = produit(unique("SOUS SEUIL"), 20);
            stock(produit, 2);
            referencement(produit, fournisseur("LABOREX " + unique("")));
            viderLeCache();

            SuggestionReapproDTO suggestion = services.responsableCommandeDashboardService.getSuggestionsReappro().getFirst();

            assertThat(suggestion.consommationMoyenne()).isZero();
            assertThat(suggestion.quantiteSuggeree()).isEqualTo(40);
        }

        @Test
        @DisplayName("les produits les plus dégarnis passent en tête")
        void ordreParStockCroissant() {
            Fournisseur laborex = fournisseur("LABOREX " + unique(""));

            Produit presqueVide = produit(unique("PRESQUE VIDE"), 20);
            stock(presqueVide, 1);
            referencement(presqueVide, laborex);

            Produit entame = produit(unique("ENTAME"), 20);
            stock(entame, 10);
            referencement(entame, laborex);
            viderLeCache();

            List<SuggestionReapproDTO> suggestions = services.responsableCommandeDashboardService.getSuggestionsReappro();

            assertThat(suggestions).extracting(SuggestionReapproDTO::stockActuel).containsExactly(1, 10);
        }

        @Test
        @DisplayName("base vierge : aucune suggestion")
        void baseVierge() {
            assertThat(services.responsableCommandeDashboardService.getSuggestionsReappro()).isEmpty();
        }
    }

    // ===== fabriques locales =====

    /** Un produit référencé, en stock au rayon, avec un seuil mini hors de portée des suggestions. */
    private Produit produitEnStock(int quantite) {
        Produit produit = produit(unique("ROTATION"), 0);
        stock(produit, quantite);
        referencement(produit, fournisseur("FRS " + unique("")));
        return produit;
    }

    private void venduRecemment(Produit produit, int quantite) {
        venduLe(produit, quantite, LocalDate.now());
    }

    private void venduLe(Produit produit, int quantite, LocalDate date) {
        ligneDeVente(vente(caisseOuverte(0L), com.kobe.warehouse.domain.enumeration.NatureVente.COMPTANT, 0, 0, false, null, date),
            produit, quantite);
    }
}

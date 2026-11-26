package com.kobe.warehouse.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ContrainteColisage;
import com.kobe.warehouse.service.dto.AjustementColisage;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Le calcul lui-même appartient à {@code FournisseurProduit.appliquerColisage} et y est déjà
 * éprouvé ({@code FournisseurProduitColisageTest}). Ce qui se joue ici est l'autre moitié de la
 * décision : <b>ajuster et informer</b>. Une quantité corrigée sans explication donne l'impression
 * d'un logiciel qui change les chiffres tout seul, et le pharmacien cesse de lui faire confiance.
 *
 * <p>D'où deux exigences testées séparément : le report n'existe que si la quantité a bougé — sinon
 * l'écran afficherait un message à chaque saisie conforme — et son motif nomme la contrainte qui a
 * réellement emporté la décision, le colisage ou le minimum fournisseur.
 */
@DisplayName("Colisage — ajuster la quantité et dire pourquoi")
class ColisageServiceImplTest {

    private final ColisageServiceImpl colisageService = new ColisageServiceImpl();

    @Test
    @DisplayName("7 unités pour un colis de 12 : la ligne part à 12, le motif nomme le colis")
    void ajusterAuColisSuperieur() {
        OrderLine ligne = ligne(7, 12, 0, "DOLIPRANE 1000MG");

        AjustementColisage ajustement = colisageService.ajusterLigne(ligne).orElseThrow();

        assertThat(ligne.getQuantityRequested()).isEqualTo(12);
        assertThat(ajustement.quantiteDemandee()).isEqualTo(7);
        assertThat(ajustement.quantiteRetenue()).isEqualTo(12);
        assertThat(ajustement.contrainte()).isEqualTo(ContrainteColisage.COLISAGE);
        assertThat(ajustement.produitLibelle()).isEqualTo("DOLIPRANE 1000MG");
    }

    @Test
    @DisplayName("14 unités pour un colis de 12 : deux colis, et le motif le dit")
    void nommerLeNombreDeColis() {
        OrderLine ligne = ligne(14, 12, 0, "AMOXICILLINE 500MG");

        AjustementColisage ajustement = colisageService.ajusterLigne(ligne).orElseThrow();

        assertThat(ligne.getQuantityRequested()).isEqualTo(24);
        assertThat(ajustement.contrainte()).isEqualTo(ContrainteColisage.COLISAGE);
    }

    @Test
    @DisplayName("2 unités sous un minimum de 6 : la ligne part à 6, le motif nomme le minimum")
    void porterAuMinimumFournisseur() {
        OrderLine ligne = ligne(2, 1, 6, "SERINGUE 5ML");

        AjustementColisage ajustement = colisageService.ajusterLigne(ligne).orElseThrow();

        assertThat(ligne.getQuantityRequested()).isEqualTo(6);
        assertThat(ajustement.contrainte()).isEqualTo(ContrainteColisage.QUANTITE_MINIMALE);
        assertThat(ajustement.qteMinimaleCommande()).isEqualTo(6);
    }

    /**
     * Le cas où les deux contraintes jouent : l'arrondi au colis donne 6, le minimum exige 8. Le
     * résultat 8 n'étant pas un multiple de 3, c'est bien le minimum qui a décidé — et le motif ne
     * doit pas parler de colis, sous peine d'annoncer un nombre de colis fractionnaire.
     */
    @Test
    @DisplayName("quand le minimum dépasse l'arrondi, c'est lui que le motif nomme")
    void distinguerLaContrainteQuiDecide() {
        OrderLine ligne = ligne(5, 3, 8, "COMPRESSES STÉRILES");

        AjustementColisage ajustement = colisageService.ajusterLigne(ligne).orElseThrow();

        assertThat(ligne.getQuantityRequested()).isEqualTo(8);
        assertThat(ajustement.contrainte()).isEqualTo(ContrainteColisage.QUANTITE_MINIMALE);
    }

    /** À l'inverse : l'arrondi au colis dépasse le minimum, c'est le colisage qui décide. */
    @Test
    @DisplayName("quand l'arrondi dépasse le minimum, c'est le colisage que le motif nomme")
    void nommerLeColisageQuandIlLemporte() {
        OrderLine ligne = ligne(8, 5, 6, "ALCOOL 70°");

        AjustementColisage ajustement = colisageService.ajusterLigne(ligne).orElseThrow();

        assertThat(ligne.getQuantityRequested()).isEqualTo(10);
        assertThat(ajustement.contrainte()).isEqualTo(ContrainteColisage.COLISAGE);
    }

    @Test
    @DisplayName("une quantité déjà conforme ne produit aucun message")
    void resterMuetSurUneQuantiteConforme() {
        OrderLine ligne = ligne(24, 12, 0, "IBUPROFÈNE 400MG");

        Optional<AjustementColisage> ajustement = colisageService.ajusterLigne(ligne);

        assertThat(ajustement).isEmpty();
        assertThat(ligne.getQuantityRequested()).isEqualTo(24);
    }

    @Test
    @DisplayName("sans contrainte au catalogue, la quantité saisie passe telle quelle")
    void laisserPasserSansContrainte() {
        OrderLine ligne = ligne(7, 1, 0, "PARACÉTAMOL 500MG");

        assertThat(colisageService.ajusterLigne(ligne)).isEmpty();
        assertThat(ligne.getQuantityRequested()).isEqualTo(7);
    }

    /**
     * Une ligne provisoire peut n'avoir ni fournisseurProduit ni quantité au moment où elle passe
     * ici. Le colisage n'est pas une validation de saisie : il ne doit rien faire tomber.
     */
    @Test
    @DisplayName("une ligne incomplète ne fait pas échouer l'ajustement")
    void supporterUneLigneIncomplete() {
        OrderLine sansFournisseur = new OrderLine();
        sansFournisseur.setQuantityRequested(7);

        OrderLine sansQuantite = new OrderLine();
        sansQuantite.setFournisseurProduit(fournisseurProduit(12, 0, null));

        assertThat(colisageService.ajusterLigne(sansFournisseur)).isEmpty();
        assertThat(colisageService.ajusterLigne(sansQuantite)).isEmpty();
    }

    /**
     * Le chemin emprunté par la génération des suggestions, qui ajuste une quantité nue avant
     * qu'aucune ligne n'existe.
     */
    @Test
    @DisplayName("la quantité nue de la génération de suggestion est ajustée de la même façon")
    void ajusterUneQuantiteNue() {
        assertThat(colisageService.ajusterQuantite(fournisseurProduit(12, 0, null), 7))
                .isEqualTo(12);
        assertThat(colisageService.ajusterQuantite(fournisseurProduit(1, 6, null), 2))
                .isEqualTo(6);
        assertThat(colisageService.ajusterQuantite(null, 7)).isEqualTo(7);
    }

    // ===== jeu d'essai =====

    private OrderLine ligne(int quantiteDemandee, Integer colis, Integer minimum, String libelle) {
        OrderLine orderLine = new OrderLine();
        orderLine.setQuantityRequested(quantiteDemandee);
        orderLine.setFournisseurProduit(fournisseurProduit(colis, minimum, libelle));
        return orderLine;
    }

    private FournisseurProduit fournisseurProduit(Integer colis, Integer minimum, String libelle) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setQteColis(colis);
        fournisseurProduit.setQteMinimaleCommande(minimum);
        if (libelle != null) {
            Produit produit = new Produit();
            produit.setLibelle(libelle);
            fournisseurProduit.setProduit(produit);
        }
        return fournisseurProduit;
    }
}

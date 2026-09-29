package com.kobe.warehouse.service.ajustement.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.EcartStockDTO;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Lot 4 de PLAN-VENTE-SUR-STOCK-ERRONE : un stock négatif est légitime tant qu'il ne dépasse pas ce
 * que doivent les avoirs ouverts. Au-delà, c'est un écart machine/physique que la liste doit montrer.
 */
@DisplayName("AjustementService.findEcartsARegulariser — sur PostgreSQL")
class EcartsARegulariserIntegrationTest extends AbstractAjustementIntegrationTest {

    @Test
    @DisplayName("Un négatif sans avoir est entièrement un écart")
    void negatifSansAvoir() {
        Produit produit = produitEnStock(unique("ECART-SEC"), -4);

        EcartStockDTO ecart = ecartDe(produit).orElseThrow();

        assertThat(ecart.stock()).isEqualTo(-4);
        assertThat(ecart.quantiteDue()).isZero();
        assertThat(ecart.ecart()).isEqualTo(4);
        assertThat(ecart.libelle()).isEqualTo(produit.getLibelle());
        assertThat(ecart.codeCip()).isEqualTo(codeCip(produit));
    }

    @Test
    @DisplayName("Un négatif couvert par les avoirs ouverts n'est pas un écart")
    void negatifCouvertParLesAvoirs() {
        Produit produit = produitEnStock(unique("ECART-DETTE"), -3);
        avoir(produit, 2, "OUVERT");
        avoir(produit, 1, "OUVERT");

        assertThat(ecartDe(produit)).isEmpty();
    }

    @Test
    @DisplayName("Seule la part non couverte par les avoirs est un écart")
    void negatifPartiellementCouvert() {
        Produit produit = produitEnStock(unique("ECART-PARTIEL"), -5);
        avoir(produit, 2, "OUVERT");

        EcartStockDTO ecart = ecartDe(produit).orElseThrow();

        assertThat(ecart.quantiteDue()).isEqualTo(2);
        assertThat(ecart.ecart()).isEqualTo(3);
    }

    @Test
    @DisplayName("Les avoirs clôturés ou annulés ne doivent plus rien")
    void avoirsClosNeComptentPas() {
        Produit produit = produitEnStock(unique("ECART-CLOS"), -3);
        avoir(produit, 2, "CLOTURE");
        avoir(produit, 1, "ANNULE");

        assertThat(ecartDe(produit).orElseThrow().ecart()).isEqualTo(3);
    }

    @Test
    @DisplayName("Le stock s'apprécie sur tout le magasin, réserve et UG comprises")
    void stockDuMagasinEntier() {
        Produit reserveCouvre = produitEnStock(unique("ECART-RESERVE"), -3);
        stock(reserveCouvre, reserve, 3);
        Produit ugCouvrent = produit(unique("ECART-UG"));
        stock(ugCouvrent, rayon, -2, 2);

        assertThat(ecartDe(reserveCouvre)).isEmpty();
        assertThat(ecartDe(ugCouvrent)).isEmpty();
    }

    @Test
    @DisplayName("Un stock positif ou nul n'apparaît jamais")
    void stockPositif() {
        Produit positif = produitEnStock(unique("ECART-POSITIF"), 5);
        Produit nul = produitEnStock(unique("ECART-NUL"), 0);

        assertThat(ecartDe(positif)).isEmpty();
        assertThat(ecartDe(nul)).isEmpty();
    }

    @Test
    @DisplayName("Les plus grands écarts viennent en tête")
    void triParEcartDecroissant() {
        Produit petit = produitEnStock(unique("ECART-PETIT"), -1);
        Produit grand = produitEnStock(unique("ECART-GRAND"), -9);

        List<Integer> ordre = services.ajustementService.findEcartsARegulariser().stream()
            .map(EcartStockDTO::produitId)
            .filter(id -> id.equals(petit.getId()) || id.equals(grand.getId()))
            .toList();

        assertThat(ordre).containsExactly(grand.getId(), petit.getId());
    }

    private Optional<EcartStockDTO> ecartDe(Produit produit) {
        return services.ajustementService.findEcartsARegulariser().stream()
            .filter(e -> e.produitId().equals(produit.getId()))
            .findFirst();
    }

    private void avoir(Produit produit, int quantite, String statut) {
        em.createNativeQuery("""
                INSERT INTO avoir_client (reference, statut, quantite, montant, produit_id, created_by_id)
                VALUES (:reference, :statut, :quantite, :montant, :produit, :auteur)
                """)
            .setParameter("reference", unique("AV"))
            .setParameter("statut", statut)
            .setParameter("quantite", quantite)
            .setParameter("montant", quantite * 1_000)
            .setParameter("produit", produit.getId())
            .setParameter("auteur", utilisateur.getId())
            .executeUpdate();
    }
}

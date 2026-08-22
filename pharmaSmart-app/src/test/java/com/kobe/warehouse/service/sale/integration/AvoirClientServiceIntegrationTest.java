package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.service.sale.dto.AvoirClientDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * {@link com.kobe.warehouse.service.sale.AvoirClientService} sur un vrai PostgreSQL.
 *
 * <p>Ce service ne lit pas la table des avoirs : il liste les <em>lignes de vente</em> restées en
 * dette, celles dont {@code quantity_avoir} est encore positif. La recherche descend jusqu'au
 * référencement fournisseur du produit, par une jointure sur un ensemble ; le filtre de période
 * traverse la ligne pour atteindre la date de sa vente. Deux détours de Criteria que seule une base
 * peut confirmer.
 */
@DisplayName("AvoirClientService — lignes de vente en attente d'avoir sur PostgreSQL")
class AvoirClientServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("Seules les lignes servies en partie remontent, avec le montant encore dû")
    void lignesEnAttente() {
        Produit manquant = produitEnStock("HUMALOG", 12_000, 7_000, 0, 20);
        Produit servi = produitEnStock("LANTUS", 14_000, 8_000, 0, 20);
        UninsuredCustomer client = client("TOURE", "Aicha", "CLI-AVL-1");
        CashSale vente = venteFermee(manquant, 5, 2);
        vente.setCustomer(client);
        venteFermee(servi, 3, 3);
        viderLeCache();

        var page = services.avoirClientService.findAvoirs(null, null, null, PageRequest.of(0, 20));

        assertEquals(1, page.getTotalElements(), "la vente entièrement servie ne doit rien");
        AvoirClientDTO avoir = page.getContent().getFirst();
        assertEquals("HUMALOG", avoir.produitLibelle());
        assertEquals(3, avoir.quantityAvoir());
        assertEquals(36_000, avoir.montantAvoir(), "3 manquants × 12 000");
        assertEquals("Aicha TOURE", avoir.customerName());
        assertEquals(vente.getNumberTransaction(), avoir.numberTransaction());
    }

    @Test
    @DisplayName("La recherche accepte le libellé du produit comme son code CIP")
    void rechercheParProduitOuCip() {
        Produit cherche = produitEnStock("NOVORAPID", 11_000, 6_500, 0, 20);
        Produit autre = produitEnStock("TRESIBA", 13_000, 7_500, 0, 20);
        fournisseurProduit(cherche, "CIP-90001");
        fournisseurProduit(autre, "CIP-90002");
        venteFermee(cherche, 4, 1);
        venteFermee(autre, 4, 1);
        viderLeCache();

        var parLibelle = services.avoirClientService.findAvoirs("novo", null, null, PageRequest.of(0, 20));
        var parCip = services.avoirClientService.findAvoirs("CIP-90002", null, null, PageRequest.of(0, 20));

        assertEquals(1, parLibelle.getTotalElements());
        assertEquals("NOVORAPID", parLibelle.getContent().getFirst().produitLibelle());
        assertEquals(1, parCip.getTotalElements());
        assertEquals("TRESIBA", parCip.getContent().getFirst().produitLibelle());
    }

    @Test
    @DisplayName("Le filtre de période porte sur la date de la vente, pas sur celle de la ligne")
    void filtreParPeriode() {
        Produit produit = produitEnStock("PERIODE AVOIR", 5_000, 3_000, 0, 30);
        venteFermee(produit, 4, 1, LocalDate.now().minusDays(10));
        venteFermee(produit, 4, 1);
        viderLeCache();

        var duJour = services.avoirClientService.findAvoirs(null, LocalDate.now(), LocalDate.now(), PageRequest.of(0, 20));
        var surDixJours = services.avoirClientService.findAvoirs(
            null,
            LocalDate.now().minusDays(10),
            LocalDate.now(),
            PageRequest.of(0, 20)
        );

        assertEquals(1, duJour.getTotalElements());
        assertEquals(2, surDixJours.getTotalElements());
    }

    @Test
    @DisplayName("La pagination découpe sans fausser le total")
    void pagination() {
        Produit produit = produitEnStock("PAGE AVOIR", 2_000, 1_200, 0, 50);
        venteFermee(produit, 4, 1);
        venteFermee(produit, 5, 2);
        venteFermee(produit, 6, 3);
        viderLeCache();

        var premierePage = services.avoirClientService.findAvoirs(null, null, null, PageRequest.of(0, 2));

        assertEquals(2, premierePage.getContent().size());
        assertEquals(3, premierePage.getTotalElements());
        assertTrue(premierePage.getContent().stream().allMatch(a -> a.quantityAvoir() > 0));
    }
}

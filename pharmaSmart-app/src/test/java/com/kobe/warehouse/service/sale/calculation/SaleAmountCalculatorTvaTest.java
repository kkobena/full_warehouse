package com.kobe.warehouse.service.sale.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La TVA d'une vente se calcule sur ce que la cliente paie : une remise accordée à la vente (remise
 * produit, promotion) sort de la base imposable. Calculée sur le brut, elle était surévaluée.
 */
@DisplayName("TVA de la vente — sur le net des lignes, remise déduite")
class SaleAmountCalculatorTvaTest {

    private final SaleAmountCalculator calculateur = new SaleAmountCalculator(null);
    private long prochainId = 1;

    @Test
    @DisplayName("Une ligne à 18 % remisée : la TVA porte sur le montant payé")
    void tvaSurLeNet() {
        // 11 800 TTC remisé de 1 180 : 10 620 payés, dont 1 620 de TVA (et non 1 800).
        Sales vente = vente(ligne(1, 11_800, 18, 1_180), ligne(2, 1_000, 0, 0));

        calculateur.computeSaleEagerAmount(vente);

        assertEquals(1_620, vente.getTaxAmount());
        assertEquals(9_000 + 2_000, vente.getHtAmount());
    }

    @Test
    @DisplayName("La TVA suit la remise quand elle est posée, puis retirée")
    void tvaSuitLaRemise() {
        SalesLine ligne = ligne(1, 11_800, 18, 0);
        Sales vente = vente(ligne);
        calculateur.computeSaleEagerAmount(vente);
        assertEquals(1_800, vente.getTaxAmount());

        ligne.setDiscountAmount(1_180);
        calculateur.processDiscountCommonAmounts(vente);
        assertEquals(1_620, vente.getTaxAmount());

        ligne.setDiscountAmount(0);
        calculateur.computeTva(vente);
        assertEquals(1_800, vente.getTaxAmount());
    }

    @Test
    @DisplayName("Au retrait d'une ligne, HT et TVA se refont sur les lignes restantes")
    void retraitDUneLigne() {
        SalesLine retiree = ligne(1, 11_800, 18, 0);
        Sales vente = vente(retiree, ligne(1, 5_900, 18, 590));
        calculateur.computeSaleEagerAmount(vente);
        vente.getSalesLines().remove(retiree);

        calculateur.computeTvaAmountOnRemovingItem(vente, retiree);

        // 5 900 − 590 = 5 310 payés : 4 500 HT, 810 de TVA.
        assertEquals(810, vente.getTaxAmount());
        assertEquals(4_500, vente.getHtAmount());
    }

    private static Sales vente(SalesLine... lignes) {
        CashSale vente = new CashSale();
        vente.setSalesLines(new LinkedHashSet<>(java.util.List.of(lignes)));
        return vente;
    }

    private SalesLine ligne(int quantite, int prix, int tva, int remise) {
        Produit produit = new Produit();
        produit.setId(prix + tva);
        SalesLine ligne = new SalesLine();
        // Une ligne de vente s'identifie par son id : sans lui, l'ensemble n'en garderait qu'une.
        ligne.setId(prochainId++);
        ligne.setProduit(produit);
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setRegularUnitPrice(prix);
        ligne.setSalesAmount(quantite * prix);
        ligne.setCostAmount(0);
        ligne.setTaxValue(tva);
        ligne.setDiscountAmount(remise);
        return ligne;
    }
}

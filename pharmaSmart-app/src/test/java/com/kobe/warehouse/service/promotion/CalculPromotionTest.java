package com.kobe.warehouse.service.promotion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Promotion;
import com.kobe.warehouse.domain.PromotionProduit;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Promotion : remise d'une ligne")
class CalculPromotionTest {

    private static Promotion promo(TypePromotion type, Integer valeur) {
        return new Promotion().setType(type).setValeur(valeur);
    }

    private static PromotionProduit article(Integer prixPromo) {
        Produit produit = new Produit();
        produit.setId(1);
        return new PromotionProduit(produit, prixPromo);
    }

    @Test
    void uneCumulableSAppliqueApresLaRemiseDuCaissierEtCedeDevantLePlancher() {
        Promotion dixPourCent = promo(TypePromotion.PROMO_POURCENTAGE, 10);
        // 3 × 1 000, remise produit 150 : la promotion porte sur 2 850, soit 285.
        assertEquals(285, CalculPromotion.remiseCumulee(dixPourCent, article(null), 1_000, 3, 150, 500, null));
        // Acheté 900 : la promotion seule vend déjà à 900 (acceptée à perte) ; le cumul ne descend pas
        // sous ces 2 700, la promotion cède à 150.
        assertEquals(150, CalculPromotion.remiseCumulee(dixPourCent, article(null), 1_000, 3, 150, 900, null));
        // La remise du caissier atteint déjà le plancher : rien de plus, et elle n'est pas reprise.
        assertEquals(0, CalculPromotion.remiseCumulee(dixPourCent, article(null), 1_000, 3, 400, 900, null));
        // Un montant par unité s'ôte du prix déjà remisé.
        assertEquals(
                300,
                CalculPromotion.remiseCumulee(
                        promo(TypePromotion.PROMO_MONTANT, 100), article(null), 1_000, 3, 150, 500, null));
    }

    @Test
    void pourcentageArrondiAuSuperieurSurLeMontantDeLaLigne() {
        assertEquals(
                300, CalculPromotion.remiseLigne(promo(TypePromotion.PROMO_POURCENTAGE, 10), article(null), 1_000, 3));
        // 3 × 333 × 15 % = 149,85 → 150
        assertEquals(
                150, CalculPromotion.remiseLigne(promo(TypePromotion.PROMO_POURCENTAGE, 15), article(null), 333, 3));
    }

    @Test
    void montantParUniteJamaisSuperieurAuPrix() {
        assertEquals(
                600, CalculPromotion.remiseLigne(promo(TypePromotion.PROMO_MONTANT, 200), article(null), 1_000, 3));
        assertEquals(
                300, CalculPromotion.remiseLigne(promo(TypePromotion.PROMO_MONTANT, 5_000), article(null), 100, 3));
    }

    @Test
    void prixFixeParProduitEtIgnoreSilEstAuDessusDuPrix() {
        assertEquals(
                750, CalculPromotion.remiseLigne(promo(TypePromotion.PROMO_PRIX_FIXE, null), article(750), 1_000, 3));
        assertEquals(
                0, CalculPromotion.remiseLigne(promo(TypePromotion.PROMO_PRIX_FIXE, null), article(1_200), 1_000, 3));
        assertEquals(
                0, CalculPromotion.remiseLigne(promo(TypePromotion.PROMO_PRIX_FIXE, null), article(null), 1_000, 3));
    }

    @Test
    void seuilDeQuantiteNonAtteintNeRemiseRien() {
        Promotion promotion = promo(TypePromotion.PROMO_POURCENTAGE, 10).setQuantiteMin(3);
        assertEquals(0, CalculPromotion.remiseLigne(promotion, article(null), 1_000, 2));
        assertEquals(300, CalculPromotion.remiseLigne(promotion, article(null), 1_000, 3));
    }

    @Test
    void auDelaDuPlafondLesUnitesRestentAuPrixDeReference() {
        Promotion promotion = promo(TypePromotion.PROMO_MONTANT, 100).setQuantiteMaxParVente(2);
        assertEquals(200, CalculPromotion.remiseLigne(promotion, article(null), 1_000, 5));
    }

    @Test
    void lesUnitesDisponiblesBornentLaRemiseMaisPasLeSeuil() {
        Promotion promotion = promo(TypePromotion.PROMO_MONTANT, 100).setQuantiteMin(3);
        // Trois achetées, une seule encore disponible : le seuil est atteint, une unité est remisée.
        assertEquals(1, CalculPromotion.unitesRemisees(promotion, 3, 1));
        assertEquals(100, CalculPromotion.remiseLigne(promotion, article(null), 1_000, 3, 1));
        assertEquals(0, CalculPromotion.remiseLigne(promotion, article(null), 1_000, 3, 0));
        assertEquals(300, CalculPromotion.remiseLigne(promotion, article(null), 1_000, 3, null));
    }

    @Test
    void prixUnitaireApresIgnoreLeSeuil() {
        Promotion promotion = promo(TypePromotion.PROMO_POURCENTAGE, 20).setQuantiteMin(3);
        assertEquals(800, CalculPromotion.prixUnitaireApres(promotion, article(null), 1_000));
    }
}

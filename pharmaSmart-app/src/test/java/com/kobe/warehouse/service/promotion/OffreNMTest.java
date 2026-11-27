package com.kobe.warehouse.service.promotion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.service.promotion.OffreNM.Ligne;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Offre N+M")
class OffreNMTest {

    @Test
    void deuxAcheteesUneOffertesurUnMemeArticle() {
        RemiseRepartie r = OffreNM.appliquer(2, 1, List.of(new Ligne(1_200, 3)), null);

        assertEquals(1_200, r.remiseTotale());
        assertArrayEquals(new int[] {3}, r.unites());
    }

    @Test
    void lesUnitesEnSurplusRestentAuPrixNormal() {
        // 7 unités en 2+1 : deux groupes complets, la septième paie plein tarif.
        RemiseRepartie r = OffreNM.appliquer(2, 1, List.of(new Ligne(1_000, 7)), null);

        assertEquals(2_000, r.remiseTotale());
        assertArrayEquals(new int[] {6}, r.unites());
    }

    @Test
    void sousLeGroupeRienNEstRemise() {
        RemiseRepartie r = OffreNM.appliquer(2, 1, List.of(new Ligne(1_000, 2)), null);

        assertEquals(0, r.remiseTotale());
        assertArrayEquals(new int[] {0}, r.unites());
    }

    @Test
    void laMoinsChereDuGroupeEstOfferteEtLaRemiseSeRepartitSurTout() {
        // Shampooing 3 000 + après-shampooing 2 000 + masque 1 500, en 2+1 : le masque est offert.
        RemiseRepartie r =
                OffreNM.appliquer(2, 1, List.of(new Ligne(3_000, 1), new Ligne(2_000, 1), new Ligne(1_500, 1)), null);

        assertEquals(1_500, r.remiseTotale());
        // Au prorata de 3 000 / 2 000 / 1 500 sur 6 500 : rendre le shampooing rend sa part de remise.
        assertArrayEquals(new int[] {692, 462, 346}, r.remises());
        assertArrayEquals(new int[] {1, 1, 1}, r.unites());
    }

    @Test
    void lesGroupesSeFormentDuPlusCherAuMoinsCher() {
        // 4 unités à 2 000 et 2 à 500 en 2+1 : groupes {2000,2000,2000} et {2000,500,500}.
        // Offerts : 2 000, puis 500. Une mise en commun naïve offrirait les deux 500.
        RemiseRepartie r = OffreNM.appliquer(2, 1, List.of(new Ligne(500, 2), new Ligne(2_000, 4)), null);

        assertEquals(2_500, r.remiseTotale());
        assertArrayEquals(new int[] {2, 4}, r.unites());
    }

    @Test
    void lePlafondNeLaisseQueDesGroupesComplets() {
        // 9 unités en 2+1, mais 5 disponibles : un seul groupe tient.
        RemiseRepartie r = OffreNM.appliquer(2, 1, List.of(new Ligne(1_000, 9)), 5);

        assertEquals(1_000, r.remiseTotale());
        assertArrayEquals(new int[] {3}, r.unites());
    }

    @Test
    void uneOffreSansUniteOfferteNeRemiseRien() {
        assertEquals(
                0, OffreNM.appliquer(2, 0, List.of(new Ligne(1_000, 9)), null).remiseTotale());
    }
}

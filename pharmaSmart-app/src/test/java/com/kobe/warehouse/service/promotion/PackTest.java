package com.kobe.warehouse.service.promotion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.service.promotion.Pack.Ligne;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pack")
class PackTest {

    /** Shampooing (id 1) + après-shampooing (id 2) = 8 000 F au lieu de 5 000 + 4 500. */
    private static final Map<Integer, Integer> DUO = new TreeMap<>(Map.of(1, 1, 2, 1));

    @Test
    void unPackCompletRemiseLaDifferenceRepartieAuProrata() {
        RemiseRepartie r = Pack.appliquer(DUO, 8_000, List.of(new Ligne(1, 5_000, 1), new Ligne(2, 4_500, 1)), null);

        assertEquals(1_500, r.remiseTotale());
        // 1 500 au prorata de 5 000 et 4 500 : 789,47 et 710,53 ; le franc restant à la plus forte décimale.
        assertArrayEquals(new int[] {789, 711}, r.remises());
        assertArrayEquals(new int[] {1, 1}, r.unites());
    }

    @Test
    void unPackIncompletNeRemiseRien() {
        RemiseRepartie r = Pack.appliquer(DUO, 8_000, List.of(new Ligne(1, 5_000, 3)), null);

        assertEquals(0, r.remiseTotale());
        assertArrayEquals(new int[] {0}, r.unites());
    }

    @Test
    void deuxPacksCompletsEtLeSurplusAuPrixNormal() {
        // 3 shampooings et 2 après-shampooings : deux packs, le troisième shampooing au prix.
        RemiseRepartie r = Pack.appliquer(DUO, 8_000, List.of(new Ligne(1, 5_000, 3), new Ligne(2, 4_500, 2)), null);

        assertEquals(3_000, r.remiseTotale());
        assertArrayEquals(new int[] {2, 2}, r.unites());
    }

    @Test
    void laQuantiteDUnComposantCompte() {
        // Deux savons (id 1) et une crème (id 2) pour 3 000 au lieu de 2 × 1 000 + 1 500.
        Map<Integer, Integer> trio = new TreeMap<>(Map.of(1, 2, 2, 1));

        RemiseRepartie seul =
                Pack.appliquer(trio, 3_000, List.of(new Ligne(1, 1_000, 1), new Ligne(2, 1_500, 1)), null);
        RemiseRepartie complet =
                Pack.appliquer(trio, 3_000, List.of(new Ligne(1, 1_000, 2), new Ligne(2, 1_500, 1)), null);

        assertEquals(0, seul.remiseTotale());
        assertEquals(500, complet.remiseTotale());
    }

    @Test
    void lePlafondNeLaisseQueDesPacksComplets() {
        // 2 packs dans le panier, 3 unités disponibles : un seul pack (2 unités) tient.
        RemiseRepartie r = Pack.appliquer(DUO, 8_000, List.of(new Ligne(1, 5_000, 2), new Ligne(2, 4_500, 2)), 3);

        assertEquals(1_500, r.remiseTotale());
        assertArrayEquals(new int[] {1, 1}, r.unites());
    }

    @Test
    void unComposantSurDeuxLignesSePrendDansLOrdre() {
        RemiseRepartie r = Pack.appliquer(
                DUO, 8_000, List.of(new Ligne(1, 5_000, 1), new Ligne(2, 4_500, 1), new Ligne(1, 5_000, 1)), null);

        assertArrayEquals(new int[] {1, 1, 0}, r.unites());
        assertEquals(0, r.remises()[2]);
    }
}

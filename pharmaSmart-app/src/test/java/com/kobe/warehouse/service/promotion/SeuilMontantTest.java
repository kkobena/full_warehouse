package com.kobe.warehouse.service.promotion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kobe.warehouse.service.promotion.SeuilMontant.Ligne;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Remise dès un montant d'achat")
class SeuilMontantTest {

    @Test
    void sousLeSeuilRienNEstRemise() {
        RemiseRepartie r = SeuilMontant.appliquer(20_000, 10, List.of(new Ligne(12_000, 1), new Ligne(7_999, 1)));

        assertEquals(0, r.remiseTotale());
        assertArrayEquals(new int[] {0, 0}, r.unites());
    }

    @Test
    void leSeuilAtteintLeTauxVautPourToutLeMontant() {
        // 12 000 + 8 500 = 20 500 : 10 % de tout, pas de l'excédent sur 20 000.
        RemiseRepartie r = SeuilMontant.appliquer(20_000, 10, List.of(new Ligne(6_000, 2), new Ligne(8_500, 1)));

        assertEquals(2_050, r.remiseTotale());
        assertArrayEquals(new int[] {1_200, 850}, r.remises());
        assertArrayEquals(new int[] {2, 1}, r.unites());
    }

    @Test
    void leSeuilExactementAtteintSuffit() {
        assertEquals(
                2_000,
                SeuilMontant.appliquer(20_000, 10, List.of(new Ligne(20_000, 1)))
                        .remiseTotale());
    }

    @Test
    void uneSeuleFoisArrondiAuSuperieurPuisReparti() {
        // 20 333 à 10 % : 2 033,3 arrondi à 2 034. Au prorata : 733,56 et 1 300,44 ; le franc restant
        // va à la plus forte décimale, jamais un arrondi par ligne.
        RemiseRepartie r = SeuilMontant.appliquer(20_000, 10, List.of(new Ligne(7_333, 1), new Ligne(13_000, 1)));

        assertEquals(2_034, r.remiseTotale());
        assertArrayEquals(new int[] {734, 1_300}, r.remises());
    }

    @Test
    void uneLigneSansPrixNAidePasNiNEstRemisee() {
        RemiseRepartie r = SeuilMontant.appliquer(10_000, 10, List.of(new Ligne(0, 5), new Ligne(10_000, 1)));

        assertArrayEquals(new int[] {0, 1_000}, r.remises());
        assertArrayEquals(new int[] {0, 1}, r.unites());
    }
}

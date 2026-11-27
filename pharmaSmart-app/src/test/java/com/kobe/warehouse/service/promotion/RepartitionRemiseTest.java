package com.kobe.warehouse.service.promotion;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Répartition d'une remise au plus fort reste")
class RepartitionRemiseTest {

    @Test
    void laSommeDesPartsVautToujoursLaRemise() {
        // 100 F sur trois lignes égales : 33,33 chacune. Un ceil ligne par ligne donnerait 102.
        int[] parts = RepartitionRemise.auPlusFortReste(100, new long[] {1_000, 1_000, 1_000});

        assertEquals(100, Arrays.stream(parts).sum());
        // À reste égal, la première ligne l'emporte.
        assertArrayEquals(new int[] {34, 33, 33}, parts);
    }

    @Test
    void lesFrancsRestantsVontAuxPlusFortesPartiesDecimales() {
        // 1 000 F au prorata de 1 500 / 2 300 / 3 200 : 214,28 / 328,57 / 457,14.
        int[] parts = RepartitionRemise.auPlusFortReste(1_000, new long[] {1_500, 2_300, 3_200});

        assertArrayEquals(new int[] {214, 329, 457}, parts);
    }

    @Test
    void uneLigneSansPoidsNeRecoitRien() {
        assertArrayEquals(new int[] {0, 10}, RepartitionRemise.auPlusFortReste(10, new long[] {0, 500}));
        assertArrayEquals(new int[] {0, 0}, RepartitionRemise.auPlusFortReste(10, new long[] {0, 0}));
        assertArrayEquals(new int[] {0, 0}, RepartitionRemise.auPlusFortReste(0, new long[] {100, 200}));
    }
}

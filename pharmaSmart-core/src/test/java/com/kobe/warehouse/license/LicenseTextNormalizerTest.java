package com.kobe.warehouse.license;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * La normalisation est le garde-fou contre le faux positif le plus probable du dispositif : une
 * officine bloquée parce que sa raison sociale est saisie en minuscules ou avec des accents.
 */
class LicenseTextNormalizerTest {

    @ParameterizedTest
    @CsvSource(
        {
            "'PHARMACIE DE LA PAIX', 'Pharmacie de la Paix'",
            "'PHARMACIE DE LA PAIX', '  pharmacie   de la  paix  '",
            "'PHARMACIE SAINTE THERESE', 'Pharmacie Sainte Thérèse'",
            "'PHARMACIE COTE D IVOIRE', 'pharmacie côte d ivoire'",
        }
    )
    void considereEquivalentesDesEcrituresDifferentesDuMemeNom(String licence, String officine) {
        assertThat(LicenseTextNormalizer.matches(licence, officine)).isTrue();
    }

    @Test
    void distingueDeuxOfficinesReellementDifferentes() {
        assertThat(LicenseTextNormalizer.matches("PHARMACIE DE LA PAIX", "PHARMACIE DU PLATEAU")).isFalse();
    }

    @Test
    void neReconnaitPasUneValeurAbsente() {
        assertThat(LicenseTextNormalizer.matches(null, "PHARMACIE")).isFalse();
        assertThat(LicenseTextNormalizer.matches("PHARMACIE", "   ")).isFalse();
    }

    @Test
    void leControleDeRenfortNeTrancheQueSiLesDeuxValeursSontRenseignees() {
        // taxId est nullable côté Magasin : son absence ne doit jamais bloquer une officine.
        assertThat(LicenseTextNormalizer.matchesIfBothPresent(null, "1234567A")).isTrue();
        assertThat(LicenseTextNormalizer.matchesIfBothPresent("1234567A", null)).isTrue();
        assertThat(LicenseTextNormalizer.matchesIfBothPresent("1234567a", " 1234567A ")).isTrue();
        assertThat(LicenseTextNormalizer.matchesIfBothPresent("1234567A", "7654321B")).isFalse();
    }
}

package com.kobe.warehouse.service.pilotage.calcul;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import org.junit.jupiter.api.Test;

class FormatRapportTest {

    @Test
    void abregeLesMontantsCommeLesTuiles() {
        assertThat(FormatRapport.formaterValeur(19_194_652d, UniteIndicateur.MONTANT)).isEqualTo("19,2 M");
        assertThat(FormatRapport.formaterValeur(850_000d, UniteIndicateur.MONTANT)).isEqualTo("850 k");
        assertThat(FormatRapport.formaterValeur(940d, UniteIndicateur.MONTANT)).isEqualTo("940");
        assertThat(FormatRapport.formaterValeur(null, UniteIndicateur.MONTANT)).isEqualTo("—");
    }

    @Test
    void separeLesMilliersParUneEspaceOrdinaire() {
        assertThat(FormatRapport.formaterValeur(1234d, UniteIndicateur.NOMBRE)).isEqualTo("1 234");
        assertThat(FormatRapport.formaterValeur(32.54, UniteIndicateur.POURCENTAGE)).isEqualTo("32,5 %");
        // Comme l'écran : décimales toujours écrites pour un taux ou un ratio.
        assertThat(FormatRapport.formaterValeur(31d, UniteIndicateur.POURCENTAGE)).isEqualTo("31,0 %");
        assertThat(FormatRapport.formaterValeur(4d, UniteIndicateur.RATIO)).isEqualTo("4,00");
    }

    @Test
    void unTauxVarieEnPointsEtUneHausseDesRemisesEstDefavorable() {
        assertThat(FormatRapport.formaterVariation(1.25, 4.0, UniteIndicateur.POURCENTAGE, SensFavorable.HAUSSE).texte()).isEqualTo("+ 1,3 pt");
        var remises = FormatRapport.formaterVariation(1000d, 12.0, UniteIndicateur.MONTANT, SensFavorable.BAISSE);
        assertThat(remises.texte()).isEqualTo("+ 12,0 %");
        assertThat(remises.appreciation()).isEqualTo("defavorable");
        assertThat(FormatRapport.formaterVariation(null, null, UniteIndicateur.MONTANT, SensFavorable.HAUSSE).texte()).isEqualTo("n.s.");
    }
}

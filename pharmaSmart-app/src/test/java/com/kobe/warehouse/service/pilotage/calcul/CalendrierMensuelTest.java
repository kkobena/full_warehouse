package com.kobe.warehouse.service.pilotage.calcul;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import java.util.stream.LongStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pilotage — calendrier mensuel à sommes préfixées")
class CalendrierMensuelTest {

    /** Janvier 2025 à mars 2026 : le mois n (1 = janvier 2025) vaut n × 100 de CA. */
    private final CalendrierMensuel calendrier = CalendrierMensuel.ranger(2025, LongStream.rangeClosed(1, 15).mapToObj(n -> ca(n * 100)).toList());

    @Test
    @DisplayName("le mois, le cumul depuis janvier et les douze mois glissants")
    void fenetres() {
        assertThat(calendrier.lireFenetre(2026, 2, ModeAnnees.MENSUEL).caTtc()).isEqualTo(1_400);
        assertThat(calendrier.lireFenetre(2026, 2, ModeAnnees.CUMULE).caTtc()).isEqualTo(1_300 + 1_400);
        // Mars 2025 à février 2026 : mois 3 à 14.
        assertThat(calendrier.lireFenetre(2026, 2, ModeAnnees.GLISSANT).caTtc()).isEqualTo(LongStream.rangeClosed(3, 14).sum() * 100);
    }

    @Test
    @DisplayName("avant le départ, rien ; après le mois en cours, les mois n'existent pas et ne comptent pas")
    void bornes() {
        assertThat(calendrier.lireFenetre(2025, 2, ModeAnnees.GLISSANT).caTtc()).isEqualTo(100 + 200);
        assertThat(calendrier.existe(2026, 3)).isTrue();
        assertThat(calendrier.existe(2026, 4)).isFalse();
        assertThat(calendrier.lireMois(2026, 1, 12).caTtc()).isEqualTo(1_300 + 1_400 + 1_500);
        assertThat(calendrier.lireMois(2024, 1, 12)).isEqualTo(MesuresPilotage.AUCUNE);
    }

    private static MesuresPilotage ca(long montant) {
        return new MesuresPilotage(1, 0, montant, montant, 0, 0, montant, montant, 0, 0, 0, 0, 1);
    }
}

package com.kobe.warehouse.service.ordonnance;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.enumeration.StatutOrdonnance;
import com.kobe.warehouse.service.dto.ordonnance.LigneSuivieDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SuiviOrdonnanceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 5);

    @Test
    void resteADelivrerTientCompteDesRenouvellements() {
        // 2 boîtes x (2 renouvellements + 1) = 6 au total.
        assertThat(SuiviOrdonnance.capacite(2, 2)).isEqualTo(6);
        assertThat(SuiviOrdonnance.resteADelivrer(2, 2, 2)).isEqualTo(4);
        assertThat(SuiviOrdonnance.resteADelivrer(2, 2, 9)).isZero();
    }

    @Test
    void delivrancePartielleLaisseLeResteEtGardeLOrdonnanceEnCours() {
        var lignes = List.of(new LigneSuivieDTO(3, 1));
        assertThat(SuiviOrdonnance.statut(false, null, 0, lignes, AUJOURDHUI)).isEqualTo(StatutOrdonnance.EN_COURS);
        assertThat(SuiviOrdonnance.resteADelivrer(3, 0, 1)).isEqualTo(2);
    }

    @Test
    void ordonnanceSoldeeEstTerminee() {
        assertThat(SuiviOrdonnance.statut(false, null, 0, List.of(new LigneSuivieDTO(3, 3), new LigneSuivieDTO(1, 1)), AUJOURDHUI))
            .isEqualTo(StatutOrdonnance.TERMINEE);
    }

    @Test
    void uneLigneNonSoldeeSuffitAGarderLOrdonnanceEnCours() {
        assertThat(SuiviOrdonnance.statut(false, null, 0, List.of(new LigneSuivieDTO(3, 3), new LigneSuivieDTO(1, 0)), AUJOURDHUI))
            .isEqualTo(StatutOrdonnance.EN_COURS);
    }

    @Test
    void expireeApresSaDateDeFinMaisSoldeePrimeSurExpiree() {
        var enCours = List.of(new LigneSuivieDTO(1, 0));
        assertThat(SuiviOrdonnance.statut(false, AUJOURDHUI.minusDays(1), 0, enCours, AUJOURDHUI)).isEqualTo(StatutOrdonnance.EXPIREE);
        assertThat(SuiviOrdonnance.statut(false, AUJOURDHUI, 0, enCours, AUJOURDHUI)).isEqualTo(StatutOrdonnance.EN_COURS);
        assertThat(SuiviOrdonnance.statut(false, AUJOURDHUI.minusDays(1), 0, List.of(new LigneSuivieDTO(1, 1)), AUJOURDHUI))
            .isEqualTo(StatutOrdonnance.TERMINEE);
    }

    @Test
    void clotureManuelleTermineMemeAvecUnResteADelivrer() {
        assertThat(SuiviOrdonnance.statut(true, null, 0, List.of(new LigneSuivieDTO(1, 0)), AUJOURDHUI)).isEqualTo(StatutOrdonnance.TERMINEE);
    }

    @Test
    void ordonnanceSansLigneNEstJamaisSoldee() {
        assertThat(SuiviOrdonnance.statut(false, null, 0, List.of(), AUJOURDHUI)).isEqualTo(StatutOrdonnance.EN_COURS);
    }

    @Test
    void renouvellementsRestantsDecroissentPassageApresPassage() {
        // 2 renouvellements = 3 passages possibles. Le premier passage n'est pas un renouvellement.
        assertThat(SuiviOrdonnance.renouvellementsRestants(2, List.of(new LigneSuivieDTO(2, 0)))).isEqualTo(2);
        assertThat(SuiviOrdonnance.renouvellementsRestants(2, List.of(new LigneSuivieDTO(2, 1)))).isEqualTo(2);
        assertThat(SuiviOrdonnance.renouvellementsRestants(2, List.of(new LigneSuivieDTO(2, 2)))).isEqualTo(2);
        assertThat(SuiviOrdonnance.renouvellementsRestants(2, List.of(new LigneSuivieDTO(2, 4)))).isEqualTo(1);
        assertThat(SuiviOrdonnance.renouvellementsRestants(2, List.of(new LigneSuivieDTO(2, 6)))).isZero();
    }

    @Test
    void lePassageCompletSeMesureSurLaLigneLaMoinsDelivree() {
        assertThat(SuiviOrdonnance.passagesComplets(List.of(new LigneSuivieDTO(2, 4), new LigneSuivieDTO(1, 1)))).isEqualTo(1);
    }
}

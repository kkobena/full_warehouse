package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.repository.PilotageAnalyseRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.MesuresVentileesDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pilotage — rapprochement d'une ventilation avec sa référence")
class VentilateurPilotageTest {

    private static final PeriodeDTO PERIODE = new PeriodeDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 31));
    private static final PeriodeDTO REFERENCE = new PeriodeDTO(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 10, 31));
    private static final ComparaisonPeriodesDTO COMPARAISON = new ComparaisonPeriodesDTO(PERIODE, REFERENCE, false);

    @Mock
    private PilotageAnalyseRepository pilotageAnalyseRepository;

    @InjectMocks
    private VentilateurPilotage ventilateur;

    @Test
    @DisplayName("un élément absent d'un côté y vaut 0 ; le total couvre les deux périodes ; le libellé vient de l'axe")
    void elementsRapproches() {
        lire(PERIODE, ligne("1", "Antalgiques", null, 30_000), ligne(null, null, null, 2_000));
        lire(REFERENCE, ligne("1", "Antalgiques", null, 40_000), ligne("2", "Dermatologie", null, 8_000));

        var ventilation = ventilateur.comparer(SourceAnalyse.LIGNES, COMPARAISON, AxeAnalyse.FAMILLE, null, List.of(), Granularite.MOIS);

        assertThat(ventilation.elements())
            .extracting(e -> e.membre().cle(), e -> e.membre().libelle(), e -> e.periode().caTtc(), e -> e.reference().caTtc())
            .containsExactly(tuple("1", "Antalgiques", 30_000L, 40_000L), tuple("", "Sans famille", 2_000L, 0L), tuple("2", "Dermatologie", 0L, 8_000L));
        assertThat(ventilation.total().periode().caTtc()).isEqualTo(32_000);
        assertThat(ventilation.total().reference().caTtc()).isEqualTo(48_000);
    }

    @Test
    @DisplayName("la tranche N de la période fait face à la tranche N de la référence")
    void tranchesParRang() {
        lire(PERIODE, ligne(null, null, LocalDate.of(2026, 9, 3), 1_000), ligne(null, null, LocalDate.of(2026, 10, 3), 3_000));
        lire(REFERENCE, ligne(null, null, LocalDate.of(2025, 9, 20), 500), ligne(null, null, LocalDate.of(2025, 10, 30), 700));

        var ventilation = ventilateur.comparer(SourceAnalyse.LIGNES, COMPARAISON, AxeAnalyse.PERIODE, null, List.of(), Granularite.MOIS);

        assertThat(ventilation.elements())
            .extracting(e -> e.membre().cle(), e -> e.membre().libelle(), e -> e.periode().caTtc(), e -> e.reference().caTtc())
            .containsExactly(tuple("0", "Sept. 2026", 1_000L, 500L), tuple("1", "Oct. 2026", 3_000L, 700L));
    }

    @Test
    @DisplayName("le jour de la semaine se déduit du jour, numéroté à partir du lundi")
    void jourDeLaSemaine() {
        // 5 et 12 octobre 2026 : deux lundis.
        lire(PERIODE, ligne(null, null, LocalDate.of(2026, 10, 5), 1_000), ligne(null, null, LocalDate.of(2026, 10, 12), 2_000));
        lire(REFERENCE);

        var ventilation = ventilateur.comparer(SourceAnalyse.LIGNES, COMPARAISON, AxeAnalyse.JOUR_SEMAINE, null, List.of(), Granularite.MOIS);

        assertThat(ventilation.elements()).extracting(e -> e.membre().cle(), e -> e.membre().libelle(), e -> e.periode().caTtc()).containsExactly(tuple("1", "Lundi", 3_000L));
    }

    private void lire(PeriodeDTO periode, MesuresVentileesDTO... lignes) {
        when(pilotageAnalyseRepository.ventiler(argThat(requete -> requete != null && requete.du().equals(periode.du())))).thenReturn(List.of(lignes));
    }

    private static MesuresVentileesDTO ligne(String cle, String libelle, LocalDate jour, long ca) {
        return new MesuresVentileesDTO(cle, libelle, null, null, jour, 0, ca, ca, 0, 0, 0, 0);
    }
}

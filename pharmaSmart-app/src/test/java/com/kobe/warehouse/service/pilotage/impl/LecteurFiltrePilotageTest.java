package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Pilotage — mesures d'un périmètre filtré")
class LecteurFiltrePilotageTest {

    private static final PeriodeDTO DEUX_MOIS = new PeriodeDTO(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28));
    private static final List<FiltreAnalyseDTO> ANTALGIQUES = List.of(new FiltreAnalyseDTO(AxeAnalyse.FAMILLE, List.of("12")));

    @Mock
    private LecteurMesuresPilotage lecteurMesuresPilotage;

    @Mock
    private VentilateurPilotage ventilateurPilotage;

    @InjectMocks
    private LecteurFiltrePilotage lecteur;

    @BeforeEach
    void officine() {
        when(lecteurMesuresPilotage.lireParTranche(eq(DEUX_MOIS), anyList())).thenReturn(List.of(mesures(10_000, 25), mesures(9_000, 24)));
    }

    @Test
    @DisplayName("sans filtre, les mesures de l'officine")
    void sansFiltre() {
        assertThat(lecteur.lire(DEUX_MOIS, Granularite.MOIS, List.of(), IndicateurPilotage.NB_VENTES)).extracting(MesuresPilotage::caTtc).containsExactly(10_000L, 9_000L);
        verifyNoInteractions(ventilateurPilotage);
    }

    @Test
    @DisplayName("filtrées, les mesures gardent les jours ouvrés de l'officine ; un mois sans vente vaut 0")
    void filtreGardeLesJoursOuvres() {
        when(ventilateurPilotage.comparer(eq(SourceAnalyse.LIGNES), any(), eq(AxeAnalyse.PERIODE), any(), eq(ANTALGIQUES), eq(Granularite.MOIS))).thenReturn(
            new Ventilation(List.of(new MesuresComparees(new MembreAnalyseDTO("1", "Févr."), null, mesures(2_000, 0), MesuresPilotage.AUCUNE)), MesuresComparees.AUCUNES)
        );

        List<MesuresPilotage> filtrees = lecteur.lire(DEUX_MOIS, Granularite.MOIS, ANTALGIQUES, IndicateurPilotage.CA_TTC);

        assertThat(filtrees).extracting(MesuresPilotage::caTtc, MesuresPilotage::joursOuvres).containsExactly(
            tuple(0L, 25L),
            tuple(2_000L, 24L)
        );
    }

    @Test
    @DisplayName("le nombre de ventes ne se lit pas par famille")
    void indicateurIllisible() {
        assertThatThrownBy(() -> lecteur.lire(DEUX_MOIS, Granularite.MOIS, ANTALGIQUES, IndicateurPilotage.NB_VENTES)).isInstanceOf(GenericError.class);
    }

    private static MesuresPilotage mesures(long ca, long joursOuvres) {
        return new MesuresPilotage(0, 0, ca, ca, 0, 0, ca, ca, 0, 0, 0, 0, joursOuvres);
    }
}

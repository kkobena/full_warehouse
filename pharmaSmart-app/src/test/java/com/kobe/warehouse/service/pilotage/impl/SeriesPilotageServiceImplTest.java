package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.ObjectifPilotage;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.repository.ObjectifPilotageRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Pilotage — séries et objectifs")
class SeriesPilotageServiceImplTest {

    private static final PeriodeDTO SEPTEMBRE = new PeriodeDTO(LocalDate.of(2025, 9, 1), LocalDate.of(2025, 9, 30));

    private final PeriodeComparaisonService periodeComparaisonService = mock(PeriodeComparaisonService.class);
    private final LecteurMesuresPilotage lecteurMesuresPilotage = mock(LecteurMesuresPilotage.class);
    private final DictionnaireIndicateursService dictionnaire = mock(DictionnaireIndicateursService.class);
    private final ObjectifPilotageRepository repository = mock(ObjectifPilotageRepository.class);
    private final SeriesPilotageServiceImpl service = new SeriesPilotageServiceImpl(periodeComparaisonService, lecteurMesuresPilotage, dictionnaire, repository);

    @BeforeEach
    void preparer() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(new ComparaisonPeriodesDTO(SEPTEMBRE, null, false));
        when(lecteurMesuresPilotage.lireParTranche(any(), anyList())).thenReturn(List.of(new MesuresPilotage(1, 0, 2_700, 2_700, 0, 0, 2_700, 2_700, 0, 1, 0, 0, 1)));
        when(dictionnaire.filtrerAutorises(anyList())).thenReturn(List.of(IndicateurPilotage.CA_TTC));
        when(repository.findAllByAnneeBetween(anyInt(), anyInt())).thenReturn(
            List.of(new ObjectifPilotage().setIndicateur(IndicateurPilotage.CA_TTC).setAnnee(2025).setMois(9).setValeur(3_000))
        );
    }

    @Test
    @DisplayName("comparaison « Objectif » : la référence de la série et de chaque tranche est l'objectif")
    void comparerALObjectif() {
        when(dictionnaire.lireDroitsAccordes()).thenReturn(Set.of(DroitPilotage.OBJECTIFS));

        SerieIndicateurDTO ca = service.calculerSeries(lireRequete(TypeComparaison.OBJECTIF)).series().getFirst();

        assertThat(ca.objectif()).isEqualTo(3_000.0);
        assertThat(ca.valeurReference()).isEqualTo(3_000.0);
        assertThat(ca.ecartPct()).isEqualTo(-10.0);
        assertThat(ca.points().getFirst().valeurReference()).isEqualTo(3_000.0);
        assertThat(ca.projection()).as("septembre 2025 n'est pas le mois en cours").isNull();
    }

    @Test
    @DisplayName("sans le droit de l'onglet Objectifs, aucun objectif n'est lu")
    void masquerSansDroit() {
        when(dictionnaire.lireDroitsAccordes()).thenReturn(Set.of(DroitPilotage.TABLEAU_DE_BORD));

        SerieIndicateurDTO ca = service.calculerSeries(lireRequete(TypeComparaison.OBJECTIF)).series().getFirst();

        assertThat(ca.objectif()).isNull();
        assertThat(ca.valeurReference()).isNull();
    }

    private static RequetePilotageDTO lireRequete(TypeComparaison comparaison) {
        return new RequetePilotageDTO(SEPTEMBRE.du(), SEPTEMBRE.au(), comparaison, null, null, null, false, Granularite.MOIS, List.of(IndicateurPilotage.CA_TTC));
    }
}

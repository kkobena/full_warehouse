package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.service.dto.pilotage.AxeExpliqueDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.ExplicationEcartDTO;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import java.time.LocalDate;
import java.util.EnumSet;
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
@DisplayName("Pilotage — expliquer l'écart d'un élément")
class ExplicateurEcartTest {

    private static final PeriodeDTO PERIODE = new PeriodeDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 9));
    private static final PeriodeDTO REFERENCE = new PeriodeDTO(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 9));
    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(PERIODE.du(), PERIODE.au(), TypeComparaison.MEME_PERIODE_N_1, null, null, null, true, null, null);
    private static final List<FiltreAnalyseDTO> ANTALGIQUES = List.of(new FiltreAnalyseDTO(AxeAnalyse.FAMILLE, List.of("1")));

    @Mock
    private PeriodeComparaisonService periodeComparaisonService;

    @Mock
    private DictionnaireIndicateursService dictionnaireIndicateursService;

    @Mock
    private VentilateurPilotage ventilateurPilotage;

    @InjectMocks
    private ExplicateurEcart explicateur;

    @BeforeEach
    void antalgiquesEnBaisseDe10000() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(new ComparaisonPeriodesDTO(PERIODE, REFERENCE, true));
        when(dictionnaireIndicateursService.filtrerAutorises(anyList())).thenAnswer(appel -> appel.getArgument(0));
        when(dictionnaireIndicateursService.lireDroitsAccordes()).thenReturn(EnumSet.of(DroitPilotage.PAGE));
        // Par défaut, un axe ne rend qu'un élément : il n'explique rien.
        when(ventilateurPilotage.comparer(any(), any(), any(), any(), anyList(), any())).thenReturn(ventilation(element("x", "Seul", 50_000, 60_000)));
        when(ventilateurPilotage.comparer(any(), any(), isNull(), isNull(), anyList(), any())).thenReturn(ventilation(element(null, null, 50_000, 60_000)));
        // Trois produits portent 9 000 des 10 000 de baisse, un quatrième le reste.
        when(ventilateurPilotage.comparer(any(), any(), eq(AxeAnalyse.PRODUIT), isNull(), anyList(), any())).thenReturn(
            ventilation(
                element("10", "Doliprane 1000", 20_000, 25_000),
                element("11", "Efferalgan", 10_000, 13_000),
                element("12", "Dafalgan", 5_000, 6_000),
                element("13", "Ibuprofène", 15_000, 16_000)
            )
        );
    }

    @Test
    @DisplayName("l'axe le plus explicatif vient d'abord, avec la part concentrée sur ses trois principaux éléments")
    void axeLePlusExplicatif() {
        ExplicationEcartDTO explication = explicateur.expliquer(REQUETE, ANTALGIQUES);

        assertThat(explication.ecart()).isEqualTo(-10_000);
        AxeExpliqueDTO premier = explication.axes().getFirst();
        assertThat(premier.axe().code()).isEqualTo(AxeAnalyse.PRODUIT);
        assertThat(premier.principales()).extracting(ContributionDTO::libelle).containsExactly("Doliprane 1000", "Efferalgan", "Dafalgan");
        assertThat(premier.part()).isEqualTo(90.0);
        assertThat(explication.axes()).extracting(axe -> axe.axe().code()).doesNotContain(AxeAnalyse.FAMILLE);
    }

    @Test
    @DisplayName("les axes sans droit ne sont pas lus")
    void axesSansDroitIgnores() {
        explicateur.expliquer(REQUETE, ANTALGIQUES);

        verify(ventilateurPilotage, never()).comparer(any(), any(), eq(AxeAnalyse.VENDEUR), any(), anyList(), any());
    }

    @Test
    @DisplayName("sans période de comparaison, il n'y a pas d'écart à expliquer")
    void sansReference() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(new ComparaisonPeriodesDTO(PERIODE, null, true));

        assertThatThrownBy(() -> explicateur.expliquer(REQUETE, ANTALGIQUES)).isInstanceOf(GenericError.class);
    }

    private static Ventilation ventilation(MesuresComparees... elements) {
        MesuresComparees total = List.of(elements).stream().reduce(MesuresComparees.AUCUNES, MesuresComparees::plus);
        return new Ventilation(List.of(elements), new MesuresComparees(null, null, total.periode(), total.reference()));
    }

    private static MesuresComparees element(String cle, String libelle, long ca, long caReference) {
        return new MesuresComparees(new MembreAnalyseDTO(cle, libelle), null, lignes(ca), lignes(caReference));
    }

    private static MesuresPilotage lignes(long ca) {
        return new MesuresPilotage(0, 0, ca, ca, 0, 0, ca, ca, 0, 0, 0, 0, 0);
    }
}

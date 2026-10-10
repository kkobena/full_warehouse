package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ElementAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.FiltreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
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
@DisplayName("Pilotage — onglet Analyser")
class AnalysePilotageServiceImplTest {

    private static final PeriodeDTO PERIODE = new PeriodeDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 9));
    private static final PeriodeDTO REFERENCE = new PeriodeDTO(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 9));
    private static final ComparaisonPeriodesDTO COMPARAISON = new ComparaisonPeriodesDTO(PERIODE, REFERENCE, true);

    @Mock
    private PeriodeComparaisonService periodeComparaisonService;

    @Mock
    private DictionnaireIndicateursService dictionnaireIndicateursService;

    @Mock
    private VentilateurPilotage ventilateurPilotage;

    @Mock
    private ExplicateurEcart explicateurEcart;

    @InjectMocks
    private AnalysePilotageServiceImpl service;

    @BeforeEach
    void autoriserTout() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(COMPARAISON);
        when(dictionnaireIndicateursService.filtrerAutorises(anyList())).thenAnswer(appel -> appel.getArgument(0));
        // Antalgiques 60 000 (réf. 50 000), Vitamines 30 000 (réf. 40 000), Dermatologie 10 000 (réf. 10 000).
        ventiler(
            AxeAnalyse.FAMILLE,
            null,
            element("1", "Antalgiques", 60_000, 50_000),
            element("2", "Vitamines", 30_000, 40_000),
            element("3", "Dermatologie", 10_000, 10_000)
        );
    }

    @Test
    @DisplayName("au-delà du top, les éléments sont regroupés en « autres » ; part et contribution sur le premier indicateur")
    void topEtAutres() {
        AnalysePilotageDTO analyse = service.analyser(requete(IndicateurPilotage.CA_TTC), new RequeteAnalyseDTO(AxeAnalyse.FAMILLE, null, 2, TriAnalyse.VALEUR, null));

        assertThat(analyse.elements()).extracting(ElementAnalyseDTO::libelle, ElementAnalyseDTO::part).containsExactly(tuple("Antalgiques", 60.0), tuple("Vitamines", 30.0));
        assertThat(analyse.autres().libelle()).isEqualTo("Autres (1)");
        assertThat(analyse.autres().cellules().getFirst().valeur()).isEqualTo(10_000);
        assertThat(analyse.total().getFirst().valeur()).isEqualTo(100_000);
        assertThat(analyse.nombreElements()).isEqualTo(3);
        // Écart total nul : pas de contribution calculable.
        assertThat(analyse.elements()).extracting(ElementAnalyseDTO::contribution).containsOnlyNulls();
    }

    @Test
    @DisplayName("le tri « en recul » met d'abord la plus forte baisse")
    void triEnRecul() {
        AnalysePilotageDTO analyse = service.analyser(requete(IndicateurPilotage.CA_TTC), new RequeteAnalyseDTO(AxeAnalyse.FAMILLE, null, 0, TriAnalyse.ECART_BAISSE, null));

        assertThat(analyse.elements()).extracting(ElementAnalyseDTO::libelle).containsExactly("Vitamines", "Dermatologie", "Antalgiques");
        assertThat(analyse.autres()).isNull();
    }

    @Test
    @DisplayName("un indicateur que les lignes ne savent pas calculer est signalé, pas calculé")
    void indicateurIgnore() {
        AnalysePilotageDTO analyse = service.analyser(
            requete(IndicateurPilotage.CA_TTC, IndicateurPilotage.NB_VENTES),
            new RequeteAnalyseDTO(AxeAnalyse.FAMILLE, null, 0, TriAnalyse.VALEUR, null)
        );

        assertThat(analyse.source()).isEqualTo(SourceAnalyse.LIGNES);
        assertThat(analyse.indicateurs()).extracting(IndicateurPilotageDTO::code).containsExactly("CA_TTC");
        assertThat(analyse.indicateursIgnores()).extracting(IndicateurPilotageDTO::code).containsExactly("NB_VENTES");
    }

    @Test
    @DisplayName("la famille (lignes) ne se croise pas avec l'heure (en-têtes)")
    void axesIncompatibles() {
        assertThatThrownBy(() ->
            service.analyser(requete(IndicateurPilotage.CA_TTC), new RequeteAnalyseDTO(AxeAnalyse.FAMILLE, AxeAnalyse.HEURE, 0, TriAnalyse.VALEUR, null))
        )
            .isInstanceOf(GenericError.class)
            .hasMessageContaining("ne se croisent pas");
    }

    @Test
    @DisplayName("le croisé ne lit que les éléments retenus par le top")
    void croiseRestreintAuTop() {
        when(
            ventilateurPilotage.comparer(
                eq(SourceAnalyse.LIGNES),
                eq(COMPARAISON),
                eq(AxeAnalyse.FAMILLE),
                eq(AxeAnalyse.NATURE_VENTE),
                argThat(filtres -> filtres.equals(List.of(new FiltreAnalyseDTO(AxeAnalyse.FAMILLE, List.of("1"))))),
                any()
            )
        ).thenReturn(
            new Ventilation(
                List.of(
                    new MesuresComparees(new MembreAnalyseDTO("1", "Antalgiques"), new MembreAnalyseDTO("COMPTANT", "Comptant"), lignes(40_000), lignes(30_000)),
                    new MesuresComparees(new MembreAnalyseDTO("1", "Antalgiques"), new MembreAnalyseDTO("ASSURANCE", "Assurance"), lignes(20_000), lignes(20_000))
                ),
                MesuresComparees.AUCUNES
            )
        );

        AnalysePilotageDTO analyse = service.analyser(
            requete(IndicateurPilotage.CA_TTC),
            new RequeteAnalyseDTO(AxeAnalyse.FAMILLE, AxeAnalyse.NATURE_VENTE, 1, TriAnalyse.VALEUR, null)
        );

        assertThat(analyse.croise().colonnes()).extracting(MembreAnalyseDTO::libelle).containsExactly("Comptant", "Assurance");
        assertThat(analyse.croise().lignes().getFirst().cellules()).extracting(c -> c.valeur(), c -> c.ecart()).containsExactly(tuple(40_000.0, 10_000.0), tuple(20_000.0, 0.0));
    }

    private void ventiler(AxeAnalyse axe, AxeAnalyse axe2, MesuresComparees... elements) {
        MesuresComparees total = List.of(elements).stream().reduce(MesuresComparees.AUCUNES, MesuresComparees::plus);
        when(ventilateurPilotage.comparer(any(), eq(COMPARAISON), eq(axe), axe2 == null ? isNull() : eq(axe2), anyList(), any())).thenReturn(
            new Ventilation(List.of(elements), new MesuresComparees(null, null, total.periode(), total.reference()))
        );
    }

    private static RequetePilotageDTO requete(IndicateurPilotage... indicateurs) {
        return new RequetePilotageDTO(PERIODE.du(), PERIODE.au(), TypeComparaison.MEME_PERIODE_N_1, null, null, null, true, null, List.of(indicateurs));
    }

    private static MesuresComparees element(String cle, String libelle, long ca, long caReference) {
        return new MesuresComparees(new MembreAnalyseDTO(cle, libelle), null, lignes(ca), lignes(caReference));
    }

    private static MesuresPilotage lignes(long ca) {
        return new MesuresPilotage(0, 0, ca, ca, 0, 0, ca, ca, 0, 0, 0, 0, 0);
    }
}

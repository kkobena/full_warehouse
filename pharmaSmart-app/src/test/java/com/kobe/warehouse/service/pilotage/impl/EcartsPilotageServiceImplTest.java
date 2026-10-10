package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ContributionDTO;
import com.kobe.warehouse.service.dto.pilotage.EcartsPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
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
@DisplayName("Pilotage — explication de l'écart de CA")
class EcartsPilotageServiceImplTest {

    private static final PeriodeDTO PERIODE = new PeriodeDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 9));
    private static final PeriodeDTO REFERENCE = new PeriodeDTO(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 9));
    private static final ComparaisonPeriodesDTO COMPARAISON = new ComparaisonPeriodesDTO(PERIODE, REFERENCE, true);
    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(PERIODE.du(), PERIODE.au(), TypeComparaison.MEME_PERIODE_N_1, null, null, null, true, null, null);

    @Mock
    private PeriodeComparaisonService periodeComparaisonService;

    @Mock
    private LecteurMesuresPilotage lecteurMesuresPilotage;

    @Mock
    private VentilateurPilotage ventilateurPilotage;

    @Mock
    private DictionnaireIndicateursService dictionnaireIndicateursService;

    @InjectMocks
    private EcartsPilotageServiceImpl service;

    @BeforeEach
    void comparerOctobreAOctobreNMoins1() {
        when(periodeComparaisonService.comparer(eq(REQUETE), any())).thenReturn(COMPARAISON);
        // Référence : 100 ventes × 2 articles × 500 = 100 000 ; période : 110 ventes × 2,5 articles × 400 = 110 000.
        when(lecteurMesuresPilotage.lireTotal(REFERENCE)).thenReturn(mesures(100, 200, 100_000));
        when(lecteurMesuresPilotage.lireTotal(PERIODE)).thenReturn(mesures(110, 275, 110_000));
        when(dictionnaireIndicateursService.lireDroitsAccordes()).thenReturn(EnumSet.allOf(DroitPilotage.class));
        when(ventilateurPilotage.comparer(eq(SourceAnalyse.LIGNES), eq(COMPARAISON), any(), any(), any(), any())).thenReturn(
            new Ventilation(List.of(), MesuresComparees.AUCUNES)
        );
    }

    @Test
    @DisplayName("les trois effets se somment exactement à l'écart")
    void effetsSommesALEcart() {
        EcartsPilotageDTO ecarts = service.expliquerEcarts(REQUETE, 5);

        assertThat(ecarts.effetFrequentation()).isEqualTo(10_000);
        assertThat(ecarts.effetArticles()).isEqualTo(27_500);
        assertThat(ecarts.effetPrix()).isEqualTo(-27_500);
        assertThat(ecarts.effetFrequentation() + ecarts.effetArticles() + ecarts.effetPrix()).isEqualTo(ecarts.ca() - ecarts.caReference());
    }

    @Test
    @DisplayName("sans le droit « Clients & équipe », les vendeurs ne sont pas lus")
    void vendeursTusSansDroit() {
        when(dictionnaireIndicateursService.lireDroitsAccordes()).thenReturn(EnumSet.of(DroitPilotage.PAGE));

        service.expliquerEcarts(REQUETE, 5);

        verify(ventilateurPilotage, never()).comparer(any(), any(), eq(AxeAnalyse.VENDEUR), any(), any(), any());
    }

    @Test
    @DisplayName("hausses et baisses : les plus forts écarts de chaque sens, toutes ventilations confondues")
    void contributionsClassees() {
        when(ventilateurPilotage.comparer(eq(SourceAnalyse.LIGNES), eq(COMPARAISON), eq(AxeAnalyse.FAMILLE), any(), any(), any())).thenReturn(
            new Ventilation(
                List.of(element("1", "Antalgiques", 30_000, 40_000), element("2", "Dermatologie", 0, 8_000), element("3", "Vitamines", 5_000, 0)),
                MesuresComparees.AUCUNES
            )
        );

        EcartsPilotageDTO ecarts = service.expliquerEcarts(REQUETE, 5);

        assertThat(ecarts.hausses()).extracting(ContributionDTO::libelle, ContributionDTO::ecart).containsExactly(tuple("Vitamines", 5_000L));
        assertThat(ecarts.baisses())
            .extracting(ContributionDTO::libelle, ContributionDTO::ecart)
            .containsExactly(tuple("Antalgiques", -10_000L), tuple("Dermatologie", -8_000L));
        assertThat(ecarts.hausses()).extracting(ContributionDTO::libelleAxe).containsExactly("Famille");
    }

    private static MesuresComparees element(String cle, String libelle, long ca, long caReference) {
        return new MesuresComparees(new MembreAnalyseDTO(cle, libelle), null, lignes(ca), lignes(caReference));
    }

    private static MesuresPilotage lignes(long ca) {
        return new MesuresPilotage(0, 0, ca, ca, 0, 0, ca, ca, 0, 0, 0, 0, 0);
    }

    private static MesuresPilotage mesures(long ventes, long articles, long ca) {
        return new MesuresPilotage(ventes, 0, ca, ca, 0, 0, ca, ca, 0, articles, 0, 0, 1);
    }
}

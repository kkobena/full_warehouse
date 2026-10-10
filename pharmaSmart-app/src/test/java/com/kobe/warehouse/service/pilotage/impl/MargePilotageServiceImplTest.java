package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.repository.PilotageVentesDetailRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneMargeDTO;
import com.kobe.warehouse.service.dto.pilotage.MargeRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteMargeDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
import com.kobe.warehouse.service.pilotage.calcul.MesuresComparees;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import com.kobe.warehouse.service.settings.AppConfigurationService;
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
@DisplayName("Pilotage — marge")
class MargePilotageServiceImplTest {

    private static final PeriodeDTO PERIODE = new PeriodeDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 9));
    private static final ComparaisonPeriodesDTO COMPARAISON = new ComparaisonPeriodesDTO(PERIODE, new PeriodeDTO(LocalDate.of(2025, 10, 1), LocalDate.of(2025, 10, 9)), true);
    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(PERIODE.du(), PERIODE.au(), TypeComparaison.MEME_PERIODE_N_1, null, null, null, true, null, null);

    @Mock
    private PeriodeComparaisonService periodeComparaisonService;

    @Mock
    private DictionnaireIndicateursService dictionnaireIndicateursService;

    @Mock
    private VentilateurPilotage ventilateurPilotage;

    @Mock
    private PilotageVentesDetailRepository pilotageVentesDetailRepository;

    @Mock
    private AppConfigurationService appConfigurationService;

    @InjectMocks
    private MargePilotageServiceImpl service;

    @BeforeEach
    void deuxFamilles() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(COMPARAISON);
        when(dictionnaireIndicateursService.lireDroitsAccordes()).thenReturn(EnumSet.of(DroitPilotage.RENTABILITE_REMISES));
        when(appConfigurationService.getSeuilFaibleMargePilotage()).thenReturn(15);
        // Antalgiques : 10 000 HT à 20 % (réf. 8 000 à 25 %) ; Dermatologie : 10 000 à 40 % (réf. 12 000 à 40 %).
        MesuresComparees antalgiques = element("1", "Antalgiques", 10_000, 2_000, 8_000, 2_000);
        MesuresComparees dermatologie = element("2", "Dermatologie", 10_000, 4_000, 12_000, 4_800);
        when(ventilateurPilotage.comparer(any(), eq(COMPARAISON), eq(AxeAnalyse.FAMILLE), any(), anyList(), any())).thenReturn(
            new Ventilation(List.of(antalgiques, dermatologie), antalgiques.plus(dermatologie))
        );
        when(ventilateurPilotage.comparer(any(), any(), eq(AxeAnalyse.PRODUIT), any(), anyList(), any())).thenReturn(
            new Ventilation(
                List.of(element("10", "Doliprane", 9_000, 900, 0, 0), element("11", "Crème", 500, 50, 0, 0), element("12", "Vitamine C", 3_000, 1_500, 0, 0)),
                MesuresComparees.AUCUNES
            )
        );
        when(pilotageVentesDetailRepository.listerVentesAMargeNegative(any(), any(), any(), any(), any())).thenReturn(
            List.of(new VenteMargeDTO(5L, PERIODE.du(), "V5", "Awa Koné", 1_000.0, 1_200.0, -200.0))
        );
    }

    @Test
    @DisplayName("effet volume + effet taux = écart de marge ; effet mix + effet taux = variation du taux")
    void decompositionsExactes() {
        MargeRentabiliteDTO marge = service.analyserMarge(REQUETE, AxeAnalyse.FAMILLE);

        LigneMargeDTO antalgiques = marge.lignes().stream().filter(ligne -> ligne.libelle().equals("Antalgiques")).findFirst().orElseThrow();
        assertThat(antalgiques.effetVolume() + antalgiques.effetTaux()).isEqualTo(2_000 - 2_000);
        assertThat(antalgiques.effetVolume()).isEqualTo(500);
        assertThat(antalgiques.ecartTaux()).isCloseTo(-5.0, within(1e-9));
        // Taux global : 30 % contre 34 % → -4 points.
        assertThat(marge.tauxMarge() - marge.tauxMargeReference()).isCloseTo(-4.0, within(1e-9));
        assertThat(marge.effetMix() + marge.effetTaux()).isCloseTo(-4.0, within(1e-9));
        assertThat(marge.effetTaux()).isCloseTo(-2.5, within(1e-9));
        assertThat(marge.lignes()).extracting(LigneMargeDTO::libelle).containsExactly("Dermatologie", "Antalgiques");
    }

    @Test
    @DisplayName("produits sous le seuil, plus gros CA d'abord ; vendeur masqué sans le droit « Clients & équipe »")
    void faiblesMargesEtVentes() {
        MargeRentabiliteDTO marge = service.analyserMarge(REQUETE, AxeAnalyse.FAMILLE);

        assertThat(marge.faiblesMarges()).extracting(LigneMargeDTO::libelle).containsExactly("Doliprane", "Crème");
        assertThat(marge.ventesAMargeNegative()).extracting(VenteMargeDTO::vendeur).containsOnlyNulls();
    }

    @Test
    @DisplayName("la marge ne se ventile pas par heure (en-têtes)")
    void axeRefuse() {
        assertThatThrownBy(() -> service.analyserMarge(REQUETE, AxeAnalyse.HEURE)).isInstanceOf(GenericError.class);
    }

    private static MesuresComparees element(String cle, String libelle, long caHt, long marge, long caHtReference, long margeReference) {
        return new MesuresComparees(new MembreAnalyseDTO(cle, libelle), null, lignes(caHt, marge), lignes(caHtReference, margeReference));
    }

    private static MesuresPilotage lignes(long caHt, long marge) {
        return new MesuresPilotage(0, 0, caHt, caHt, 0, 0, caHt, caHt, caHt - marge, 0, 0, 0, 0);
    }
}

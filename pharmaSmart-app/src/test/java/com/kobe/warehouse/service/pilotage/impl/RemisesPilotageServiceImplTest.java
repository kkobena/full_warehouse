package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.repository.PilotageVentesDetailRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneRemiseDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RemisesRentabiliteDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.VenteRemiseeDTO;
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
@DisplayName("Pilotage — remises")
class RemisesPilotageServiceImplTest {

    private static final PeriodeDTO PERIODE = new PeriodeDTO(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 9));
    private static final ComparaisonPeriodesDTO SANS_REFERENCE = new ComparaisonPeriodesDTO(PERIODE, null, true);
    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(PERIODE.du(), PERIODE.au(), TypeComparaison.AUCUNE, null, null, null, true, null, null);

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
    private RemisesPilotageServiceImpl service;

    @BeforeEach
    void centVentesDontVingtRemisees() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(SANS_REFERENCE);
        when(dictionnaireIndicateursService.lireDroitsAccordes()).thenReturn(EnumSet.allOf(DroitPilotage.class));
        when(appConfigurationService.getAlerteRemiseVendeurPilotage()).thenReturn(2.0);
        // 80 ventes sans remise (80 000), 15 par privilège (20 000, 1 500 de remise), 5 autorisées (10 000, 1 000).
        entetes(
            AxeAnalyse.OCTROI_REMISE,
            entete("AUCUNE", "Sans remise", 80, 80_000, 0),
            entete("PRIVILEGE", "Privilège du vendeur", 15, 20_000, 1_500),
            entete("AUTORISEE", "Autorisée (clé de sécurité)", 5, 10_000, 1_000)
        );
        // Marge brute des lignes : 27 500.
        when(ventilateurPilotage.comparer(eq(SourceAnalyse.LIGNES), any(), isNull(), any(), anyList(), any())).thenReturn(
            new Ventilation(List.of(), new MesuresComparees(null, null, lignes(110_000, 82_500, 2_500), MesuresPilotage.AUCUNE))
        );
        entetes(AxeAnalyse.REMISE, entete("0-0", "Sans remise", 80, 80_000, 0), entete("5-9", "5 à 10 %", 20, 30_000, 2_500));
        when(ventilateurPilotage.comparer(eq(SourceAnalyse.LIGNES), any(), eq(AxeAnalyse.REMISE), any(), anyList(), any())).thenReturn(
            new Ventilation(
                List.of(
                    new MesuresComparees(new MembreAnalyseDTO("5-9", "5 à 10 %"), null, lignes(30_000, 27_000, 2_500), MesuresPilotage.AUCUNE),
                    new MesuresComparees(new MembreAnalyseDTO("0-0", "Sans remise"), null, lignes(80_000, 56_000, 0), MesuresPilotage.AUCUNE)
                ),
                MesuresComparees.AUCUNES
            )
        );
        // Équipe : 2 500 / 110 000 = 2,3 % ; Awa : 2 000 / 20 000 = 10 % ; Koffi : 500 / 90 000.
        entetes(AxeAnalyse.VENDEUR, entete("7", "Awa", 30, 20_000, 2_000), entete("8", "Koffi", 70, 90_000, 500));
        when(pilotageVentesDetailRepository.listerPlusFortesRemises(any(), any(), any(), any(), eq("PR_AJOUTER_REMISE_VENTE"), any())).thenReturn(
            List.of(new VenteRemiseeDTO(3L, PERIODE.du(), "V3", "Awa", 6_000, 900, 15.0, "Dr Yao"))
        );
    }

    @Test
    @DisplayName("tuiles : part des ventes remisées, remise moyenne, poids des remises dans la marge")
    void tuiles() {
        RemisesRentabiliteDTO remises = service.analyserRemises(REQUETE);

        assertThat(remises.remises().valeur()).isEqualTo(2_500);
        assertThat(remises.partVentesRemisees().valeur()).isCloseTo(20.0, within(1e-9));
        assertThat(remises.remiseMoyenne().valeur()).isCloseTo(125.0, within(1e-9));
        assertThat(remises.poidsRemisesMarge().valeur()).isCloseTo(2_500 * 100.0 / (27_500 + 2_500), within(1e-9));
        assertThat(remises.octrois()).extracting(LigneRemiseDTO::libelle).containsExactly("Privilège du vendeur", "Autorisée (clé de sécurité)");
    }

    @Test
    @DisplayName("tranches dans leur ordre, ventes des en-têtes et taux de marge des lignes")
    void tranches() {
        List<LigneRemiseDTO> tranches = service.analyserRemises(REQUETE).tranches();

        assertThat(tranches).extracting(LigneRemiseDTO::libelle).containsExactly("Sans remise", "5 à 10 %");
        assertThat(tranches.getLast().nbVentes().valeur()).isEqualTo(20);
        assertThat(tranches.getLast().tauxMarge().valeur()).isCloseTo(10.0, within(1e-9));
    }

    @Test
    @DisplayName("un vendeur au-delà du double du taux de l'équipe est signalé ; sans droit, ni vendeurs ni noms")
    void vendeurs() {
        assertThat(service.analyserRemises(REQUETE).vendeurs()).extracting(LigneRemiseDTO::libelle, LigneRemiseDTO::alerte).containsExactly(
            org.assertj.core.api.Assertions.tuple("Awa", true),
            org.assertj.core.api.Assertions.tuple("Koffi", false)
        );

        when(dictionnaireIndicateursService.lireDroitsAccordes()).thenReturn(EnumSet.of(DroitPilotage.RENTABILITE_REMISES));
        RemisesRentabiliteDTO sansDroit = service.analyserRemises(REQUETE);
        assertThat(sansDroit.vendeurs()).isNull();
        assertThat(sansDroit.plusFortesRemises()).extracting(VenteRemiseeDTO::vendeur, VenteRemiseeDTO::autorisePar).containsExactly(
            org.assertj.core.api.Assertions.tuple(null, null)
        );
    }

    private void entetes(AxeAnalyse axe, MesuresComparees... elements) {
        MesuresComparees total = List.of(elements).stream().reduce(MesuresComparees.AUCUNES, MesuresComparees::plus);
        when(ventilateurPilotage.comparer(eq(SourceAnalyse.ENTETES), any(), eq(axe), any(), anyList(), any())).thenReturn(
            new Ventilation(List.of(elements), new MesuresComparees(null, null, total.periode(), total.reference()))
        );
    }

    private static MesuresComparees entete(String cle, String libelle, long ventes, long ca, long remises) {
        return new MesuresComparees(
            new MembreAnalyseDTO(cle, libelle),
            null,
            new MesuresPilotage(ventes, 0, ca, ca, remises, 0, 0, 0, 0, 0, 0, 0, 0),
            MesuresPilotage.AUCUNE
        );
    }

    private static MesuresPilotage lignes(long caHt, long coutHt, long remises) {
        return new MesuresPilotage(0, 0, caHt, caHt, remises, 0, caHt, caHt, coutHt, 0, 0, 0, 0);
    }
}

package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.TypeComparaison;
import com.kobe.warehouse.repository.PilotageTresorerieRepository;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonPeriodesDTO;
import com.kobe.warehouse.service.dto.pilotage.ComptageDTO;
import com.kobe.warehouse.service.dto.pilotage.DelaiObserveDTO;
import com.kobe.warehouse.service.dto.pilotage.EncaissementModeJourDTO;
import com.kobe.warehouse.service.dto.pilotage.EncaissementsTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.FactureEncoursDTO;
import com.kobe.warehouse.service.dto.pilotage.FactureOrganismeDTO;
import com.kobe.warehouse.service.dto.pilotage.ModeEncaissementDTO;
import com.kobe.warehouse.service.dto.pilotage.OrganismeTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.RequetePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.TiersPayantTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.TrancheMontantDTO;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.PeriodeComparaisonService;
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
@DisplayName("Pilotage — trésorerie et tiers payant")
class TresoreriePilotageServiceImplTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 10);
    private static final PeriodeDTO SEPTEMBRE = new PeriodeDTO(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
    private static final RequetePilotageDTO REQUETE = new RequetePilotageDTO(SEPTEMBRE.du(), SEPTEMBRE.au(), TypeComparaison.AUCUNE, null, null, null, false, Granularite.MOIS, null);

    @Mock
    private PeriodeComparaisonService periodeComparaisonService;

    @Mock
    private DictionnaireIndicateursService dictionnaireIndicateursService;

    @Mock
    private PilotageTresorerieRepository repository;

    @Mock
    private AppConfigurationService configuration;

    @InjectMocks
    private TresoreriePilotageServiceImpl service;

    @BeforeEach
    void septembre() {
        when(periodeComparaisonService.comparer(any(), any())).thenReturn(new ComparaisonPeriodesDTO(SEPTEMBRE, null, false));
        when(dictionnaireIndicateursService.lireDroitsAccordes()).thenReturn(EnumSet.of(DroitPilotage.TRESORERIE_TIERS_PAYANT));
        when(configuration.getDelaiReglement()).thenReturn(45);
    }

    @Test
    @DisplayName("encaissements par mode, classés, avec leur part ; pas de caissiers sans le droit « Clients & équipe »")
    void encaissements() {
        when(repository.sommerEncaissementsParJourEtMode(any(), any(), anyCollection())).thenReturn(
            List.of(
                new EncaissementModeJourDTO(LocalDate.of(2026, 9, 2), "CASH", "Espèces", 3_000L),
                new EncaissementModeJourDTO(LocalDate.of(2026, 9, 3), "OM", "Orange Money", 5_000L),
                new EncaissementModeJourDTO(LocalDate.of(2026, 9, 4), "CASH", "Espèces", 4_000L)
            )
        );

        EncaissementsTresorerieDTO encaissements = service.analyserEncaissements(REQUETE);

        assertThat(encaissements.total().valeur()).isEqualTo(12_000);
        assertThat(encaissements.modes()).extracting(ModeEncaissementDTO::libelle, ModeEncaissementDTO::part).containsExactly(
            tuple("Espèces", 7_000 * 100.0 / 12_000),
            tuple("Orange Money", 5_000 * 100.0 / 12_000)
        );
        assertThat(encaissements.series().getFirst().valeurs()).containsExactly(7_000L);
        assertThat(encaissements.caissiers()).isNull();
    }

    @Test
    @DisplayName("délai retenu : observé (3 factures réglées), sinon celui du groupe, sinon celui de l'officine ; retard, échéancier, DSO")
    void tiersPayant() {
        when(repository.sommerFacturationParOrganisme(any(), any())).thenReturn(List.of(new FactureOrganismeDTO("T1", "MUGEFCI", 100_000L, 60_000L)));
        when(repository.listerDelaisObserves(InvoiceStatut.PAID)).thenReturn(List.of(new DelaiObserveDTO("T1", 20.0, 3L), new DelaiObserveDTO("T2", 10.0, 2L)));
        when(repository.listerFacturesNonSoldees(anyCollection())).thenReturn(
            List.of(
                // MUGEFCI, délai observé 20 j : facture du 1er sept. échue le 21 sept., donc en retard.
                new FactureEncoursDTO("T1", "MUGEFCI", LocalDate.of(2026, 9, 1), 40_000L, 30),
                // Organisme T2, historique insuffisant : délai de son groupe, 60 j → échéance le 29 novembre.
                new FactureEncoursDTO("T2", "MCI", LocalDate.of(2026, 9, 30), 10_000L, 60),
                // Organisme T3, ni historique ni groupe : délai de l'officine, 45 j → échéance le 24 novembre.
                new FactureEncoursDTO("T3", "ASCOMA", LocalDate.of(2026, 10, 10), 50_000L, null)
            )
        );

        TiersPayantTresorerieDTO tiersPayant = service.analyserTiersPayant(REQUETE, AUJOURDHUI);

        assertThat(tiersPayant.encours()).isEqualTo(100_000);
        assertThat(tiersPayant.organismes())
            .extracting(OrganismeTresorerieDTO::libelle, OrganismeTresorerieDTO::delaiRetenu, OrganismeTresorerieDTO::origineDelai)
            .containsExactly(tuple("ASCOMA", 45, "DEFAUT"), tuple("MUGEFCI", 20, "OBSERVE"), tuple("MCI", 60, "GROUPE"));
        OrganismeTresorerieDTO mugefci = tiersPayant.organismes().get(1);
        assertThat(mugefci.enRetard()).isEqualTo(40_000);
        assertThat(mugefci.tauxRecouvrement()).isCloseTo(60.0, within(1e-9));
        assertThat(tiersPayant.encaissementsAttendus()).extracting(TrancheMontantDTO::montant).startsWith(40_000L, 0L, 60_000L);
        // Âges : 39 j × 40 000, 10 j × 10 000, 0 j × 50 000 → (1 560 000 + 100 000) / 100 000 = 16,6 → 17 j.
        assertThat(tiersPayant.dso()).isEqualTo(17);
        assertThat(tiersPayant.vieillissement()).extracting(TrancheMontantDTO::montant).containsExactly(60_000L, 40_000L, 0L, 0L);
        assertThat(tiersPayant.concentrationTrois()).isCloseTo(100.0, within(1e-9));
    }

    @Test
    @DisplayName("les différés vieillissent par tranche ; avoirs émis et remboursés sur la période")
    void differes() {
        when(repository.sommerDifferesParAge(eq(AUJOURDHUI.minusDays(30)), any(), any(), any(), any())).thenReturn(
            new com.kobe.warehouse.service.dto.pilotage.DifferesAgeDTO(1_000L, 2_000L, 0L, 5_000L)
        );
        when(repository.sommerAvoirsEmis(any(), any())).thenReturn(new ComptageDTO("", "Émis", 2L, 3L, 4_500L));
        when(repository.sommerAvoirsRembourses(any(), any(), anyCollection())).thenReturn(new ComptageDTO("", "Remboursés", 1L, 1L, 1_500L));

        var differes = service.analyserDifferes(REQUETE, AUJOURDHUI);

        assertThat(differes.encours()).isEqualTo(8_000);
        assertThat(differes.vieillissement()).extracting(TrancheMontantDTO::montant).containsExactly(1_000L, 2_000L, 0L, 5_000L);
        assertThat(differes.avoirsEmis().valeur()).isEqualTo(4_500);
        assertThat(differes.avoirsRembourses().valeur()).isEqualTo(1_500);
    }
}

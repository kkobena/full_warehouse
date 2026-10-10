package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.Granularite;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.ModeAnnees;
import com.kobe.warehouse.service.dto.pilotage.AnneeCompareeDTO;
import com.kobe.warehouse.service.dto.pilotage.ComparaisonAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteAnneesDTO;
import com.kobe.warehouse.service.dto.pilotage.SyntheseAnneeDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import java.time.LocalDate;
import java.util.ArrayList;
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
@DisplayName("Pilotage — comparer les années")
class ComparaisonAnneesServiceImplTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 3, 15);

    @Mock
    private DictionnaireIndicateursService dictionnaireIndicateursService;

    @Mock
    private LecteurFiltrePilotage lecteurFiltrePilotage;

    @Mock
    private CroiseurAnnees croiseurAnnees;

    @InjectMocks
    private ComparaisonAnneesServiceImpl service;

    @BeforeEach
    void troisAnneesDeVentes() {
        when(dictionnaireIndicateursService.filtrerAutorises(anyList())).thenAnswer(appel -> appel.getArgument(0));
        when(dictionnaireIndicateursService.lireDroitsAccordes()).thenReturn(EnumSet.allOf(DroitPilotage.class));
        // Lu depuis 2023, l'année qui précède la première comparée. 2023 : 800 par mois ; 2024 : 1 000 ; 2025 : 1 100, décembre
        // 2 200 ; 2026 : 1 200 en janvier, février et mars (en cours).
        List<MesuresPilotage> mois = new ArrayList<>();
        for (int numero = 1; numero <= 12; numero++) {
            mois.add(ca(800, 20));
        }
        for (int numero = 1; numero <= 12; numero++) {
            mois.add(ca(1_000, 20));
        }
        for (int numero = 1; numero <= 12; numero++) {
            mois.add(ca(numero == 12 ? 2_200 : 1_100, 25));
        }
        mois.add(ca(1_200, 24));
        mois.add(ca(1_200, 24));
        mois.add(ca(1_200, 12));
        when(lecteurFiltrePilotage.lire(argThat(periode -> periode != null && periode.du().equals(LocalDate.of(2023, 1, 1))), eq(Granularite.MOIS), anyList(), any())).thenReturn(mois);
        // 2025 arrêtée au 15 mars : 3 000.
        when(lecteurFiltrePilotage.lire(argThat(periode -> periode != null && periode.au().equals(LocalDate.of(2025, 3, 15))), eq(Granularite.ANNEE), anyList(), any())).thenReturn(
            List.of(ca(3_000, 60))
        );
        // Mars 2025 arrêté au 15 : 500.
        when(lecteurFiltrePilotage.lire(argThat(periode -> periode != null && periode.du().equals(LocalDate.of(2025, 3, 1))), eq(Granularite.ANNEE), anyList(), any())).thenReturn(
            List.of(ca(500, 10))
        );
    }

    @Test
    @DisplayName("l'année en cours se compare à la même date de l'année précédente, et s'arrête au mois en cours")
    void anneeEnCoursADate() {
        ComparaisonAnneesDTO comparaison = service.comparerAnnees(requete(ModeAnnees.MENSUEL, false), AUJOURDHUI);

        AnneeCompareeDTO enCours = comparaison.annees().getLast();
        assertThat(enCours.complete()).isFalse();
        assertThat(enCours.total().valeur()).isEqualTo(3_600);
        assertThat(enCours.total().valeurReference()).isEqualTo(3_000);
        assertThat(enCours.total().ecartPct()).isCloseTo(20.0, within(0.001));
        assertThat(enCours.mois().get(3).valeur()).isNull();
        assertThat(enCours.mois().getFirst().valeurReference()).isEqualTo(1_100);
        assertThat(enCours.mois().get(2).valeurReference()).as("mars en cours, face à mars N-1 au 15").isEqualTo(500);
        assertThat(enCours.trimestres().getFirst().valeurReference()).as("T1 en cours, face au T1 N-1 au 15 mars").isEqualTo(2_700);
        assertThat(comparaison.annees().getFirst().total().ecartPct()).isCloseTo(25.0, within(0.001));
    }

    @Test
    @DisplayName("cumul depuis janvier et douze mois glissants")
    void modes() {
        assertThat(service.comparerAnnees(requete(ModeAnnees.CUMULE, false), AUJOURDHUI).annees().getLast().mois().get(2).valeur()).isEqualTo(3_600);
        // Avril 2025 à mars 2026 : 8 × 1 100 + 2 200 + 3 × 1 200.
        assertThat(service.comparerAnnees(requete(ModeAnnees.GLISSANT, false), AUJOURDHUI).annees().getLast().mois().get(2).valeur()).isEqualTo(14_600);
    }

    @Test
    @DisplayName("par jour ouvré, un mois court se compare juste")
    void parJourOuvre() {
        AnneeCompareeDTO enCours = service.comparerAnnees(requete(ModeAnnees.MENSUEL, true), AUJOURDHUI).annees().getLast();

        assertThat(enCours.mois().get(2).valeur()).isEqualTo(100);
        assertThat(enCours.mois().getFirst().valeur()).isEqualTo(50);
    }

    @Test
    @DisplayName("saisonnalité et croissance annuelle moyenne sur les années closes, mois le plus élevé")
    void lecturesDeSynthese() {
        ComparaisonAnneesDTO comparaison = service.comparerAnnees(requete(ModeAnnees.MENSUEL, false), AUJOURDHUI);

        // Décembre : 2024 = 1/12 ; 2025 = 2 200 / 14 300.
        assertThat(comparaison.saisonnalite().get(11)).isCloseTo((100.0 / 12 + 2_200 * 100.0 / 14_300) / 2, within(0.001));
        assertThat(comparaison.croissanceAnnuelleMoyenne()).isCloseTo((14_300.0 / 12_000 - 1) * 100, within(0.001));
        assertThat(comparaison.croissanceDepuis()).isEqualTo(2024);
        assertThat(comparaison.moisMaximum().annee()).isEqualTo(2025);
        assertThat(comparaison.moisMaximum().mois()).isEqualTo(12);
    }

    @Test
    @DisplayName("sans le droit « Rentabilité », la synthèse ne montre pas la marge")
    void margeMasquee() {
        when(dictionnaireIndicateursService.lireDroitsAccordes()).thenReturn(EnumSet.of(DroitPilotage.PAGE, DroitPilotage.COMPARER_ANNEES));

        assertThat(service.comparerAnnees(requete(ModeAnnees.MENSUEL, false), AUJOURDHUI).synthese()).extracting(SyntheseAnneeDTO::margeBrute).containsOnlyNulls();
    }

    @Test
    @DisplayName("un indicateur non autorisé est refusé ; on compare de 2 à 6 années")
    void controles() {
        when(dictionnaireIndicateursService.filtrerAutorises(anyList())).thenReturn(List.of());
        assertThatThrownBy(() -> service.comparerAnnees(requete(ModeAnnees.MENSUEL, false), AUJOURDHUI)).isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.comparerAnnees(new RequeteAnneesDTO(null, 7, null, null, null, null), AUJOURDHUI)).isInstanceOf(GenericError.class);
    }

    private static RequeteAnneesDTO requete(ModeAnnees mode, boolean parJourOuvre) {
        return new RequeteAnneesDTO(IndicateurPilotage.CA_TTC, 3, mode, parJourOuvre, true, null);
    }

    private static MesuresPilotage ca(long montant, long joursOuvres) {
        return new MesuresPilotage(10, 0, montant, montant, 0, 0, montant, montant, 0, 0, 0, 0, joursOuvres);
    }
}

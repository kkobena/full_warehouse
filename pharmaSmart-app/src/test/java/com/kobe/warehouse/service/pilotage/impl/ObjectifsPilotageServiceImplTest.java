package com.kobe.warehouse.service.pilotage.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.ObjectifPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.repository.ObjectifPilotageRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.pilotage.PeriodeDTO;
import com.kobe.warehouse.service.dto.pilotage.SaisieObjectifsDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviObjectifsDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.calcul.MesuresPilotage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Pilotage — objectifs")
class ObjectifsPilotageServiceImplTest {

    private static final LocalDate LE_10_OCTOBRE = LocalDate.of(2026, 10, 10);
    private static final MesuresPilotage MILLE = lireCa(1_000);

    @Mock
    private ObjectifPilotageRepository repository;

    @Mock
    private LecteurMesuresPilotage lecteurMesuresPilotage;

    @Mock
    private DictionnaireIndicateursService dictionnaireIndicateursService;

    @Mock
    private UserService userService;

    @InjectMocks
    private ObjectifsPilotageServiceImpl service;

    @BeforeEach
    void preparer() {
        when(dictionnaireIndicateursService.filtrerAutorises(anyList())).thenReturn(List.of(IndicateurPilotage.CA_TTC, IndicateurPilotage.TAUX_REMISE));
        when(userService.getUser()).thenReturn(new AppUser());
    }

    @Test
    @DisplayName("suivi : mois par mois, cumul des mois clos ayant un objectif, projection du mois en cours")
    void suivre() {
        when(repository.listerParAnnee(2026)).thenReturn(
            List.of(lireObjectif(IndicateurPilotage.CA_TTC, 1, 900), lireObjectif(IndicateurPilotage.CA_TTC, 2, 1_200), lireObjectif(IndicateurPilotage.CA_TTC, 10, 5_000))
        );
        when(lecteurMesuresPilotage.lireParTranche(eq(new PeriodeDTO(LocalDate.of(2026, 1, 1), LE_10_OCTOBRE)), anyList())).thenReturn(Collections.nCopies(10, MILLE));
        when(lecteurMesuresPilotage.lireMemeMoisN1(LE_10_OCTOBRE)).thenReturn(List.of(lireCa(300), lireCa(700)));

        SuiviObjectifsDTO suivi = service.suivre(2026, LE_10_OCTOBRE);
        SuiviIndicateurDTO ca = suivi.indicateurs().getFirst();

        assertThat(suivi.jusquAu()).isEqualTo(LE_10_OCTOBRE);
        assertThat(ca.mois().getFirst().tenu()).isTrue();
        assertThat(ca.mois().get(1).tenu()).isFalse();
        assertThat(ca.mois().get(9).enCours()).isTrue();
        assertThat(ca.mois().get(10).realise()).isNull();
        assertThat(ca.cumul()).satisfies(cumul -> {
            assertThat(cumul.mois()).isEqualTo(2);
            assertThat(cumul.objectif()).isEqualTo(2_100.0);
            assertThat(cumul.realise()).isEqualTo(2_000.0);
        });
        assertThat(ca.projection().projection()).isCloseTo(3_333.33, within(0.01));
        assertThat(ca.projection().objectif()).isEqualTo(5_000.0);
        assertThat(suivi.indicateurs().get(1).cumul()).as("un taux ne se cumule pas").isNull();
    }

    @Test
    @DisplayName("une année close n'a pas de projection ; une année à venir n'a pas de réalisé")
    void suivreAnneesCloseEtAVenir() {
        when(lecteurMesuresPilotage.lireParTranche(any(), anyList())).thenReturn(Collections.nCopies(12, MILLE));

        assertThat(service.suivre(2025, LE_10_OCTOBRE).indicateurs().getFirst().projection()).isNull();
        assertThat(service.suivre(2027, LE_10_OCTOBRE).indicateurs().getFirst().mois()).allSatisfy(mois -> assertThat(mois.realise()).isNull());
    }

    @Test
    @DisplayName("enregistrer : ce qui n'a pas changé reste, un mois vidé est effacé, un nouveau mois est créé")
    void enregistrer() {
        ObjectifPilotage janvier = lireObjectif(IndicateurPilotage.CA_TTC, 1, 900);
        ObjectifPilotage fevrier = lireObjectif(IndicateurPilotage.CA_TTC, 2, 1_200);
        when(repository.findAllByAnneeAndIndicateur(2026, IndicateurPilotage.CA_TTC)).thenReturn(List.of(janvier, fevrier));
        List<Double> mois = new ArrayList<>(Collections.nCopies(12, null));
        mois.set(0, 900.0);
        mois.set(2, 1_500.0);

        service.enregistrer(new SaisieObjectifsDTO(2026, IndicateurPilotage.CA_TTC, mois), LocalDate.of(2025, 12, 15));

        ArgumentCaptor<ObjectifPilotage> enregistre = ArgumentCaptor.forClass(ObjectifPilotage.class);
        verify(repository).save(enregistre.capture());
        assertThat(enregistre.getValue().getMois()).isEqualTo(3);
        assertThat(enregistre.getValue().getValeur()).isEqualTo(1_500.0);
        verify(repository).delete(fevrier);
        verify(repository, never()).save(janvier);
    }

    @Test
    @DisplayName("mois clos : sa valeur ne change plus ; le mois en cours et les suivants restent modifiables")
    void refuserMoisClos() {
        ObjectifPilotage septembre = lireObjectif(IndicateurPilotage.CA_TTC, 9, 1_000);
        when(repository.findAllByAnneeAndIndicateur(2026, IndicateurPilotage.CA_TTC)).thenReturn(List.of(septembre));
        when(repository.findAllByAnneeAndIndicateur(2025, IndicateurPilotage.CA_TTC)).thenReturn(List.of());
        List<Double> inchange = new ArrayList<>(Collections.nCopies(12, null));
        inchange.set(8, 1_000.0);
        inchange.set(9, 2_000.0);

        service.enregistrer(new SaisieObjectifsDTO(2026, IndicateurPilotage.CA_TTC, inchange), LE_10_OCTOBRE);

        verify(repository, never()).save(septembre);
        List<Double> septembreBaisse = new ArrayList<>(inchange);
        septembreBaisse.set(8, 800.0);
        assertThatThrownBy(() -> service.enregistrer(new SaisieObjectifsDTO(2026, IndicateurPilotage.CA_TTC, septembreBaisse), LE_10_OCTOBRE))
            .isInstanceOf(GenericError.class)
            .hasMessageContaining("septembre 2026");
        List<Double> aoutAjoute = new ArrayList<>(inchange);
        aoutAjoute.set(7, 500.0);
        assertThatThrownBy(() -> service.enregistrer(new SaisieObjectifsDTO(2026, IndicateurPilotage.CA_TTC, aoutAjoute), LE_10_OCTOBRE)).isInstanceOf(
            GenericError.class
        );
        assertThatThrownBy(() -> service.enregistrer(new SaisieObjectifsDTO(2025, IndicateurPilotage.CA_TTC, inchange), LE_10_OCTOBRE)).isInstanceOf(
            GenericError.class
        );
    }

    @Test
    @DisplayName("grille : nombre de mois clos selon l'année")
    void compterMoisClos() {
        assertThat(service.lireGrille(2025, LE_10_OCTOBRE).moisClos()).isEqualTo(12);
        assertThat(service.lireGrille(2026, LE_10_OCTOBRE).moisClos()).isEqualTo(9);
        assertThat(service.lireGrille(2027, LE_10_OCTOBRE).moisClos()).isZero();
    }

    @Test
    @DisplayName("un indicateur hors objectifs, ou non visible, est refusé")
    void refuserIndicateur() {
        assertThatThrownBy(() -> service.enregistrer(new SaisieObjectifsDTO(2026, IndicateurPilotage.NB_VENTES, Collections.nCopies(12, null)), LE_10_OCTOBRE)).isInstanceOf(
            GenericError.class
        );
    }

    @Test
    @DisplayName("proposer : un montant d'après N-1 + x %, un taux repris de N-1")
    void proposer() {
        MesuresPilotage remise = new MesuresPilotage(1, 0, 1_000, 1_000, 43, 0, 1_000, 1_000, 0, 1, 0, 0, 1);
        when(lecteurMesuresPilotage.lireParTranche(eq(new PeriodeDTO(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31))), anyList())).thenReturn(
            Collections.nCopies(12, remise)
        );

        assertThat(service.proposer(2026, IndicateurPilotage.CA_TTC, 5)).hasSize(12).allSatisfy(valeur -> assertThat(valeur).isEqualTo(1_050.0));
        assertThat(service.proposer(2026, IndicateurPilotage.TAUX_REMISE, 5)).allSatisfy(valeur -> assertThat(valeur).isEqualTo(4.3));
    }

    private static ObjectifPilotage lireObjectif(IndicateurPilotage indicateur, int mois, double valeur) {
        return new ObjectifPilotage().setIndicateur(indicateur).setAnnee(2026).setMois(mois).setValeur(valeur).setModifieLe(LocalDateTime.of(2026, 1, mois, 9, 0));
    }

    private static MesuresPilotage lireCa(long caTtc) {
        return new MesuresPilotage(1, 0, caTtc, caTtc, 0, 0, caTtc, caTtc, 0, 1, 0, 0, 1);
    }
}

package com.kobe.warehouse.service.pilotage.alertes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.GraviteAlerte;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SourceAnalyse;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AnalysePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ElementAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.OrganismeTresorerieDTO;
import com.kobe.warehouse.service.dto.pilotage.ProjectionObjectifDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.SeriesPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.SuiviObjectifsDTO;
import com.kobe.warehouse.service.dto.pilotage.TiersPayantTresorerieDTO;
import com.kobe.warehouse.service.pilotage.AnalysePilotageService;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.ObjectifsPilotageService;
import com.kobe.warehouse.service.pilotage.SeriesPilotageService;
import com.kobe.warehouse.service.pilotage.TresoreriePilotageService;
import com.kobe.warehouse.service.pilotage.alertes.regles.RegleAchatsQuiDerapent;
import com.kobe.warehouse.service.pilotage.alertes.regles.RegleChuteActivite;
import com.kobe.warehouse.service.pilotage.alertes.regles.RegleCreancesOrganisme;
import com.kobe.warehouse.service.pilotage.alertes.regles.RegleErosionMarge;
import com.kobe.warehouse.service.pilotage.alertes.regles.RegleFamilleEnRecul;
import com.kobe.warehouse.service.pilotage.alertes.regles.RegleObjectifMenace;
import com.kobe.warehouse.service.pilotage.impl.AlertesPilotageServiceImpl;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Pilotage — alertes")
class AlertesPilotageTest {

    private static final LocalDate LE_10_OCTOBRE = LocalDate.of(2026, 10, 10);

    private final SeriesPilotageService series = mock(SeriesPilotageService.class);
    private final AppConfigurationService configuration = mock(AppConfigurationService.class);

    @BeforeEach
    void seuilsParDefaut() {
        when(configuration.getAlerteChuteActivitePilotage()).thenReturn(90.0);
        when(configuration.getAlerteFamilleReculPilotage()).thenReturn(15.0);
        when(configuration.getAlerteErosionMargePilotage()).thenReturn(1.0);
        when(configuration.getAlerteObjectifMenacePilotage()).thenReturn(95.0);
        when(configuration.getAlerteRatioAchatsPilotage()).thenReturn(0.8);
        when(configuration.getAlerteDsoOrganismePilotage()).thenReturn(90.0);
    }

    @Nested
    @DisplayName("règles")
    class Regles {

        @Test
        @DisplayName("chute d'activité : sous 90 % des mêmes jours N-1, et pas au-dessus")
        void chuteActivite() {
            RegleChuteActivite regle = new RegleChuteActivite(series, configuration);

            repondre(serie(IndicateurPilotage.CA_TTC, 850.0, 1_000.0, null));
            assertThat(regle.evaluer(LE_10_OCTOBRE)).singleElement().satisfies(alerte -> {
                assertThat(alerte.gravite()).isEqualTo(GraviteAlerte.HAUTE);
                assertThat(alerte.detail()).contains("85 %");
            });

            repondre(serie(IndicateurPilotage.CA_TTC, 950.0, 1_000.0, null));
            assertThat(regle.evaluer(LE_10_OCTOBRE)).isEmpty();
        }

        @Test
        @DisplayName("marge qui s'érode : plus d'un point perdu")
        void erosionMarge() {
            RegleErosionMarge regle = new RegleErosionMarge(series, configuration);

            repondre(serie(IndicateurPilotage.TAUX_MARGE, 26.5, 28.0, -1.5));
            assertThat(regle.evaluer(LE_10_OCTOBRE)).singleElement().extracting(AlertePilotageDTO::detail).asString().contains("1,5 point(s)");

            repondre(serie(IndicateurPilotage.TAUX_MARGE, 27.5, 28.0, -0.5));
            assertThat(regle.evaluer(LE_10_OCTOBRE)).isEmpty();
        }

        @Test
        @DisplayName("achats qui dérapent : ventes sous 0,8 fois les achats sur 30 jours")
        void achatsQuiDerapent() {
            RegleAchatsQuiDerapent regle = new RegleAchatsQuiDerapent(series, configuration);
            when(series.calculerSeries(any())).thenReturn(
                new SeriesPilotageDTO(null, 0, 0, List.of(serie(IndicateurPilotage.CA_TTC, 700.0, null, null), serie(IndicateurPilotage.ACHATS_TTC, 1_000.0, null, null)))
            );

            assertThat(regle.evaluer(LE_10_OCTOBRE)).singleElement().extracting(AlertePilotageDTO::detail).asString().contains("0,7 fois");
        }

        @Test
        @DisplayName("familles en recul : seules les familles qui pèsent au moins 2 % du CA N-1 comptent")
        void famillesEnRecul() {
            AnalysePilotageService analyse = mock(AnalysePilotageService.class);
            when(analyse.analyser(any(), any())).thenReturn(
                analyser(
                    new CelluleAnalyseDTO(10_000.0, 12_000.0, null, null),
                    element("Antalgiques", 4_000.0, 5_000.0, -20.0),
                    element("Cosmétiques", 100.0, 200.0, -50.0),
                    element("Vitamines", 3_000.0, 3_100.0, -3.2)
                )
            );

            assertThat(new RegleFamilleEnRecul(analyse, configuration).evaluer(LE_10_OCTOBRE)).singleElement().satisfies(alerte -> {
                assertThat(alerte.titre()).isEqualTo("Une famille en recul");
                assertThat(alerte.detail()).contains("Antalgiques (-20 %)").doesNotContain("Cosmétiques");
            });
        }

        @Test
        @DisplayName("objectif menacé : projection sous 95 % ; un plafond dépassé")
        void objectifMenace() {
            ObjectifsPilotageService objectifs = mock(ObjectifsPilotageService.class);
            when(objectifs.suivre(2026, LE_10_OCTOBRE)).thenReturn(
                new SuiviObjectifsDTO(
                    2026,
                    LE_10_OCTOBRE,
                    List.of(
                        suivi(IndicateurPilotage.CA_TTC, new ProjectionObjectifDTO(1_000.0, 3_000.0, 5_000.0, 60.0, false, "")),
                        suivi(IndicateurPilotage.MARGE_BRUTE, new ProjectionObjectifDTO(500.0, 970.0, 1_000.0, 97.0, false, "")),
                        suivi(IndicateurPilotage.TAUX_REMISE, new ProjectionObjectifDTO(5.0, 5.0, 4.0, 125.0, false, ""))
                    )
                )
            );

            assertThat(new RegleObjectifMenace(objectifs, configuration).evaluer(LE_10_OCTOBRE))
                .extracting(AlertePilotageDTO::code)
                .containsExactly("OBJECTIF_MENACE_CA_TTC", "OBJECTIF_MENACE_TAUX_REMISE");
        }

        @Test
        @DisplayName("créances : organismes au-delà de 90 jours de chiffre, encours non nul")
        void creances() {
            TresoreriePilotageService tresorerie = mock(TresoreriePilotageService.class);
            when(tresorerie.analyserTiersPayant(any(), any())).thenReturn(
                new TiersPayantTresorerieDTO(
                    null,
                    null,
                    null,
                    0,
                    null,
                    List.of(organisme("MUGEF-CI", 500, 120), organisme("SOLDE", 0, 200), organisme("ASCOMA", 300, 45)),
                    List.of(),
                    List.of(),
                    null,
                    null,
                    3
                )
            );

            assertThat(new RegleCreancesOrganisme(tresorerie, configuration).evaluer(LE_10_OCTOBRE)).singleElement().extracting(AlertePilotageDTO::detail).asString().isEqualTo(
                "Encours au-delà de 90 jours de chiffre : MUGEF-CI (120 j)."
            );
        }
    }

    @Nested
    @DisplayName("service")
    class Service {

        private final DictionnaireIndicateursService dictionnaire = mock(DictionnaireIndicateursService.class);

        @Test
        @DisplayName("une règle sans droit n'est pas évaluée, une règle en échec n'empêche pas les autres, les plus graves d'abord")
        void listerAlertes() {
            RegleAlerte moyenne = regle(DroitPilotage.TABLEAU_DE_BORD, GraviteAlerte.MOYENNE);
            RegleAlerte haute = regle(DroitPilotage.TABLEAU_DE_BORD, GraviteAlerte.HAUTE);
            RegleAlerte enEchec = regle(DroitPilotage.TABLEAU_DE_BORD, null);
            when(enEchec.evaluer(LE_10_OCTOBRE)).thenThrow(new IllegalStateException("panne"));
            RegleAlerte sansDroit = regle(DroitPilotage.TRESORERIE_TIERS_PAYANT, GraviteAlerte.HAUTE);
            when(dictionnaire.lireDroitsAccordes()).thenReturn(Set.of(DroitPilotage.TABLEAU_DE_BORD));

            List<AlertePilotageDTO> alertes = new AlertesPilotageServiceImpl(List.of(moyenne, enEchec, haute, sansDroit), dictionnaire).listerAlertes(LE_10_OCTOBRE);

            assertThat(alertes).extracting(AlertePilotageDTO::gravite).containsExactly(GraviteAlerte.HAUTE, GraviteAlerte.MOYENNE);
            verify(sansDroit, never()).evaluer(any());
        }

        private RegleAlerte regle(DroitPilotage droit, GraviteAlerte gravite) {
            RegleAlerte regle = mock(RegleAlerte.class);
            when(regle.lireDroit()).thenReturn(droit);
            if (gravite != null) {
                when(regle.evaluer(LE_10_OCTOBRE)).thenReturn(List.of(new AlertePilotageDTO(gravite.name(), gravite, "", "", "tableau-de-bord")));
            }
            return regle;
        }
    }

    private void repondre(SerieIndicateurDTO serie) {
        when(series.calculerSeries(any())).thenReturn(new SeriesPilotageDTO(null, 0, 0, List.of(serie)));
    }

    private static SerieIndicateurDTO serie(IndicateurPilotage indicateur, Double valeur, Double reference, Double ecart) {
        return new SerieIndicateurDTO(IndicateurPilotageDTO.fromIndicateur(indicateur), valeur, reference, ecart, null, List.of(), null, null);
    }

    private static SuiviIndicateurDTO suivi(IndicateurPilotage indicateur, ProjectionObjectifDTO projection) {
        return new SuiviIndicateurDTO(IndicateurPilotageDTO.fromIndicateur(indicateur), List.of(), null, projection);
    }

    private static ElementAnalyseDTO element(String libelle, double valeur, double reference, double ecartPct) {
        return new ElementAnalyseDTO(libelle, libelle, List.of(new CelluleAnalyseDTO(valeur, reference, valeur - reference, ecartPct)), null, null);
    }

    private static AnalysePilotageDTO analyser(CelluleAnalyseDTO total, ElementAnalyseDTO... elements) {
        return new AnalysePilotageDTO(
            null,
            SourceAnalyse.LIGNES,
            List.of(IndicateurPilotageDTO.fromIndicateur(IndicateurPilotage.CA_TTC)),
            List.of(),
            AxeAnalyseDTO.fromAxe(AxeAnalyse.FAMILLE),
            null,
            List.of(total),
            List.of(elements),
            null,
            elements.length,
            null
        );
    }

    private static OrganismeTresorerieDTO organisme(String libelle, long encours, Integer dso) {
        return new OrganismeTresorerieDTO(libelle, libelle, 0, 0, null, encours, null, dso, 0, 30, "DEFAUT");
    }
}

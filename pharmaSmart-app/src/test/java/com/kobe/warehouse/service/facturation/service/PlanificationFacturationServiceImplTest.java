package com.kobe.warehouse.service.facturation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.PlanificationFacturation;
import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import com.kobe.warehouse.domain.enumeration.OrigineGeneration;
import com.kobe.warehouse.domain.enumeration.Periodicite;
import com.kobe.warehouse.repository.GroupeTiersPayantRepository;
import com.kobe.warehouse.repository.HistoriquePlanificationRepository;
import com.kobe.warehouse.repository.PlanificationFacturationRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import com.kobe.warehouse.service.facturation.dto.PlanificationDto;
import com.kobe.warehouse.service.facturation.registry.FacturationServiceRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Le calendrier de la facturation programmée, éprouvé sans base.
 *
 * <p>Tout ce service tient sur une question : quelle période facturer, et quand. La réponse se
 * calcule à partir de la fin de la dernière période et de la périodicité — quatre règles distinctes,
 * dont deux à cas limites (la quinzaine bascule au 15, l'hebdomadaire tombe sur le dimanche). Ces
 * combinaisons se couvrent en un tableau ici ; les reproduire en base coûterait beaucoup pour ne
 * rien prouver de plus, puisqu'aucune ne dépend d'une lecture.
 *
 * <p>Le second point décidé ici est l'arbitrage groupe / individuel : les organismes réunis en
 * groupe sont facturés ensemble, les autres un par un, et un organisme déjà couvert par un groupe
 * ne doit pas être facturé deux fois.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlanificationFacturationServiceImpl")
class PlanificationFacturationServiceImplTest {

    @Mock
    private PlanificationFacturationRepository planificationRepository;

    @Mock
    private HistoriquePlanificationRepository historiqueRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private FacturationServiceRegistry facturationServiceRegistry;

    @Mock
    private TiersPayantRepository tiersPayantRepository;

    @Mock
    private GroupeTiersPayantRepository groupeTiersPayantRepository;

    @Mock
    private PlanificationStatutService statutService;

    private PlanificationFacturationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PlanificationFacturationServiceImpl(
            planificationRepository,
            historiqueRepository,
            facturationServiceRegistry,
            tiersPayantRepository,
            groupeTiersPayantRepository,
            statutService
        );
    }

    @Nested
    @DisplayName("calcul de la prochaine exécution")
    class ProchaineExecution {

        @ParameterizedTest(name = "{0} après une période finie le {1} → prochaine exécution le {2}")
        @CsvSource({
            // La prochaine période démarre au lendemain de la dernière ; l'exécution a lieu le
            // lendemain de sa fin, une fois la période entièrement écoulée.
            "MENSUEL, 2026-01-31, 2026-03-01",
            "MENSUEL, 2026-02-28, 2026-04-01",
            "BIMENSUEL, 2026-01-31, 2026-04-01",
            "QUINZAINE, 2026-01-15, 2026-02-01",
            "QUINZAINE, 2026-01-31, 2026-02-16",
            "HEBDOMADAIRE, 2026-01-05, 2026-01-12",
        })
        void calendrier(Periodicite periodicite, LocalDate dernierePeriodeFin, LocalDate prochaineExecution) {
            when(planificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            PlanificationDto cree = service.create(dto(periodicite, dernierePeriodeFin, LocalTime.of(6, 30)));

            assertThat(cree.prochaineExecution()).isEqualTo(prochaineExecution.atTime(6, 30));
        }

        @Test
        @DisplayName("sans heure choisie, la planification se déclenche à 8 h")
        void heureParDefaut() {
            when(planificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            PlanificationDto cree = service.create(dto(Periodicite.MENSUEL, LocalDate.of(2026, 1, 31), null));

            assertThat(cree.prochaineExecution()).isEqualTo(LocalDate.of(2026, 3, 1).atTime(8, 0));
        }

        @Test
        @DisplayName("sans dernière période connue, aucune exécution n'est programmée")
        void sansDernierePeriode() {
            when(planificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            PlanificationDto cree = service.create(dto(Periodicite.MENSUEL, null, LocalTime.of(9, 0)));

            assertThat(cree.prochaineExecution()).isNull();
        }
    }

    @Nested
    @DisplayName("activation")
    class Activation {

        @Test
        @DisplayName("réactiver une planification dont l'échéance est passée la reprogramme")
        void reprogrammationALActivation() {
            PlanificationFacturation plan = entite(Periodicite.MENSUEL, LocalDate.of(2026, 1, 31));
            plan.setActif(false);
            plan.setProchaineExecution(LocalDateTime.now().minusMonths(2));
            when(planificationRepository.getReferenceById(1)).thenReturn(plan);

            service.toggleActif(1);

            assertThat(plan.isActif()).isTrue();
            assertThat(plan.getProchaineExecution()).isEqualTo(LocalDate.of(2026, 3, 1).atTime(6, 30));
        }

        @Test
        @DisplayName("désactiver laisse l'échéance en place")
        void desactivationSansRecalcul() {
            PlanificationFacturation plan = entite(Periodicite.MENSUEL, LocalDate.of(2026, 1, 31));
            plan.setActif(true);
            LocalDateTime echeance = LocalDateTime.now().plusMonths(1);
            plan.setProchaineExecution(echeance);
            when(planificationRepository.getReferenceById(1)).thenReturn(plan);

            service.toggleActif(1);

            assertThat(plan.isActif()).isFalse();
            assertThat(plan.getProchaineExecution()).isEqualTo(echeance);
        }

        @Test
        @DisplayName("réactiver une planification dont l'échéance est à venir ne la déplace pas")
        void reactivationSansDeplacement() {
            PlanificationFacturation plan = entite(Periodicite.MENSUEL, LocalDate.of(2026, 1, 31));
            plan.setActif(false);
            LocalDateTime echeance = LocalDateTime.now().plusMonths(1);
            plan.setProchaineExecution(echeance);
            when(planificationRepository.getReferenceById(1)).thenReturn(plan);

            service.toggleActif(1);

            assertThat(plan.getProchaineExecution()).isEqualTo(echeance);
        }
    }

    @Nested
    @DisplayName("arbitrage groupe / individuel")
    class ChoixDesOrganismes {

        @Test
        @DisplayName("les organismes réunis en groupe sont facturés ensemble, les autres un par un")
        void groupesPuisIndividuels() {
            PlanificationFacturation plan = entite(Periodicite.MENSUEL, LocalDate.of(2026, 1, 31));
            when(planificationRepository.getReferenceById(1)).thenReturn(plan);
            when(groupeTiersPayantRepository.findIdsForAutoGenerationDefinitive(Periodicite.MENSUEL)).thenReturn(List.of(7, 8));
            when(
                tiersPayantRepository.findIdsNotInGroupsForAutoGenerationDefinitive(Periodicite.MENSUEL, Set.of(7, 8))
            ).thenReturn(List.of(21));
            when(facturationServiceRegistry.getService(any()).createFactureEdition(any())).thenReturn(
                new FactureEditionResponse(202601001, true)
            );

            service.executerMaintenant(1);

            ArgumentCaptor<EditionSearchParams> params = ArgumentCaptor.forClass(EditionSearchParams.class);
            verify(facturationServiceRegistry.getService(ModeEditionEnum.GROUP), org.mockito.Mockito.atLeastOnce())
                .createFactureEdition(params.capture());
            List<EditionSearchParams> captures = params.getAllValues();
            assertThat(captures)
                .anySatisfy(p -> {
                    assertThat(p.modeEdition()).isEqualTo(ModeEditionEnum.GROUP);
                    assertThat(p.groupIds()).containsExactlyInAnyOrder(7, 8);
                })
                .anySatisfy(p -> {
                    assertThat(p.modeEdition()).isEqualTo(ModeEditionEnum.TIERS_PAYANT);
                    assertThat(p.tiersPayantIds()).containsExactly(21);
                });
            assertThat(captures).allSatisfy(p -> {
                assertThat(p.origineGeneration()).isEqualTo(OrigineGeneration.AUTO);
                assertThat(p.startDate()).isEqualTo(LocalDate.of(2026, 2, 1));
                assertThat(p.endDate()).isEqualTo(LocalDate.of(2026, 2, 28));
            });
        }

        @Test
        @DisplayName("sans groupe éligible, tous les organismes de la périodicité sont pris")
        void aucunGroupe() {
            PlanificationFacturation plan = entite(Periodicite.MENSUEL, LocalDate.of(2026, 1, 31));
            when(planificationRepository.getReferenceById(1)).thenReturn(plan);
            when(groupeTiersPayantRepository.findIdsForAutoGenerationDefinitive(Periodicite.MENSUEL)).thenReturn(List.of());
            when(tiersPayantRepository.findAllIdsForAutoGenerationDefinitive(Periodicite.MENSUEL)).thenReturn(List.of(3, 4));
            when(facturationServiceRegistry.getService(any()).createFactureEdition(any())).thenReturn(
                new FactureEditionResponse(202601002, false)
            );

            service.executerMaintenant(1);

            verify(tiersPayantRepository, never()).findIdsNotInGroupsForAutoGenerationDefinitive(any(), any());
        }

        @Test
        @DisplayName("aucun organisme éligible : rien n'est édité et la planification reste en succès")
        void aucunOrganisme() {
            PlanificationFacturation plan = entite(Periodicite.MENSUEL, LocalDate.of(2026, 1, 31));
            when(planificationRepository.getReferenceById(1)).thenReturn(plan);
            when(groupeTiersPayantRepository.findIdsForAutoGenerationDefinitive(Periodicite.MENSUEL)).thenReturn(List.of());
            when(tiersPayantRepository.findAllIdsForAutoGenerationDefinitive(Periodicite.MENSUEL)).thenReturn(List.of());

            FactureEditionResponse reponse = service.executerMaintenant(1);

            assertThat(reponse.generationCode()).isNull();
            assertThat(plan.getDernierStatut()).isEqualTo(ExecutionStatut.SUCCESS);
        }

        @Test
        @DisplayName("une planification provisoire interroge les organismes éligibles au provisoire")
        void modeProvisoire() {
            PlanificationFacturation plan = entite(Periodicite.QUINZAINE, LocalDate.of(2026, 1, 15));
            plan.setFactureProvisoire(true);
            when(planificationRepository.getReferenceById(1)).thenReturn(plan);
            when(groupeTiersPayantRepository.findIdsForAutoGenerationProvisoire(Periodicite.QUINZAINE)).thenReturn(List.of());
            when(tiersPayantRepository.findAllIdsForAutoGenerationProvisoire(Periodicite.QUINZAINE)).thenReturn(List.of(5));
            when(facturationServiceRegistry.getService(any()).createFactureEdition(any())).thenReturn(
                new FactureEditionResponse(202601003, false)
            );

            service.executerMaintenant(1);

            ArgumentCaptor<EditionSearchParams> params = ArgumentCaptor.forClass(EditionSearchParams.class);
            verify(facturationServiceRegistry.getService(ModeEditionEnum.TIERS_PAYANT)).createFactureEdition(params.capture());
            assertThat(params.getValue().factureProvisoire()).isTrue();
            assertThat(params.getValue().endDate())
                .as("du 16 au dernier jour du mois")
                .isEqualTo(LocalDate.of(2026, 1, 31));
        }
    }

    @Nested
    @DisplayName("exécution en échec")
    class Echec {

        @Test
        @DisplayName("une planification sans dernière période connue échoue sans rien éditer")
        void sansDernierePeriodeFin() {
            PlanificationFacturation plan = entite(Periodicite.MENSUEL, null);
            when(planificationRepository.getReferenceById(1)).thenReturn(plan);

            FactureEditionResponse reponse = service.executerMaintenant(1);

            assertThat(reponse.generationCode()).isNull();
            assertThat(plan.getDernierStatut()).isEqualTo(ExecutionStatut.ECHEC);
            assertThat(plan.getDernierMessage()).contains("dernierePeriodeFin");
            verify(historiqueRepository).save(any());
        }

        @Test
        @DisplayName("un échec laisse l'échéance en place plutôt que de la reculer")
        void echeanceInchangeeApresEchec() {
            PlanificationFacturation plan = entite(Periodicite.MENSUEL, null);
            LocalDateTime echeance = LocalDateTime.now().plusDays(3);
            plan.setProchaineExecution(echeance);
            when(planificationRepository.getReferenceById(1)).thenReturn(plan);

            service.executerMaintenant(1);

            assertThat(plan.getProchaineExecution()).isEqualTo(echeance);
        }

        @Test
        @DisplayName("le scheduler avance quand même l'échéance après un échec, pour ne pas boucler")
        void schedulerAvanceApresEchec() {
            PlanificationFacturation plan = entite(Periodicite.MENSUEL, LocalDate.of(2026, 1, 31));
            when(groupeTiersPayantRepository.findIdsForAutoGenerationDefinitive(Periodicite.MENSUEL)).thenThrow(
                new IllegalStateException("base injoignable")
            );

            service.executerPlanificationScheduled(plan);

            verify(statutService).sauvegarderEchec(
                org.mockito.ArgumentMatchers.eq(plan),
                any(),
                org.mockito.ArgumentMatchers.eq("base injoignable"),
                org.mockito.ArgumentMatchers.eq(LocalDate.of(2026, 3, 1).atTime(6, 30))
            );
        }
    }

    // ===== outils =====

    private PlanificationDto dto(Periodicite periodicite, LocalDate dernierePeriodeFin, LocalTime heure) {
        return new PlanificationDto(
            null,
            "Facturation " + periodicite,
            periodicite,
            heure,
            false,
            true,
            dernierePeriodeFin,
            null,
            null,
            null,
            null,
            0L
        );
    }

    private PlanificationFacturation entite(Periodicite periodicite, LocalDate dernierePeriodeFin) {
        PlanificationFacturation plan = new PlanificationFacturation();
        plan.setId(1);
        plan.setLibelle("Facturation " + periodicite);
        plan.setPeriodicite(periodicite);
        plan.setDernierePeriodeFin(dernierePeriodeFin);
        plan.setHeureDeclenchement(LocalTime.of(6, 30));
        return plan;
    }
}

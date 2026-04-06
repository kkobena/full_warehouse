package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.HistoriquePlanification;
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
import com.kobe.warehouse.service.facturation.dto.HistoriquePlanificationDto;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import com.kobe.warehouse.service.facturation.dto.ModeEditionSort;
import com.kobe.warehouse.service.facturation.dto.PlanificationDto;
import com.kobe.warehouse.service.facturation.registry.FacturationServiceRegistry;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlanificationFacturationServiceImpl implements PlanificationFacturationService {

    private static final Logger log = LoggerFactory.getLogger(PlanificationFacturationServiceImpl.class);

    private final PlanificationFacturationRepository planificationRepository;
    private final HistoriquePlanificationRepository historiqueRepository;
    private final FacturationServiceRegistry facturationServiceRegistry;
    private final TiersPayantRepository tiersPayantRepository;
    private final GroupeTiersPayantRepository groupeTiersPayantRepository;
    private final PlanificationStatutService statutService;

    public PlanificationFacturationServiceImpl(
        PlanificationFacturationRepository planificationRepository,
        HistoriquePlanificationRepository historiqueRepository,
        FacturationServiceRegistry facturationServiceRegistry,
        TiersPayantRepository tiersPayantRepository,
        GroupeTiersPayantRepository groupeTiersPayantRepository,
        PlanificationStatutService statutService
    ) {
        this.planificationRepository = planificationRepository;
        this.historiqueRepository = historiqueRepository;
        this.facturationServiceRegistry = facturationServiceRegistry;
        this.tiersPayantRepository = tiersPayantRepository;
        this.groupeTiersPayantRepository = groupeTiersPayantRepository;
        this.statutService = statutService;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PlanificationDto> findAll() {
        return planificationRepository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public PlanificationDto create(PlanificationDto dto) {
        PlanificationFacturation entity = new PlanificationFacturation();
        mapDtoToEntity(dto, entity);
        entity.setProchaineExecution(calculerProchaine(entity));
        return toDto(planificationRepository.save(entity));
    }

    @Override
    public PlanificationDto update(Integer id, PlanificationDto dto) {
        PlanificationFacturation entity = planificationRepository.getReferenceById(id);
        mapDtoToEntity(dto, entity);
        entity.setProchaineExecution(calculerProchaine(entity));
        return toDto(planificationRepository.save(entity));
    }

    @Override
    public void toggleActif(Integer id) {
        PlanificationFacturation entity = planificationRepository.getReferenceById(id);
        boolean nouvelEtat = !entity.isActif();
        entity.setActif(nouvelEtat);
        // À l'activation, recalculer prochaineExecution si elle est nulle ou dans le passé
        if (nouvelEtat && (entity.getProchaineExecution() == null
                || entity.getProchaineExecution().isBefore(LocalDateTime.now()))) {
            entity.setProchaineExecution(calculerProchaine(entity));
        }
        planificationRepository.save(entity);
    }

    @Override
    public void delete(Integer id) {
        planificationRepository.deleteById(id);
    }

    // ── Exécution ─────────────────────────────────────────────────────────────

    @Override
    public FactureEditionResponse executerMaintenant(Integer id) {
        PlanificationFacturation plan = planificationRepository.getReferenceById(id);
        LocalDateTime debut = LocalDateTime.now();
        try {
            LocalDate[] periode = buildPeriode(plan);
            FactureEditionResponse response = executeAutoGeneration(plan, periode[0], periode[1]);
            enregistrerHistorique(plan, debut, LocalDateTime.now(), ExecutionStatut.SUCCESS, response.generationCode(), null);
            plan.setDernierePeriodeFin(periode[1]);
            plan.setDerniereExecution(debut);
            plan.setDernierStatut(ExecutionStatut.SUCCESS);
            plan.setDernierMessage(null);
            plan.setProchaineExecution(calculerProchaine(plan));
            planificationRepository.save(plan);
            return response;
        } catch (Exception e) {
            log.error("Erreur lors de l'exécution manuelle de la planification {}: {}", id, e.getMessage(), e);
            enregistrerHistorique(plan, debut, LocalDateTime.now(), ExecutionStatut.ECHEC, null, e.getMessage());
            plan.setDerniereExecution(debut);
            plan.setDernierStatut(ExecutionStatut.ECHEC);
            plan.setDernierMessage(e.getMessage());
            planificationRepository.save(plan);
            return new FactureEditionResponse(null, false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistoriquePlanificationDto> getHistorique(Integer id, Pageable pageable) {
        Page<HistoriquePlanification> page = historiqueRepository.findByPlanificationId(id, pageable);
        List<HistoriquePlanificationDto> content = page.getContent().stream()
            .map(h -> new HistoriquePlanificationDto(
                h.getId(),
                h.getPlanificationId(),
                h.getExecutionDebut(),
                h.getExecutionFin(),
                h.getStatut(),
                h.getGenerationCode(),
                h.getNombreFactures(),
                h.getMessage()
            ))
            .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void executerPlanificationScheduled(PlanificationFacturation plan) {
        LocalDateTime debut = LocalDateTime.now();
        log.info("Exécution planification: {} ({})", plan.getLibelle(), plan.getId());
        try {
            LocalDate[] periode = buildPeriode(plan);
            FactureEditionResponse response = executeAutoGeneration(plan, periode[0], periode[1]);
            // Bean séparé → proxy Spring → REQUIRES_NEW effectif
            statutService.sauvegarderSuccess(plan, debut, periode[1], response.generationCode(), calculerProchaine(plan));
            log.info("Planification {} exécutée avec succès, code: {}", plan.getId(), response.generationCode());
        } catch (Exception e) {
            log.error("Erreur planification {}: {}", plan.getId(), e.getMessage(), e);
            statutService.sauvegarderEchec(plan, debut, e.getMessage(), calculerProchaine(plan));
        }
    }

    // ── Génération automatique (deux passes : GROUP → TIERS_PAYANT) ───────────

    /**
     * Priorité aux factures groupées : les groupes éligibles sont traités en premier
     * (EditionByGroupTiersService), puis les tiers payants non couverts par un groupe
     * qualifiant sont traités individuellement (EditionByTiersPayantService).
     */
    private FactureEditionResponse executeAutoGeneration(
        PlanificationFacturation plan, LocalDate startDate, LocalDate endDate
    ) {
        boolean provisoire = plan.isFactureProvisoire();
        Periodicite periodicite = plan.getPeriodicite();

        // 1. Groupes éligibles selon le type de facturation (définitive ou provisoire)
        List<Integer> groupeIds = provisoire
            ? groupeTiersPayantRepository.findIdsForAutoGenerationProvisoire(periodicite)
            : groupeTiersPayantRepository.findIdsForAutoGenerationDefinitive(periodicite);

        // 2. Tiers payants individuels non couverts par un groupe éligible
        List<Integer> tpIds = groupeIds.isEmpty()
            ? (provisoire
                ? tiersPayantRepository.findAllIdsForAutoGenerationProvisoire(periodicite)
                : tiersPayantRepository.findAllIdsForAutoGenerationDefinitive(periodicite))
            : (provisoire
                ? tiersPayantRepository.findIdsNotInGroupsForAutoGenerationProvisoire(periodicite, new HashSet<>(groupeIds))
                : tiersPayantRepository.findIdsNotInGroupsForAutoGenerationDefinitive(periodicite, new HashSet<>(groupeIds)));

        if (groupeIds.isEmpty() && tpIds.isEmpty()) {
            log.warn("Planification {} : aucun organisme éligible (type={}, periodicite={})",
                plan.getId(), provisoire ? "PROVISOIRE" : "DEFINITIVE", periodicite);
            return new FactureEditionResponse(null, false);
        }

        Integer generationCode = null;

        // Passe 1 : facturation groupée (prioritaire)
        if (!groupeIds.isEmpty()) {
            log.info("Planification {} : {} groupe(s) en facturation groupée ({})",
                plan.getId(), groupeIds.size(), provisoire ? "provisoire" : "définitive");
            EditionSearchParams groupParams = new EditionSearchParams(
                ModeEditionSort.TIERS_NAME_ASC, ModeEditionEnum.GROUP,
                startDate, endDate,
                new HashSet<>(groupeIds), Collections.emptySet(), Collections.emptySet(),
                false, Collections.emptySet(),
                provisoire, OrigineGeneration.AUTO
            );
            generationCode = facturationServiceRegistry
                .getService(ModeEditionEnum.GROUP)
                .createFactureEdition(groupParams).generationCode();
        }

        // Passe 2 : facturation individuelle pour les tiers payants hors groupe
        if (!tpIds.isEmpty()) {
            log.info("Planification {} : {} tiers payant(s) individuels ({})",
                plan.getId(), tpIds.size(), provisoire ? "provisoire" : "définitive");
            EditionSearchParams tpParams = new EditionSearchParams(
                ModeEditionSort.TIERS_NAME_ASC, ModeEditionEnum.TIERS_PAYANT,
                startDate, endDate,
                Collections.emptySet(), new HashSet<>(tpIds), Collections.emptySet(),
                false, Collections.emptySet(),
                provisoire, OrigineGeneration.AUTO
            );
            FactureEditionResponse tpResponse = facturationServiceRegistry
                .getService(ModeEditionEnum.TIERS_PAYANT)
                .createFactureEdition(tpParams);
            if (generationCode == null) generationCode = tpResponse.generationCode();
        }

        return new FactureEditionResponse(generationCode, generationCode != null);
    }

    // ── Calcul de période ─────────────────────────────────────────────────────

    /**
     * Détermine la période à facturer à partir de {@code dernierePeriodeFin}.
     *
     * @return [startDate, endDate] de la prochaine période à facturer
     */
    private LocalDate[] buildPeriode(PlanificationFacturation plan) {
        if (plan.getPeriodicite() == null) {
            throw new IllegalStateException("Périodicité non définie sur la planification " + plan.getId());
        }
        if (plan.getDernierePeriodeFin() == null) {
            throw new IllegalStateException("dernierePeriodeFin non initialisée sur la planification " + plan.getId());
        }
        LocalDate debut = plan.getDernierePeriodeFin().plusDays(1);
        LocalDate fin   = calculerFinPeriode(debut, plan.getPeriodicite());
        return new LocalDate[]{debut, fin};
    }

    /**
     * Calcule la date de fin d'une période selon la périodicité.
     * <ul>
     *   <li>MENSUEL     : dernier jour du mois de début</li>
     *   <li>BIMENSUEL   : dernier jour du 2ème mois (début + 2 mois - 1 jour)</li>
     *   <li>QUINZAINE   : 1–15 → fin = 15 ; 16–fin → fin = dernier jour du mois</li>
     *   <li>HEBDOMADAIRE: dimanche de la semaine de début</li>
     * </ul>
     */
    private LocalDate calculerFinPeriode(LocalDate debut, Periodicite periodicite) {
        return switch (periodicite) {
            case MENSUEL      -> debut.with(TemporalAdjusters.lastDayOfMonth());
            case BIMENSUEL    -> debut.plusMonths(2).minusDays(1);
            case QUINZAINE    -> debut.getDayOfMonth() <= 15
                                    ? debut.withDayOfMonth(15)
                                    : debut.with(TemporalAdjusters.lastDayOfMonth());
            case HEBDOMADAIRE -> debut.with(DayOfWeek.SUNDAY);
        };
    }

    /**
     * Calcule la prochaine exécution : le lendemain de la fin de la prochaine période,
     * à l'heure configurée. Ainsi on attend que la période soit entièrement écoulée.
     */
    private LocalDateTime calculerProchaine(PlanificationFacturation plan) {
        if (plan.getPeriodicite() == null || plan.getDernierePeriodeFin() == null) {
            return null;
        }
        LocalTime heure = plan.getHeureDeclenchement() != null
            ? plan.getHeureDeclenchement()
            : LocalTime.of(8, 0);
        LocalDate debutProchaine = plan.getDernierePeriodeFin().plusDays(1);
        LocalDate finProchaine   = calculerFinPeriode(debutProchaine, plan.getPeriodicite());
        // Exécuter le 1er jour APRÈS la fin de la prochaine période
        return finProchaine.plusDays(1).atTime(heure);
    }

    // ── Historique (exécution manuelle, même transaction) ────────────────────

    private void enregistrerHistorique(
        PlanificationFacturation plan,
        LocalDateTime debut,
        LocalDateTime fin,
        ExecutionStatut statut,
        Integer generationCode,
        String message
    ) {
        HistoriquePlanification h = new HistoriquePlanification();
        h.setPlanificationId(plan.getId());
        h.setExecutionDebut(debut);
        h.setExecutionFin(fin);
        h.setStatut(statut);
        h.setGenerationCode(generationCode);
        h.setMessage(message);
        historiqueRepository.save(h);
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private void mapDtoToEntity(PlanificationDto dto, PlanificationFacturation entity) {
        entity.setLibelle(dto.libelle());
        entity.setPeriodicite(dto.periodicite());
        entity.setHeureDeclenchement(dto.heureDeclenchement());
        entity.setFactureProvisoire(dto.factureProvisoire());
        entity.setActif(dto.actif());
        if (dto.dernierePeriodeFin() != null) {
            entity.setDernierePeriodeFin(dto.dernierePeriodeFin());
        }
    }

    private PlanificationDto toDto(PlanificationFacturation entity) {
        long nombreOrganismes = 0L;
        if (entity.getPeriodicite() != null) {
            nombreOrganismes = entity.isFactureProvisoire()
                ? tiersPayantRepository.countForAutoGenerationProvisoire(entity.getPeriodicite())
                : tiersPayantRepository.countForAutoGenerationDefinitive(entity.getPeriodicite());
        }
        return new PlanificationDto(
            entity.getId(),
            entity.getLibelle(),
            entity.getPeriodicite(),
            entity.getHeureDeclenchement(),
            entity.isFactureProvisoire(),
            entity.isActif(),
            entity.getDernierePeriodeFin(),
            entity.getProchaineExecution(),
            entity.getDerniereExecution(),
            entity.getDernierStatut(),
            entity.getDernierMessage(),
            nombreOrganismes
        );
    }

}


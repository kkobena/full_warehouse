package com.kobe.warehouse.service.fne.service;

import com.kobe.warehouse.domain.HistoriqueCertificationFne;
import com.kobe.warehouse.domain.PlanificationCertificationFne;
import com.kobe.warehouse.repository.HistoriqueCertificationFneRepository;
import com.kobe.warehouse.repository.PlanificationCertificationFneRepository;
import com.kobe.warehouse.service.fne.model.CertificationFneResult;
import com.kobe.warehouse.service.scheduler.CertificationFneStatutService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlanificationCertificationFneServiceImpl implements PlanificationCertificationFneService {

    private static final Logger log = LoggerFactory.getLogger(PlanificationCertificationFneServiceImpl.class);

    private final PlanificationCertificationFneRepository planificationRepository;
    private final HistoriqueCertificationFneRepository historiqueRepository;
    private final FneService fneService;
    private final CertificationFneStatutService statutService;

    public PlanificationCertificationFneServiceImpl(
        PlanificationCertificationFneRepository planificationRepository,
        HistoriqueCertificationFneRepository historiqueRepository,
        FneService fneService,
        CertificationFneStatutService statutService
    ) {
        this.planificationRepository = planificationRepository;
        this.historiqueRepository = historiqueRepository;
        this.fneService = fneService;
        this.statutService = statutService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlanificationCertificationFne> findFirst() {
        return planificationRepository.findAll().stream().findFirst();
    }

    @Override
    public PlanificationCertificationFne toggleActif(Integer id) {
        PlanificationCertificationFne plan = planificationRepository.getReferenceById(id);
        plan.setActif(!plan.isActif());
        return planificationRepository.save(plan);
    }

    @Override
    public void executerMaintenant(Integer id) {
        PlanificationCertificationFne plan = planificationRepository.getReferenceById(id);
        LocalDateTime debut = LocalDateTime.now();
        log.info("Exécution manuelle certification FNE: {} (id={})", plan.getLibelle(), plan.getId());
        try {
            CertificationFneResult result = fneService.certifierFacturesPendantes(plan);
            statutService.sauvegarderStatut(plan, debut, result);
        } catch (Exception e) {
            log.error("Erreur exécution manuelle FNE planification {}: {}", plan.getId(), e.getMessage(), e);
            statutService.sauvegarderEchec(plan, debut, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HistoriqueCertificationFne> getHistorique(Integer planificationId, Pageable pageable) {
        return historiqueRepository.findByPlanificationIdOrderByExecutionDebutDesc(planificationId, pageable);
    }
}

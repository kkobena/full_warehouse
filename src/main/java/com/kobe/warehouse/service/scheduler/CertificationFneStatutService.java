package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.domain.HistoriqueCertificationFne;
import com.kobe.warehouse.domain.PlanificationCertificationFne;
import com.kobe.warehouse.domain.enumeration.ExecutionStatut;
import com.kobe.warehouse.repository.HistoriqueCertificationFneRepository;
import com.kobe.warehouse.repository.PlanificationCertificationFneRepository;
import com.kobe.warehouse.service.fne.model.CertificationFneResult;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bean séparé pour les sauvegardes de statut en REQUIRES_NEW.
 * Nécessaire pour contourner la limitation Spring AOP (self-invocation).
 */
@Service
public class CertificationFneStatutService {

    private final PlanificationCertificationFneRepository planificationRepository;
    private final HistoriqueCertificationFneRepository historiqueRepository;

    public CertificationFneStatutService(
        PlanificationCertificationFneRepository planificationRepository,
        HistoriqueCertificationFneRepository historiqueRepository
    ) {
        this.planificationRepository = planificationRepository;
        this.historiqueRepository = historiqueRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sauvegarderStatut(PlanificationCertificationFne plan, LocalDateTime debut, CertificationFneResult result) {
        ExecutionStatut statut;
        String message = null;

        if (result.isEmpty()) {
            statut = ExecutionStatut.SUCCESS;
            message = "Aucune facture en attente de certification";
        } else if (result.isFullSuccess()) {
            statut = ExecutionStatut.SUCCESS;
        } else if (result.nbCertifiees() > 0) {
            statut = ExecutionStatut.PARTIAL;
            message = result.nbEchecs() + " facture(s) en échec";
        } else {
            statut = ExecutionStatut.ECHEC;
            message = "Toutes les certifications ont échoué (" + result.nbEchecs() + ")";
        }

        enregistrerHistorique(plan, debut, statut, result.nbCertifiees(), result.nbEchecs(), message);
        mettreAJourPlan(plan, debut, statut, message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sauvegarderEchec(PlanificationCertificationFne plan, LocalDateTime debut, String message) {
        enregistrerHistorique(plan, debut, ExecutionStatut.ECHEC, 0, 0, message);
        mettreAJourPlan(plan, debut, ExecutionStatut.ECHEC, message);
    }

    private void enregistrerHistorique(
        PlanificationCertificationFne plan, LocalDateTime debut,
        ExecutionStatut statut, int nbOk, int nbKo, String message
    ) {
        HistoriqueCertificationFne h = new HistoriqueCertificationFne();
        h.setPlanificationId(plan.getId());
        h.setExecutionDebut(debut);
        h.setExecutionFin(LocalDateTime.now());
        h.setStatut(statut);
        h.setNbCertifiees(nbOk);
        h.setNbEchecs(nbKo);
        h.setMessage(message);
        historiqueRepository.save(h);
    }

    private void mettreAJourPlan(
        PlanificationCertificationFne plan, LocalDateTime debut,
        ExecutionStatut statut, String message
    ) {
        plan.setDerniereExecution(debut);
        plan.setDernierStatut(statut);
        plan.setDernierMessage(message);
        plan.setProchaineExecution(calculerProchaine(plan));
        planificationRepository.save(plan);
    }

    private LocalDateTime calculerProchaine(PlanificationCertificationFne plan) {
        LocalTime heure = plan.getHeureDeclenchement() != null
            ? plan.getHeureDeclenchement()
            : LocalTime.of(2, 0);
        return LocalDateTime.now().plusDays(1).toLocalDate().atTime(heure);
    }
}

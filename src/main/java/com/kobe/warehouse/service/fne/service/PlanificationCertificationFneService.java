package com.kobe.warehouse.service.fne.service;

import com.kobe.warehouse.domain.HistoriqueCertificationFne;
import com.kobe.warehouse.domain.PlanificationCertificationFne;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlanificationCertificationFneService {

    Optional<PlanificationCertificationFne> findFirst();

    PlanificationCertificationFne toggleActif(Integer id);

    void executerMaintenant(Integer id);

    Page<HistoriqueCertificationFne> getHistorique(Integer planificationId, Pageable pageable);
}

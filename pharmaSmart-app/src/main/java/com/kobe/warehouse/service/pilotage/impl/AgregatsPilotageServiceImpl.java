package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.repository.PilotageAgregatRepository;
import com.kobe.warehouse.service.pilotage.AgregatsPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.PeriodePilotage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AgregatsPilotageServiceImpl implements AgregatsPilotageService {

    private final PilotageAgregatRepository pilotageAgregatRepository;

    public AgregatsPilotageServiceImpl(PilotageAgregatRepository pilotageAgregatRepository) {
        this.pilotageAgregatRepository = pilotageAgregatRepository;
    }

    @Override
    public void recalculer(LocalDate du, LocalDate au) {
        PeriodePilotage.verifier(du, au);
        pilotageAgregatRepository.recalculer(du, au);
    }

    @Override
    public int actualiserJoursModifiesDepuis(LocalDateTime depuis) {
        List<LocalDate> jours = pilotageAgregatRepository.listerJoursModifiesDepuis(depuis);
        jours.forEach(jour -> pilotageAgregatRepository.recalculer(jour, jour));
        return jours.size();
    }

    @Override
    public void photographierStock(LocalDate jour) {
        pilotageAgregatRepository.photographierStock(jour);
    }
}

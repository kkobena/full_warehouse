package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.repository.PilotageVenteJourRepository;
import com.kobe.warehouse.service.pilotage.CalendrierPilotageService;
import com.kobe.warehouse.service.pilotage.calcul.PeriodePilotage;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CalendrierPilotageServiceImpl implements CalendrierPilotageService {

    private final PilotageVenteJourRepository pilotageVenteJourRepository;

    public CalendrierPilotageServiceImpl(PilotageVenteJourRepository pilotageVenteJourRepository) {
        this.pilotageVenteJourRepository = pilotageVenteJourRepository;
    }

    @Override
    public long compterJoursOuvres(LocalDate du, LocalDate au) {
        PeriodePilotage.verifier(du, au);
        return pilotageVenteJourRepository.compterJoursOuvres(du, au);
    }
}

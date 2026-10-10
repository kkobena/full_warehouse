package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.repository.PilotageVenteRepository;
import com.kobe.warehouse.service.dto.pilotage.ChiffreAffairesReferenceDTO;
import com.kobe.warehouse.service.pilotage.ChiffreAffairesReferenceService;
import com.kobe.warehouse.service.pilotage.calcul.PeriodePilotage;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChiffreAffairesReferenceServiceImpl implements ChiffreAffairesReferenceService {

    private final PilotageVenteRepository pilotageVenteRepository;

    public ChiffreAffairesReferenceServiceImpl(PilotageVenteRepository pilotageVenteRepository) {
        this.pilotageVenteRepository = pilotageVenteRepository;
    }

    @Override
    public ChiffreAffairesReferenceDTO calculerChiffreAffaires(LocalDate du, LocalDate au) {
        PeriodePilotage.verifier(du, au);
        return pilotageVenteRepository.calculerChiffreAffairesOfficine(du, au);
    }
}

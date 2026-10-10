package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.ChiffreAffairesReferenceDTO;
import java.time.LocalDate;

/** CA de l'officine selon la définition de référence du pilotage. */
public interface ChiffreAffairesReferenceService {
    ChiffreAffairesReferenceDTO calculerChiffreAffaires(LocalDate du, LocalDate au);
}

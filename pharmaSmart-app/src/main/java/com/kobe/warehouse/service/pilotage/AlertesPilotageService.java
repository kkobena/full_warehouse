package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import java.time.LocalDate;
import java.util.List;

/** Alertes du pilotage (§5.3 du plan), calculées à la demande sur les agrégats ; les plus graves d'abord. */
public interface AlertesPilotageService {
    List<AlertePilotageDTO> listerAlertes(LocalDate aujourdhui);
}

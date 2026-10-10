package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import com.kobe.warehouse.service.pilotage.AlertesPilotageService;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.alertes.RegleAlerte;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlertesPilotageServiceImpl implements AlertesPilotageService {

    private static final Logger LOG = LoggerFactory.getLogger(AlertesPilotageServiceImpl.class);

    private final List<RegleAlerte> regles;
    private final DictionnaireIndicateursService dictionnaireIndicateursService;

    public AlertesPilotageServiceImpl(List<RegleAlerte> regles, DictionnaireIndicateursService dictionnaireIndicateursService) {
        this.regles = regles;
        this.dictionnaireIndicateursService = dictionnaireIndicateursService;
    }

    /** Une règle en échec n'empêche pas les autres de s'afficher : elle est journalisée et ignorée. */
    @Override
    public List<AlertePilotageDTO> listerAlertes(LocalDate aujourdhui) {
        Set<DroitPilotage> droits = dictionnaireIndicateursService.lireDroitsAccordes();
        List<AlertePilotageDTO> alertes = new ArrayList<>();
        for (RegleAlerte regle : regles) {
            if (!droits.contains(regle.lireDroit())) {
                continue;
            }
            try {
                alertes.addAll(regle.evaluer(aujourdhui));
            } catch (RuntimeException e) {
                LOG.warn("Alerte du pilotage non évaluée : {}", regle.getClass().getSimpleName(), e);
            }
        }
        alertes.sort(Comparator.comparing(AlertePilotageDTO::gravite));
        return alertes;
    }
}

package com.kobe.warehouse.service.pilotage.impl;

import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.service.dto.pilotage.AxeAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.pilotage.DictionnaireIndicateursService;
import com.kobe.warehouse.service.pilotage.calcul.Ventilation;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DictionnaireIndicateursServiceImpl implements DictionnaireIndicateursService {

    private final NavAccessService navAccessService;

    public DictionnaireIndicateursServiceImpl(NavAccessService navAccessService) {
        this.navAccessService = navAccessService;
    }

    @Override
    public List<IndicateurPilotageDTO> listerIndicateursAutorises() {
        Set<DroitPilotage> droitsAccordes = lireDroitsAccordes();
        return Arrays.stream(IndicateurPilotage.values())
            .filter(indicateur -> droitsAccordes.contains(indicateur.getDroit()))
            .map(IndicateurPilotageDTO::fromIndicateur)
            .toList();
    }

    @Override
    public List<IndicateurPilotage> filtrerAutorises(List<IndicateurPilotage> demandes) {
        Set<DroitPilotage> droitsAccordes = lireDroitsAccordes();
        return demandes.stream().filter(indicateur -> droitsAccordes.contains(indicateur.getDroit())).toList();
    }

    @Override
    public List<AxeAnalyseDTO> listerAxesAutorises() {
        Set<DroitPilotage> droitsAccordes = lireDroitsAccordes();
        return Arrays.stream(AxeAnalyse.values()).filter(axe -> axe.estPropose() && droitsAccordes.contains(axe.getDroit())).map(AxeAnalyseDTO::fromAxe).toList();
    }

    @Override
    public void verifierAxesAutorises(Collection<AxeAnalyse> axes) {
        Set<DroitPilotage> droitsAccordes = lireDroitsAccordes();
        axes
            .stream()
            .filter(axe -> !droitsAccordes.contains(axe.getDroit()))
            .findFirst()
            .ifPresent(axe -> {
                throw new ForbiddenOperationException("Ventilation non autorisée : " + axe.getLibelle(), "pilotageAxeInterdit");
            });
    }

    /** Un appel par droit plutôt que par indicateur : plusieurs indicateurs partagent le même onglet. */
    @Override
    public Set<DroitPilotage> lireDroitsAccordes() {
        Set<DroitPilotage> droits = EnumSet.noneOf(DroitPilotage.class);
        for (DroitPilotage droit : DroitPilotage.values()) {
            if (navAccessService.isAllowed(List.of(droit.getCode()), NavAction.DISPLAY)) {
                droits.add(droit);
            }
        }
        return droits;
    }
}

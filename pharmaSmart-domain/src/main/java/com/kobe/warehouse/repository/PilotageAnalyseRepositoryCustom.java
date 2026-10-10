package com.kobe.warehouse.repository;

import com.kobe.warehouse.service.dto.pilotage.MesuresVentileesDTO;
import com.kobe.warehouse.service.dto.pilotage.RequeteVentilationDTO;
import java.util.List;

/** Ventilation des agrégats du pilotage par des axes choisis à la demande (onglet Analyser, explication des écarts). */
public interface PilotageAnalyseRepositoryCustom {
    List<MesuresVentileesDTO> ventiler(RequeteVentilationDTO requete);
}

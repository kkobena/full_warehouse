package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.GraviteAlerte;

/**
 * Une alerte active du pilotage.
 *
 * @param onglet onglet de la page où creuser (identifiant de l'URL : {@code analyser}, {@code objectifs}…)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record AlertePilotageDTO(String code, GraviteAlerte gravite, String titre, String detail, String onglet) {}

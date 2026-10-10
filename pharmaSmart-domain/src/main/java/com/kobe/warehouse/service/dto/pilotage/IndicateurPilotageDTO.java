package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;

/** Entrée du dictionnaire des indicateurs, telle que les écrans l'affichent (libellé, infobulle, format, couleur). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record IndicateurPilotageDTO(
    String code,
    String libelle,
    String definition,
    UniteIndicateur unite,
    SensFavorable sensFavorable,
    String droit
) {
    public static IndicateurPilotageDTO fromIndicateur(IndicateurPilotage indicateur) {
        return new IndicateurPilotageDTO(
            indicateur.name(),
            indicateur.getLibelle(),
            indicateur.getDefinition(),
            indicateur.getUnite(),
            indicateur.getSensFavorable(),
            indicateur.getDroit().getCode()
        );
    }
}

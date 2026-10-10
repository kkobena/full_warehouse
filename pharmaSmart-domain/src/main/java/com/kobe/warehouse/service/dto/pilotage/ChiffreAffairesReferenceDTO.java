package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/** CA de l'officine selon la définition de référence du pilotage (voir {@code IndicateurPilotage}). */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ChiffreAffairesReferenceDTO(long nbVentes, long caTtc, long caHt, long remises, long caNet) {
    /** Constructeur de la projection JPQL : le CA net se déduit. */
    public ChiffreAffairesReferenceDTO(long nbVentes, long caTtc, long caHt, long remises) {
        this(nbVentes, caTtc, caHt, remises, caTtc - remises);
    }
}

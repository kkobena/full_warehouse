package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * Un organisme : facturé et réglé sur les factures de la période ; encours, DSO, retard et délai retenu à date.
 *
 * @param origineDelai OBSERVE (moyenne de ses règlements), GROUPE (délai contractuel de son groupe) ou DEFAUT (délai de l'officine)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record OrganismeTresorerieDTO(
    String cle,
    String libelle,
    long facture,
    long regle,
    Double tauxRecouvrement,
    long encours,
    Double partEncours,
    Integer dso,
    long enRetard,
    int delaiRetenu,
    String origineDelai
) {}

package com.kobe.warehouse.service.dto.pilotage.rapport;

/** Libellé et montant signé déjà formaté (« + 850 k »), avec sa classe CSS de hausse ou de baisse. */
public record MontantRapportDTO(String libelle, String montant, String appreciation) {}

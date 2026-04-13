package com.kobe.warehouse.service.dto.dashboard;

import java.util.List;

/**
 * DTO pour les encaissements de la session courante du caissier connecté.
 * Les lignes sont générées dynamiquement selon les modes de paiement utilisés.
 */
public record SessionEncaissementsDTO(
    List<EncaissementParModeDTO> lignes,
    Long carnet,
    Long differe,
    Long totalEncaisse,
    Long totalARecouvrer,
    Integer nombreTransactions
) {}

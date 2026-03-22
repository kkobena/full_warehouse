package com.kobe.warehouse.service.dto;

import java.util.List;

/**
 * Données agrégées pour le tableau de bord des commandes.
 */
public record CommandeDashboardDTO(
    long totalRequested,
    long totalReceived,
    long totalPharmamlPending,
    List<CommandeResumeeDTO> commandesRequested,
    List<CommandeResumeeDTO> commandesReceived,
    List<PharmaMlEnvoiResumeeDTO> envoisPending
) {}

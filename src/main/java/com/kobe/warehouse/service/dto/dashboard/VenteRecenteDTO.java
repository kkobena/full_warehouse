package com.kobe.warehouse.service.dto.dashboard;

import java.time.LocalDateTime;

/**
 * DTO pour une vente récente filtrée sur la session du caissier connecté.
 */
public record VenteRecenteDTO(
    Long saleId,
    String numeroRecu,
    Long montant,
    LocalDateTime dateVente,
    String modePaiement,           // dérivé de nature_vente
    String typeVente,              // COMPTANT | ASSURANCE | CARNET | DIFFERE
    String clientNom               // null si vente anonyme
) {}

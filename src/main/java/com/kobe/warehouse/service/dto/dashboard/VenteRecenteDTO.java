package com.kobe.warehouse.service.dto.dashboard;

import java.time.LocalDateTime;

public record VenteRecenteDTO(
    Long saleId,
    String numeroRecu,
    Long montant,
    LocalDateTime dateVente,
    String modePaiement,
    String vendeur,
    Integer nombreLignes,
    String statut
) {}

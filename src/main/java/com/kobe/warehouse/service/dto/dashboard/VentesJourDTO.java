package com.kobe.warehouse.service.dto.dashboard;

public record VentesJourDTO(
    Long montantTotal,
    Integer nombreVentes,
    Long montantEspeces,
    Long montantCB,
    Long montantCheque,
    Long montantMobileMoney,
    Long montantVirement,
    Long montantAssurance,
    Long ticketMoyen,
    Long objectifJour,
    Double tauxAtteinte
) {}

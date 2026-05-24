package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;

public record MesPerformancesDTO(
    double caJour,
    double caObjectif,
    double tauxAtteinte,
    int rang,
    int totalVendeurs,
    String badge,
    double progression
) implements Serializable {

    public MesPerformancesDTO {
        if (badge == null) {
            badge = "BRONZE";
        }
    }
}

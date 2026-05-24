package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import java.util.List;

public record RetourAvoirStatsDTO(
    int nbRetoursMois,
    int montantTotalRetoursMois,
    List<MotifStatDTO> statsParMotif,
    List<ProduitAlerteDTO> produitsEnAlerte,
    List<ClientAlerteDTO> clientsEnAlerte,
    int nbAvoirsOuverts,
    int montantTotalAvoirsOuverts,
    int nbAvoirsProchesExpiration
) {

    public record MotifStatDTO(MotifRetourClient motif, long count) {}

    public record ProduitAlerteDTO(Integer produitId, String libelle, String codeCip, long nbRetours) {}

    public record ClientAlerteDTO(Integer clientId, String nom, long nbRetours) {}
}

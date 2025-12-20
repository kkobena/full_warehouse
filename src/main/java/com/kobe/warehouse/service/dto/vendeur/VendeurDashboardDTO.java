package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;
import java.util.List;

public record VendeurDashboardDTO(
    MesPerformancesDTO mesPerformances,
    MesClientsDTO mesClients,
    VentesParTypeDTO ventesParType,
    CommissionDTO commission,
    List<TopProduitVendeurDTO> topProduits,
    List<VenteRecenteVendeurDTO> ventesRecentes,
    List<OpportuniteVenteDTO> opportunites,
    List<ObjectifMensuelDTO> objectifsMensuels,
    List<ClientFideleDTO> clientsFideles
) implements Serializable {}

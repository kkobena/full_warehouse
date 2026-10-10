package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.List;

/**
 * Sous-section Stock : photographie du mois de fin de période (au prix d'achat TTC du dernier mouvement, comme la valorisation),
 * comparée au même mois N-1.
 *
 * @param seuilDormant jours sans vente au-delà desquels un produit en stock est dormant
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record StockPilotageDTO(
    LocalDate mois,
    CelluleAnalyseDTO valeur,
    Double rotation,
    Double couvertureJours,
    long nbDormants,
    long valeurDormante,
    int seuilDormant,
    List<PointStockDTO> courbe,
    List<LigneStockDTO> familles,
    List<ProduitDormantDTO> dormants
) {}

package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Sous-section Ruptures & péremptions. Deux ruptures distinctes, à ne pas additionner : fournisseurs (table {@code rupture},
 * produits commandés non livrés) et comptoir (table {@code avoir_client}, quantités demandées non servies : les ventes manquées).
 *
 * @param tauxRupture lignes en rupture / lignes commandées (%)
 * @param tauxVentesManquees quantités en avoir / quantités demandées (%)
 * @param valeurPerimee lots arrivés à péremption sur la période et encore en stock, au prix d'achat
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RupturesPilotageDTO(
    ComparaisonPeriodesDTO comparaison,
    CelluleAnalyseDTO tauxRupture,
    CelluleAnalyseDTO ventesManquees,
    CelluleAnalyseDTO tauxVentesManquees,
    CelluleAnalyseDTO valeurPerimee,
    long valeurAPerimerTroisMois,
    List<ComptageDTO> rupturesParFournisseur,
    List<ComptageDTO> rupturesParProduit,
    List<ComptageDTO> ventesManqueesParProduit,
    List<PeremptionMoisDTO> peremptionsAVenir
) {}

package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Sous-section Remises de « Rentabilité & remises ».
 *
 * @param partVentesRemisees ventes remisées / ventes (%)
 * @param poidsRemisesMarge remises / (marge brute + remises) (%) : ce que les remises ont coûté de marge
 * @param vendeurs {@code null} sans le droit « Clients & équipe »
 * @param multipleAlerte au-delà de ce multiple du taux de l'équipe, un vendeur est signalé
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RemisesRentabiliteDTO(
    ComparaisonPeriodesDTO comparaison,
    CelluleAnalyseDTO remises,
    CelluleAnalyseDTO tauxRemise,
    CelluleAnalyseDTO partVentesRemisees,
    CelluleAnalyseDTO remiseMoyenne,
    CelluleAnalyseDTO poidsRemisesMarge,
    List<LigneRemiseDTO> octrois,
    List<LigneRemiseDTO> tranches,
    List<LigneRemiseDTO> vendeurs,
    double multipleAlerte,
    List<VenteRemiseeDTO> plusFortesRemises
) {}

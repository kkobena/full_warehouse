package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Sous-section Clients. Un client actif a au moins une vente sur la période ; nouveau : sa première vente tombe dans la
 * période ; perdu : actif sur la référence, plus sur la période ; revenu : actif sur la période, absent de la référence, mais
 * client avant elle.
 *
 * @param partCaIdentifie part du CA réalisée avec un client identifié (%)
 * @param enBaisse clients dont le CA recule le plus face à la référence
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record ClientsPilotageDTO(
    ComparaisonPeriodesDTO comparaison,
    CelluleAnalyseDTO actifs,
    CelluleAnalyseDTO nouveaux,
    Long revenus,
    Long perdus,
    CelluleAnalyseDTO partCaIdentifie,
    CelluleAnalyseDTO caMoyenParClient,
    List<ContributionDTO> enBaisse
) {}

package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Sous-section Démarque : ajustements de sortie clôturés, valorisés au prix d'achat courant (comme le rapport de démarque).
 *
 * @param partDuCa valeur perdue / CA TTC (%)
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record DemarqueRentabiliteDTO(
    ComparaisonPeriodesDTO comparaison,
    CelluleAnalyseDTO valeur,
    CelluleAnalyseDTO partDuCa,
    List<LigneDemarqueDTO> parMotif,
    List<MontantDemarqueDTO> produits
) {}

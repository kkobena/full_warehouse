package com.kobe.warehouse.service.dto;

import java.util.List;

/**
 * DTO pour commander une sélection de lignes de suggestion avec des quantités personnalisées.
 *
 * @param suggestionId ID de la suggestion parente
 * @param lignes       Liste des lignes à commander avec leur quantité souhaitée
 */
public record CommanderSelectionDTO(
    Integer suggestionId,
    List<LigneSelection> lignes
) {

    /**
     * @param suggestionLineId ID de la SuggestionLine
     * @param quantite         Quantité à commander (peut différer de la valeur calculée)
     */
    public record LigneSelection(Integer suggestionLineId, int quantite) {}
}

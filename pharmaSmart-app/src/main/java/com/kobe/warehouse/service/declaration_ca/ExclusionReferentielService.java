package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.service.declaration_ca.dto.ExclusionItemDTO;
import java.util.List;
import java.util.Set;

/**
 * Gère les référentiels dont les ventes peuvent être écartées du chiffre d'affaires à déclarer :
 * rayons et tiers-payants.
 *
 * <p>Un seul service pour les deux : ils portent le même geste — cocher des lignes, appliquer.
 * Les séparer aurait dupliqué la mise à jour en masse et la mesure d'impact.
 */
public interface ExclusionReferentielService {
    /**
     * @param exclus {@code null} pour tout lister, sinon filtre sur l'état d'exclusion
     */
    List<ExclusionItemDTO> listerRayons(Boolean exclus);

    List<ExclusionItemDTO> listerTiersPayants(Boolean exclus);

    /**
     * Positionne l'état d'exclusion de plusieurs rayons.
     *
     * @return le nombre de rayons dont l'état a réellement changé
     */
    int majExclusionRayons(Set<Integer> ids, boolean exclure);

    int majExclusionTiersPayants(Set<Integer> ids, boolean exclure);
}

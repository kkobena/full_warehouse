package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.service.declaration_ca.dto.ExclusionItemDTO;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Gère les référentiels dont les ventes peuvent être écartées du chiffre d'affaires à déclarer :
 * rayons et tiers-payants.
 *
 * <p>Un seul service pour les deux : ils portent le même geste — cocher des lignes, appliquer.
 * Les séparer aurait dupliqué la mise à jour en masse et la mesure d'impact.
 */
public interface ExclusionReferentielService {
    /**
     * Une page de rayons de l'emplacement de l'utilisateur connecté.
     *
     * <p>Filtre et recherche sont des prédicats SQL, pas un tri du résultat : écrémer une page
     * déjà découpée renverrait des pages de tailles inégales et un total faux.
     *
     * @param exclus   {@code null} pour tout lister, sinon filtre sur l'état d'exclusion
     * @param search   recherche sur le libellé ou le code ; {@code null} ou vide pour l'ignorer
     */
    Page<ExclusionItemDTO> listerRayons(Boolean exclus, String search, Pageable pageable);

    /**
     * Une page de tiers-payants candidats à l'exclusion.
     *
     * <p>Restreinte aux tiers-payants actifs et hors {@code CARNET} / {@code DEPOT} : ces deux
     * catégories ne portent pas de chiffre d'affaires tiers-payant à écarter.
     */
    Page<ExclusionItemDTO> listerTiersPayants(Boolean exclus, String search, Pageable pageable);

    /**
     * Positionne l'état d'exclusion de plusieurs rayons.
     *
     * @return le nombre de rayons dont l'état a réellement changé
     */
    int majExclusionRayons(Set<Integer> ids, boolean exclure);

    int majExclusionTiersPayants(Set<Integer> ids, boolean exclure);
}

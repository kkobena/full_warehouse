package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Écart de CA expliqué : CA = ventes × articles par vente × prix moyen d'un article, décomposé en trois effets dont la somme
 * vaut l'écart ; puis les plus fortes hausses et baisses, toutes ventilations confondues. Effets {@code null} sans référence.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record EcartsPilotageDTO(
    ComparaisonPeriodesDTO comparaison,
    long ca,
    long caReference,
    Long effetFrequentation,
    Long effetArticles,
    Long effetPrix,
    List<ContributionDTO> hausses,
    List<ContributionDTO> baisses
) {}

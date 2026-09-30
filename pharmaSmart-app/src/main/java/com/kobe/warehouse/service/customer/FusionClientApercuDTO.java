package com.kobe.warehouse.service.customer;

import java.util.List;
import java.util.Map;

/**
 * Ce que la fusion ferait, sans rien modifier.
 *
 * @param rejets         fiches écartées, avec la raison
 * @param counts         éléments rattachés à la fiche conservée, par nature
 * @param avertissements points à vérifier après fusion (dossiers santé réunis, carnets additionnés…)
 */
public record FusionClientApercuDTO(
    Integer targetId,
    List<Integer> sourceIds,
    Map<Integer, String> rejets,
    Map<String, Integer> counts,
    List<String> avertissements
) {}

package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kobe.warehouse.domain.enumeration.AxeAnalyse;
import com.kobe.warehouse.domain.enumeration.TriAnalyse;
import java.util.List;

/**
 * Ce que l'onglet Analyser ajoute à la requête de la page : axes, nombre d'éléments, tri, chemin de descente.
 *
 * @param top nombre d'éléments montrés, le reste regroupé en « autres » ; 0 : tous
 * @param filtre chemin de descente, un filtre par élément choisi, au format {@code AXE:cle}
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record RequeteAnalyseDTO(AxeAnalyse axe, AxeAnalyse axe2, Integer top, TriAnalyse tri, List<String> filtre) {
    public RequeteAnalyseDTO {
        axe = axe == null ? AxeAnalyse.FAMILLE : axe;
        top = top == null ? 20 : top;
        tri = tri == null ? TriAnalyse.VALEUR : tri;
        filtre = filtre == null ? List.of() : List.copyOf(filtre);
    }

    public List<FiltreAnalyseDTO> lireFiltres() {
        return filtre
            .stream()
            .map(f -> {
                int separateur = f.indexOf(':');
                return new FiltreAnalyseDTO(AxeAnalyse.valueOf(f.substring(0, separateur)), List.of(f.substring(separateur + 1)));
            })
            .toList();
    }
}

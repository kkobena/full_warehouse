package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.ElementAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import java.util.List;

/**
 * MiseEnFormeAnalyse en forme des éléments : une cellule par indicateur ; part du total et contribution à l'écart sur le premier, s'il est
 * additif.
 */
public record MiseEnFormeAnalyse(List<IndicateurPilotage> indicateurs, List<CelluleAnalyseDTO> total, boolean avecReference) {
    public static MiseEnFormeAnalyse preparer(List<IndicateurPilotage> indicateurs, MesuresComparees total, boolean avecReference) {
        return new MiseEnFormeAnalyse(indicateurs, calculer(indicateurs, total, avecReference), avecReference);
    }

    public IndicateurPilotage principal() {
        return indicateurs.getFirst();
    }

    public ElementAnalyseDTO element(MembreAnalyseDTO membre, MesuresComparees mesures) {
        List<CelluleAnalyseDTO> cellules = calculer(indicateurs, mesures, avecReference);
        boolean additif = principal().estAdditif();
        return new ElementAnalyseDTO(
            membre.cle(),
            membre.libelle(),
            cellules,
            additif ? Variations.part(cellules.getFirst().valeur(), total.getFirst().valeur()) : null,
            additif ? Variations.part(cellules.getFirst().ecart(), total.getFirst().ecart()) : null
        );
    }

    private static List<CelluleAnalyseDTO> calculer(List<IndicateurPilotage> indicateurs, MesuresComparees mesures, boolean avecReference) {
        return indicateurs.stream().map(indicateur -> Variations.cellule(indicateur, mesures, avecReference)).toList();
    }
}

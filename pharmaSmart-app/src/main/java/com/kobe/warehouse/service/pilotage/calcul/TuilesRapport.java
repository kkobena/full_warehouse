package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.SerieIndicateurDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TuileRapportDTO;
import java.util.List;

/** Tuiles du rapport : pendants d'{@code app-kpi-item} (indicateur du dictionnaire) et d'{@code app-tuiles-cellules}. */
public final class TuilesRapport {

    private TuilesRapport() {}

    public static TuileRapportDTO lireTuile(SerieIndicateurDTO serie) {
        IndicateurPilotageDTO indicateur = serie.indicateur();
        return new TuileRapportDTO(
            indicateur.libelle(),
            FormatRapport.formaterValeur(serie.valeur(), indicateur.unite()),
            FormatRapport.formaterVariation(serie.ecart(), serie.ecartPct(), indicateur.unite(), indicateur.sensFavorable()),
            List.of()
        );
    }

    /** Mesure hors dictionnaire ; sans référence, pas de variation. */
    public static TuileRapportDTO lireTuile(String libelle, CelluleAnalyseDTO cellule, UniteIndicateur unite, SensFavorable sens) {
        return new TuileRapportDTO(
            libelle,
            FormatRapport.formaterValeur(cellule.valeur(), unite),
            cellule.valeurReference() == null ? null : FormatRapport.formaterVariation(cellule.ecart(), cellule.ecartPct(), unite, sens),
            List.of()
        );
    }
}

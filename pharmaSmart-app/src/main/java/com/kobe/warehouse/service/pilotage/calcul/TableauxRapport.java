package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.IndicateurPilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneCroiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.LigneRapportDTO.CelluleRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.TableauRapportDTO;
import java.util.ArrayList;
import java.util.List;

/** Tableaux du rapport PDF construits à partir des DTO de l'écran (croisés, cellules), mêmes valeurs complètes que l'écran. */
public final class TableauxRapport {

    private TableauxRapport() {}

    /** Pendant d'{@code app-croise-analyse} : valeur complète et variation quand une référence existe. */
    public static TableauRapportDTO lireCroise(String entete, CroiseAnalyseDTO croise, IndicateurPilotageDTO indicateur) {
        return lireCroise(entete, croise, indicateur, true);
    }

    /** @param avecReference faux : les valeurs seules, sans variation (CA par vendeur et par heure) */
    public static TableauRapportDTO lireCroise(String entete, CroiseAnalyseDTO croise, IndicateurPilotageDTO indicateur, boolean avecReference) {
        List<String> colonnes = croise.colonnes().stream().map(MembreAnalyseDTO::libelle).toList();
        List<LigneRapportDTO> lignes = new ArrayList<>(croise.lignes().size());
        for (LigneCroiseeDTO ligne : croise.lignes()) {
            List<CelluleRapportDTO> cellules = new ArrayList<>(ligne.cellules().size());
            for (CelluleAnalyseDTO cellule : ligne.cellules()) {
                cellules.add(avecReference ? lireCellule(cellule, indicateur) : lireValeur(cellule == null ? null : cellule.valeur(), indicateur.unite()));
            }
            lignes.add(new LigneRapportDTO(ligne.libelle(), cellules));
        }
        return new TableauRapportDTO(entete, colonnes, lignes);
    }

    public static List<CelluleRapportDTO> lireCellules(List<CelluleAnalyseDTO> cellules, IndicateurPilotageDTO indicateur) {
        List<CelluleRapportDTO> resultat = new ArrayList<>(cellules.size());
        for (CelluleAnalyseDTO cellule : cellules) {
            resultat.add(lireCellule(cellule, indicateur));
        }
        return resultat;
    }

    /** Une cellule par indicateur, chacune à l'unité et au sens de son indicateur (le détail de l'onglet Analyser). */
    public static List<CelluleRapportDTO> lireCellules(List<CelluleAnalyseDTO> cellules, List<IndicateurPilotageDTO> indicateurs) {
        List<CelluleRapportDTO> resultat = new ArrayList<>(cellules.size());
        for (int rang = 0; rang < cellules.size(); rang++) {
            resultat.add(lireCellule(cellules.get(rang), indicateurs.get(rang)));
        }
        return resultat;
    }

    public static CelluleRapportDTO lireCellule(CelluleAnalyseDTO cellule, IndicateurPilotageDTO indicateur) {
        if (cellule == null) {
            return new CelluleRapportDTO("—", null);
        }
        return new CelluleRapportDTO(
            FormatRapport.formaterValeurComplete(cellule.valeur(), indicateur.unite()),
            cellule.valeurReference() == null
                ? null
                : FormatRapport.formaterVariation(cellule.ecart(), cellule.ecartPct(), indicateur.unite(), indicateur.sensFavorable())
        );
    }

    /** Cellule d'une mesure hors dictionnaire : valeur complète et, s'il y a une référence, variation. */
    public static CelluleRapportDTO lireCellule(CelluleAnalyseDTO cellule, UniteIndicateur unite, SensFavorable sens) {
        if (cellule == null) {
            return new CelluleRapportDTO("—", null);
        }
        return new CelluleRapportDTO(
            FormatRapport.formaterValeurComplete(cellule.valeur(), unite),
            cellule.valeurReference() == null ? null : FormatRapport.formaterVariation(cellule.ecart(), cellule.ecartPct(), unite, sens)
        );
    }

    /** Cellule d'une valeur seule, complète (montant non abrégé), sans variation. */
    public static CelluleRapportDTO lireValeur(Number valeur, UniteIndicateur unite) {
        return new CelluleRapportDTO(FormatRapport.formaterValeurComplete(valeur == null ? null : valeur.doubleValue(), unite), null);
    }

    /** Les {@code maximum} premières lignes ; la mention dit combien n'ont pas été imprimées. */
    public static TableauRapportDTO limiter(TableauRapportDTO tableau, int maximum) {
        int total = tableau.lignes().size();
        if (total <= maximum) {
            return tableau;
        }
        String mention = "… et " + (total - maximum) + " autres lignes, non imprimées.";
        return new TableauRapportDTO(tableau.entete(), tableau.colonnes(), tableau.lignes().subList(0, maximum), mention);
    }
}

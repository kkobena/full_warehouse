package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.CroiseAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneCroiseeDTO;
import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.CarteChaleurRapportDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.CarteChaleurRapportDTO.CaseChaleurDTO;
import com.kobe.warehouse.service.dto.pilotage.rapport.CarteChaleurRapportDTO.LigneChaleurDTO;
import java.util.ArrayList;
import java.util.List;

/** Pendant serveur d'{@code app-carte-chaleur} : l'accent mêlé au blanc, jusqu'à 45 % pour la case la plus forte. */
public final class CartesChaleur {

    /** Au-delà, le texte perdrait du contraste sur l'accent. */
    private static final int INTENSITE_MAX = 45;
    private static final int[] ACCENT = { 0x39, 0x89, 0xc6 };
    private static final int BLANC = 0xff;

    private CartesChaleur() {}

    public static CarteChaleurRapportDTO lireCarte(String entete, CroiseAnalyseDTO croise, UniteIndicateur unite) {
        double maximum = lireMaximum(croise);
        List<LigneChaleurDTO> lignes = new ArrayList<>(croise.lignes().size());
        for (LigneCroiseeDTO ligne : croise.lignes()) {
            List<CaseChaleurDTO> cases = new ArrayList<>(ligne.cellules().size());
            for (CelluleAnalyseDTO cellule : ligne.cellules()) {
                Double valeur = cellule == null ? null : cellule.valeur();
                boolean vide = valeur == null || valeur == 0;
                cases.add(new CaseChaleurDTO(vide ? "" : FormatRapport.formaterValeur(valeur, unite), melanger(vide ? 0 : valeur / maximum)));
            }
            lignes.add(new LigneChaleurDTO(ligne.libelle(), cases));
        }
        return new CarteChaleurRapportDTO(entete, croise.colonnes().stream().map(MembreAnalyseDTO::libelle).toList(), lignes);
    }

    private static double lireMaximum(CroiseAnalyseDTO croise) {
        double maximum = 0;
        for (LigneCroiseeDTO ligne : croise.lignes()) {
            for (CelluleAnalyseDTO cellule : ligne.cellules()) {
                if (cellule != null && cellule.valeur() != null) {
                    maximum = Math.max(maximum, cellule.valeur());
                }
            }
        }
        return maximum;
    }

    /** Comme {@code color-mix(in srgb, accent p %, blanc)}. */
    private static String melanger(double part) {
        double intensite = Math.round(part * INTENSITE_MAX) / 100.0;
        StringBuilder couleur = new StringBuilder("#");
        for (int composante : ACCENT) {
            couleur.append(String.format("%02x", (int) Math.round(composante * intensite + BLANC * (1 - intensite))));
        }
        return couleur.toString();
    }
}

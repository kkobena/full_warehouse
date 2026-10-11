package com.kobe.warehouse.service.pilotage;

import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.rapport.FormatGraphe;
import com.kobe.warehouse.service.dto.pilotage.rapport.SerieGraphiqueDTO;
import java.util.List;

/** Graphes du rapport PDF du pilotage, dessinés côté serveur en PNG et rendus en data URI pour un {@code <img src>}. */
public interface GraphiquePilotageService {
    /** Courbes sur des libellés communs (une tranche par libellé) ; une valeur absente laisse un trou. */
    String dessinerCourbes(List<String> libelles, List<SerieGraphiqueDTO> series, UniteIndicateur unite, FormatGraphe format);

    /** Barres verticales, groupées par libellé quand il y a plusieurs séries. */
    String dessinerBarres(List<String> libelles, List<SerieGraphiqueDTO> series, UniteIndicateur unite, FormatGraphe format);

    /** Barres empilées : le total de chaque libellé, découpé par série (encaissements par mode). */
    String dessinerBarresEmpilees(List<String> libelles, List<SerieGraphiqueDTO> series, UniteIndicateur unite, FormatGraphe format);

    /** Barres horizontales classées, pleine largeur, d'autant plus hautes qu'il y a d'éléments ; groupées par libellé. */
    String dessinerClassement(List<String> libelles, List<SerieGraphiqueDTO> series, UniteIndicateur unite);
}

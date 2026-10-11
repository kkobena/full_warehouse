package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.SensFavorable;
import com.kobe.warehouse.domain.enumeration.UniteIndicateur;
import com.kobe.warehouse.service.dto.pilotage.CelluleAnalyseDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneVendeurDTO;
import java.util.function.Function;

/** Colonne du tableau des vendeurs : libellé, unité, sens favorable et la mesure lue sur la ligne. */
public record ColonneVendeur(String libelle, UniteIndicateur unite, SensFavorable sens, Function<LigneVendeurDTO, CelluleAnalyseDTO> lecture) {}

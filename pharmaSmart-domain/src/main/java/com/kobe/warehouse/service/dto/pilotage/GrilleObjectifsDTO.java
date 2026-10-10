package com.kobe.warehouse.service.dto.pilotage;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Grille de saisie : une ligne par indicateur pouvant porter un objectif, visible de l'utilisateur. {@code moisClos} : nombre de mois
 * de l'année déjà écoulés (0 à 12), dont les objectifs ne se modifient plus.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public record GrilleObjectifsDTO(int annee, int moisClos, List<LigneObjectifsDTO> lignes) {}

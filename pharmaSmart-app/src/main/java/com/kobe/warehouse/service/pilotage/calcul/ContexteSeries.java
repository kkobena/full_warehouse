package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.domain.enumeration.Granularite;
import java.time.LocalDate;
import java.util.List;

/**
 * Ce qui sert à construire chaque série : tranches de la période et de la référence, objectifs, et le même mois N-1 coupé au
 * même jour quand la période est le mois en cours à date ({@code moisN1} vide sinon : pas de projection).
 *
 * @param contreObjectif la référence est l'objectif (comparaison « Objectif »)
 */
public record ContexteSeries(
    TranchesSerie periode,
    TranchesSerie reference,
    Granularite granularite,
    ObjectifsPeriode objectifs,
    boolean contreObjectif,
    List<MesuresPilotage> moisN1,
    LocalDate aujourdhui
) {}

package com.kobe.warehouse.service.exports.modele;

import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.SujetExport;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Ce qu'il faut pour générer un fichier d'export, lu une fois dans l'historique avant la lecture en flux. */
public record TravailExport(
    SujetExport export,
    FormatExport format,
    LocalDate du,
    LocalDate au,
    Map<FiltreExport, List<String>> filtres,
    String fichier
) {}

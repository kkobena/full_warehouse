package com.kobe.warehouse.service.exports.modele;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import java.time.LocalDate;

/** Ce qu'il faut pour générer un fichier d'export, lu une fois dans l'historique avant la lecture en flux. */
public record TravailExport(ExportDonnees export, FormatExport format, LocalDate du, LocalDate au, String fichier) {}

package com.kobe.warehouse.service.dto.exports;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.StatutExport;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Une ligne de l'historique des exports. */
public record ExportFichierDTO(
    Long id,
    ExportDonnees export,
    String libelleExport,
    FormatExport format,
    LocalDate du,
    LocalDate au,
    StatutExport statut,
    Long nombreLignes,
    Long taille,
    String erreur,
    String demandePar,
    LocalDateTime demandeLe,
    LocalDateTime termineLe,
    LocalDateTime expireLe,
    String modele
) {}

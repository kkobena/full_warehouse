package com.kobe.warehouse.service.dto.exports;

import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.StatutExport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Une ligne de l'historique des exports.
 *
 * @param export code de l'entrée du catalogue
 * @param filtres les filtres retenus, déjà rédigés pour l'affichage (« Famille : 2 valeurs ») ; vide si l'export portait sur tout
 */
public record ExportFichierDTO(
    Long id,
    String export,
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
    String modele,
    List<String> filtres
) {}

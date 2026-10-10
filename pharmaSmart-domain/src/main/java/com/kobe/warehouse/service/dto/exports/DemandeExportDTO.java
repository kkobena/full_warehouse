package com.kobe.warehouse.service.dto.exports;

import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Un export à générer ; la période ne sert qu'aux exports périodiques. */
public record DemandeExportDTO(@NotNull ExportDonnees export, @NotNull FormatExport format, LocalDate du, LocalDate au) {}

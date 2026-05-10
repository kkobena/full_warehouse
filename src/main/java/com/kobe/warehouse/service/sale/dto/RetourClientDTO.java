package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.ModeReglementRetour;
import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RetourClientDTO(
    Integer id,
    String reference,
    LocalDateTime createdAt,
    MotifRetourClient motif,
    ModeReglementRetour modeReglement,
    String commentaire,
    int montantTotal,
    String customerName,
    String originalSaleRef,
    LocalDate originalSaleDate,
    String createdByName,
    List<RetourClientLineDTO> lines
) {}

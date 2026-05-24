package com.kobe.warehouse.service.pharmaml.dto;

import com.kobe.warehouse.domain.enumeration.PharmaMlStatut;
import java.time.LocalDateTime;

public record PharmaMlEnvoiDTO(
    Integer id,
    PharmaMlStatut statut,
    String refMessage,
    int tentatives,
    LocalDateTime derniereTentative,
    Integer totalLignes,
    Integer lignesAcceptees,
    Integer lignesRupture,
    LocalDateTime createdAt,
    String fournisseurLibelle
) {}

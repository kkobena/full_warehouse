package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AvoirClientDocumentDTO(
    Integer id,
    String reference,
    LocalDateTime createdAt,
    LocalDateTime clotureLe,
    AvoirClientStatut statut,
    ModeClotureAvoir modeCloture,
    int quantite,
    int montant,
    String commentaire,
    String customerName,
    String produitLibelle,
    String codeCip,
    Long salesLineId,
    LocalDate salesLineDate,
    String numberTransaction,
    String commandeReference,
    String closedByName,
    LocalDate dateExpiration,
    boolean procheExpiration,
    int montantUtilise,
    int montantRestant
) {}

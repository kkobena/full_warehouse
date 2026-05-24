package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.StatutLegal;
import java.time.LocalDate;

public record SaleLineForRetourDTO(
    Long salesLineId,
    LocalDate salesLineDate,
    String produitLibelle,
    String codeCip,
    int quantitySold,
    int netUnitPrice,
    StatutLegal statutLegal,
    boolean retourInterdit,
    boolean thermosensible,
    int montantRemboursableClient,
    int montantTp
) {}

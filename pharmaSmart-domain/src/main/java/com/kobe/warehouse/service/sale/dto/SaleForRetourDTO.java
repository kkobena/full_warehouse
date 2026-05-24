package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import java.time.LocalDate;
import java.util.List;

public record SaleForRetourDTO(
    Long saleId,
    LocalDate saleDate,
    String numberTransaction,
    String customerName,
    NatureVente natureVente,
    boolean hasTiersPayant,
    List<SaleLineForRetourDTO> lines,
    long ancienneteJours,
    boolean depasseDelai
) {}

package com.kobe.warehouse.service.sale.dto;

import java.time.LocalDate;
import java.util.List;

public record SaleForRetourDTO(
    Long saleId,
    LocalDate saleDate,
    String numberTransaction,
    String customerName,
    List<SaleLineForRetourDTO> lines
) {}

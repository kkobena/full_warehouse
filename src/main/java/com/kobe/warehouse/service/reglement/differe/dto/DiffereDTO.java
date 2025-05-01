package com.kobe.warehouse.service.reglement.differe.dto;

import java.util.List;

public record DiffereDTO(
    Long customerId,
    String firstName,
    String lastName,
    Long saleAmount,
    Long paidAmount,
    Long rest,
    List<DiffereItem> differeItems
) {}

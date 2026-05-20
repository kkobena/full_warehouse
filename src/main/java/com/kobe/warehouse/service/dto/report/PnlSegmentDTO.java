package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

public record PnlSegmentDTO(
    String segment,
    String segmentLabel,
    Long ca,
    Long coutAchat,
    Long margeBrute,
    BigDecimal tauxMarge,
    Integer nbTransactions
) {}

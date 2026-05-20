package com.kobe.warehouse.service.dto.report;

import java.math.BigDecimal;

public record PnlFamilleDTO(
    String famille,
    Long ca,
    Long coutAchat,
    Long margeBrute,
    BigDecimal tauxMarge
) {}

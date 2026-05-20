package com.kobe.warehouse.service.dto.report;

import java.util.List;

public record TpComptantMensuelDTO(
    List<String> labels,
    List<Long> caVo,
    List<Long> caVno,
    List<Integer> pctVo
) {}

package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.service.financiel_transaction.dto.MvtParam;
import java.util.stream.Collectors;

public interface MvtCommonService {
  default String buildWhereClause(String query, MvtParam mvtParam) {
    return String.format(
        query,
        mvtParam.getStatuts().stream()
            .map(e -> "'" + e.name() + "'")
            .collect(Collectors.joining(",")),
        mvtParam.getTypeVentes().stream()
            .map(e -> "'" + e.getValue() + "'")
            .collect(Collectors.joining(",")),
        mvtParam.getCategorieChiffreAffaires().stream()
            .map(e -> "'" + e.name() + "'")
            .collect(Collectors.joining(",")));
  }
}

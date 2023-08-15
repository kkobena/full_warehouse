package com.kobe.warehouse.service.stat;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeVente;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.enumeration.StatGroupBy;
import com.kobe.warehouse.service.utils.QueryBuilderConstant;
import com.kobe.warehouse.service.utils.VenteStatQueryBuilder;
import java.time.LocalDate;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;

public interface CommonStatService {
  default Pair<LocalDate, LocalDate> buildPeriode(VenteRecordParamDTO venteRecordParam) {
    LocalDate now = LocalDate.now();
    LocalDate end = now;
    if (Objects.nonNull(venteRecordParam.getFromDate())) {
      now = venteRecordParam.getFromDate();
    }
    if (Objects.nonNull(venteRecordParam.getToDate())) {
      end = venteRecordParam.getToDate();
    }

    return switch (venteRecordParam.getDashboardPeriode()) {
      case daily -> Pair.of(venteRecordParam.getFromDate(), end);
      case weekly -> Pair.of(now.minusWeeks(1), now);
      case monthly -> Pair.of(now.minusMonths(1), now);
      case halfyearly -> Pair.of(now.minusMonths(6), now);
      case yearly -> Pair.of(now.minusYears(1), now);
    };
  }

  default String buildGroupBy(StatGroupBy statGroup) {
    return switch (statGroup) {
      case DAY -> " DATE_FORMAT(s.updated_at,'%Y-%m-%d') ";
      case MONTH -> " MONTH(s.updated_at) ";
      case YEAR -> " YEAR(s.updated_at) ";
      case HOUR -> " HOUR(s.updated_at) ";
    };
  }

  default String buildChiffreAffaire(CategorieChiffreAffaire categorieChiffreAffaire) {
    return switch (categorieChiffreAffaire) {
      case CA -> QueryBuilderConstant.CA;
      case CALLEBASE -> QueryBuilderConstant.CALLEBASE;
      case CA_DEPOT -> QueryBuilderConstant.CA_DEPOT;
      case TO_IGNORE -> QueryBuilderConstant.TO_IGNORE;
    };
  }

  default String buildType(TypeVente typeVente) {
    if (Objects.nonNull(typeVente)) {
      return String.format(VenteStatQueryBuilder.TYPE_VENTE, typeVente);
    }
    return "";
  }

  default String buildQuery(String sql, VenteRecordParamDTO param) {

    String diff = "";

    if (param.isDiffereOnly()) {
      diff = QueryBuilderConstant.DIFFERE;
    }

    return String.format(
        sql,
        this.buildChiffreAffaire(param.getCategorieChiffreAffaire()),
        diff,
        buildType(param.getTypeVente()));
  }
}

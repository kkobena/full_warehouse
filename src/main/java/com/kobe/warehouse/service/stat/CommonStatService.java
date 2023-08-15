package com.kobe.warehouse.service.stat;

import com.kobe.warehouse.service.dto.DashboardPeriode;
import java.time.LocalDate;
import java.util.Objects;
import org.apache.commons.lang3.tuple.Pair;

public interface CommonStatService {
  default Pair<LocalDate, LocalDate> buildPeriode(
      LocalDate fromDate, LocalDate toDate, DashboardPeriode dashboardPeriode) {
    LocalDate now = LocalDate.now();
    LocalDate end = now;
    if (Objects.nonNull(fromDate)) {
      now = fromDate;
    }
    if (Objects.nonNull(toDate)) {
      end = toDate;
    }

    return switch (dashboardPeriode) {
      case daily -> Pair.of(fromDate, end);
      case weekly -> Pair.of(now.minusWeeks(1), now);
      case monthly -> Pair.of(now.minusMonths(1), now);
      case halfyearly -> Pair.of(now.minusMonths(6), now);
      case yearly -> Pair.of(now.minusYears(1), now);
    };
  }
}

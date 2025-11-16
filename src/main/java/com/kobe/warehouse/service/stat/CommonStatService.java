package com.kobe.warehouse.service.stat;

import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
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
}

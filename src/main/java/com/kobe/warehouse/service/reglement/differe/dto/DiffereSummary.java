package com.kobe.warehouse.service.reglement.differe.dto;

import com.kobe.warehouse.service.utils.NumberUtil;

public record DiffereSummary(Long rest) {
    public String restToString() {
        if(rest == null) {
            return null;
        }
        return NumberUtil.formatToString(rest);
    }

}

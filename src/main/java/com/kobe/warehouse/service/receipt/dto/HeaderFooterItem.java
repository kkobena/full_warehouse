package com.kobe.warehouse.service.receipt.dto;

import java.awt.*;
import java.util.Objects;

public record HeaderFooterItem(String value,  int lineBreakNumber, Font font) {

private void validate(){

    if (lineBreakNumber <= 0) {
        throw new IllegalArgumentException("lineBreakNumber cannot be less than or equal to 0");
    }
    if (Objects.isNull(value)) {
        throw new IllegalArgumentException("Value cannot be null");
    }

}

}

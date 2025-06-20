package com.kobe.warehouse.service.mobile.dto;

import java.util.List;

public record BalanceCard(CartName title,Long sum, List<Balance> items) {
}

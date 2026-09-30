package com.kobe.warehouse.service.customer;

import java.util.List;
import java.util.Map;

public record FusionClientResultDTO(Integer targetId, List<Integer> mergedIds, Map<String, Integer> counts) {}

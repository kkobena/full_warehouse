package com.kobe.warehouse.service.dto;

import java.util.List;

public record CloneRayonProduitsDTO(Integer sourceRayonId, List<Integer> targetRayonIds) {}

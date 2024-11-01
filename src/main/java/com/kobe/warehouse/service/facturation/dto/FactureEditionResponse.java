package com.kobe.warehouse.service.facturation.dto;

import java.time.LocalDateTime;

public record FactureEditionResponse(LocalDateTime createdDate, boolean isGroup) {}

package com.kobe.warehouse.service.dto.dashboard;

import java.time.LocalDateTime;

public record AlerteCaisseDTO(
    String type, // INFO, ATTENTION, URGENT, OK
    String titre,
    String message,
    LocalDateTime horodatage
) {}

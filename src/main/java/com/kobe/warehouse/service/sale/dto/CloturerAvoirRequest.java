package com.kobe.warehouse.service.sale.dto;

import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;

public record CloturerAvoirRequest(
    ModeClotureAvoir modeCloture,
    String commentaire
) {}

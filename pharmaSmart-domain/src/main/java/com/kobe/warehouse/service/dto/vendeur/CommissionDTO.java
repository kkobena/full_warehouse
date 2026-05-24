package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;

public record CommissionDTO(
    double montantJour,
    double montantMois,
    double tauxCommission
) implements Serializable {}

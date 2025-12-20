package com.kobe.warehouse.service.dto.vendeur;

import java.io.Serializable;

public record VentesParTypeDTO(
    double ordonnance,
    double conseil,
    double parapharmacie,
    double total
) implements Serializable {}

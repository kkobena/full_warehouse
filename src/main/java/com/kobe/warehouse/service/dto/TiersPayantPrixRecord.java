package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.OptionPrixType;

public record TiersPayantPrixRecord(int prix, int montant, OptionPrixType type, int valeur) {}

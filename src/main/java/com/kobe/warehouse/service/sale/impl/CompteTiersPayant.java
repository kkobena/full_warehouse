package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.ClientTiersPayant;

public record CompteTiersPayant(ClientTiersPayant clientTiersPayant, String numBon, short tauxVente) {
}

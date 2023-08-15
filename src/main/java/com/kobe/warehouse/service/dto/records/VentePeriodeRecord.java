package com.kobe.warehouse.service.dto.records;

import java.time.LocalDate;

public record VentePeriodeRecord(LocalDate mvtDate, VenteRecord venteRecord) {}

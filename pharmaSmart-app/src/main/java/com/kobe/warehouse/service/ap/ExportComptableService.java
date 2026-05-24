package com.kobe.warehouse.service.ap;

import java.io.IOException;
import java.time.LocalDate;

public interface ExportComptableService {

    byte[] export(
        LocalDate startDate,
        LocalDate endDate,
        String format,
        boolean ventes,
        boolean achats,
        boolean mvtCaisse,
        boolean tiersPayant,
        boolean differes,
        boolean tva
    ) throws IOException;
}

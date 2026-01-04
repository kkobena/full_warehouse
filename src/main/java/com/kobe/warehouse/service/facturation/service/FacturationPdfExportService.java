package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.FactureTiersPayant;
import java.util.List;

public interface FacturationPdfExportService {
    byte[] exportToPdf(FactureTiersPayant factureTiersPayant);

    byte[] exportToPdf(List<FactureTiersPayant> factureTiersPayants);

    List<byte[]> exportAll(List<FactureTiersPayant> factureTiersPayants);
}

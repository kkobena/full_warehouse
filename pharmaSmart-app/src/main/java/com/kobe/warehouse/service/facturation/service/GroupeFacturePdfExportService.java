package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.service.facturation.dto.GroupeFactureDto;
import java.util.List;

public interface GroupeFacturePdfExportService {
    byte[] exportToPdf(GroupeFactureDto groupeFacture);

    byte[] exportToPdf(List<GroupeFactureDto> groupeFactures);
}

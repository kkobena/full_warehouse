package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;

public interface EditionService {
    FactureEditionResponse createFactureEdition(EditionSearchParams editionSearchParams);
}

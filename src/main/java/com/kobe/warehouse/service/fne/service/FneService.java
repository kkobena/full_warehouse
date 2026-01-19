package com.kobe.warehouse.service.fne.service;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.service.errors.GenericError;

public interface FneService {
    void create(FactureItemId factureItemId, boolean isGroup) throws GenericError;
}

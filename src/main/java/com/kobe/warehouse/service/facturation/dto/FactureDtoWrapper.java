package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.FactureItemId;

public class FactureDtoWrapper {

    protected FactureItemId factureItemId;

    public FactureItemId getFactureItemId() {
        return factureItemId;
    }

    public FactureDtoWrapper setFactureItemId(FactureItemId factureItemId) {
        this.factureItemId = factureItemId;
        return this;
    }
}

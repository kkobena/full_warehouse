package com.kobe.warehouse.service.facturation.dto;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;

public interface DossierFactureSingleProjection extends DossierFactureProjection {
    TiersPayantCategorie getCategorie();
}

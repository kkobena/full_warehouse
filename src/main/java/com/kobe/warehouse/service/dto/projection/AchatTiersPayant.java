package com.kobe.warehouse.service.dto.projection;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import java.math.BigDecimal;

public record AchatTiersPayant(String libelle, TiersPayantCategorie categorie, Long bonsCount, Long clientCount, Integer montant) {}

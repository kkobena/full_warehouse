package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.mobile.dto.RecapCaisse;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;

public interface MobileCasseRecapService {
    RecapCaisse getRecapCaisse(TicketZParam param);
}

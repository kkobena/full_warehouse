package com.kobe.warehouse.service.mobile.service;

import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.mobile.dto.Dashboard;

public interface MobileDashoardService {
    Dashboard getData(VenteRecordParamDTO venteRecordParam);
}

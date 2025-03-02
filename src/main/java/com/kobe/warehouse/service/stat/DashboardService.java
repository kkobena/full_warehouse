package com.kobe.warehouse.service.stat;

import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import java.util.List;

public interface DashboardService {
    VenteRecordWrapper getPeridiqueCa(VenteRecordParamDTO venteRecordParamDTO);

    AchatRecord getAchatPeriode(AchatRecordParamDTO achatRecordParam);

    List<VentePeriodeRecord> getCaGroupingByPeriode(VenteRecordParamDTO venteRecordParamDTO);

    List<VenteByTypeRecord> getCaGroupingByType(VenteRecordParamDTO venteRecordParamDTO);

    List<VenteModePaimentRecord> getCaGroupingByPaimentMode(VenteRecordParamDTO venteRecordParamDTO);
}

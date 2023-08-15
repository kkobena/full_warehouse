package com.kobe.warehouse.service.stat;

import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.records.AchatRecord;

public interface AchatStatService extends CommonStatService{
    AchatRecord getAchatPeriode(AchatRecordParamDTO achatRecordParam);
}

package com.kobe.warehouse.service.stat.impl;

import com.kobe.warehouse.service.dto.AchatRecordParamDTO;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import com.kobe.warehouse.service.dto.records.VenteByTypeRecord;
import com.kobe.warehouse.service.dto.records.VenteModePaimentRecord;
import com.kobe.warehouse.service.dto.records.VentePeriodeRecord;
import com.kobe.warehouse.service.dto.records.VenteRecordWrapper;
import com.kobe.warehouse.service.stat.AchatStatService;
import com.kobe.warehouse.service.stat.DashboardService;
import com.kobe.warehouse.service.stat.SaleStatService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final Logger LOG = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final SaleStatService saleStatService;
    private final AchatStatService achatStatService;

    public DashboardServiceImpl(SaleStatService saleStatService, AchatStatService achatStatService) {
        this.saleStatService = saleStatService;
        this.achatStatService = achatStatService;
    }

    @Override
    public VenteRecordWrapper getPeridiqueCa(VenteRecordParamDTO venteRecordParamDTO) {
        return this.saleStatService.getPeridiqueCa(venteRecordParamDTO);
    }

    @Override
    public AchatRecord getAchatPeriode(AchatRecordParamDTO achatRecordParam) {
        return this.achatStatService.getAchatPeriode(achatRecordParam);
    }

    @Override
    public List<VentePeriodeRecord> getCaGroupingByPeriode(VenteRecordParamDTO venteRecordParamDTO) {
        return this.saleStatService.getCaGroupingByPeriode(venteRecordParamDTO);
    }

    @Override
    public List<VenteByTypeRecord> getCaGroupingByType(VenteRecordParamDTO venteRecordParamDTO) {
        return this.saleStatService.getCaGroupingByType(venteRecordParamDTO);
    }

    @Override
    public List<VenteModePaimentRecord> getCaGroupingByPaimentMode(VenteRecordParamDTO venteRecordParamDTO) {
        return this.saleStatService.getCaGroupingByPaimentMode(venteRecordParamDTO);
    }
}

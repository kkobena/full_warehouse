package com.kobe.warehouse.service.report.pdf;

import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.report.CommonReportService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public abstract class AbstractStatistiqueReportService extends CommonReportService {

    protected AbstractStatistiqueReportService( StorageService storageService) {
        super(storageService);
    }

    @Override
    protected List<?> getItems() {
        return List.of();
    }

    @Override
    protected int getMaxiRowCount() {
        return 0;
    }

}

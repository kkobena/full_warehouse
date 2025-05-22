package com.kobe.warehouse.service.stock;

import java.util.List;

import com.kobe.warehouse.domain.OrderLine;
import org.springframework.core.io.Resource;

public interface EtiquetteExportService {
    Resource print(List<OrderLine> orderLines);
}

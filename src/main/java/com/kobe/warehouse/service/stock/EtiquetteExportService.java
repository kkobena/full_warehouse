package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.OrderLine;
import java.util.List;
import org.springframework.core.io.Resource;

public interface EtiquetteExportService {
    Resource print(List<OrderLine> orderLines);
}

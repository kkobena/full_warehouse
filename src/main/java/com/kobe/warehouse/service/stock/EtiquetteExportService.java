package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import java.util.List;
import org.springframework.core.io.Resource;

public interface EtiquetteExportService {
    Resource print(List<DeliveryReceiptItem> receiptItem);
}

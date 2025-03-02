package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockEntryDataService {
    Page<DeliveryReceiptDTO> fetchAllReceipts(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable);

    List<DeliveryReceiptDTO> fetchAllDeliveryReceipts(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable);

    Optional<DeliveryReceiptDTO> findOneById(Long id);

    Resource printEtiquette(Long id, int startAt) throws IOException;

    Optional<DeliveryReceiptDTO> findOneByOrderReference(String orderReference);

    Resource exportToPdf(Long id) throws IOException;
}

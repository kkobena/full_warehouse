package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.kobe.warehouse.service.dto.projection.DeliveryReceiptItemProjection;
import com.kobe.warehouse.service.dto.projection.DeliveryReceiptProjection;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface StockEntryDataService {
    Page<DeliveryReceiptDTO> fetchAllReceipts(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable);

    List<DeliveryReceiptDTO> fetchAllDeliveryReceipts(DeliveryReceiptFilterDTO deliveryReceiptFilterDTO, Pageable pageable);

    Optional<DeliveryReceiptDTO> findOneById(CommandeId id);

    Resource printEtiquette(CommandeId id, int startAt) throws IOException;

    Resource exportToPdf(CommandeId id) throws IOException;

    Slice<DeliveryReceiptProjection> fetchAllReceipts(String searchTerm);

    List<DeliveryReceiptItemProjection> findAllByCommandeIdAndCommandeOrderDate(CommandeId id);
}

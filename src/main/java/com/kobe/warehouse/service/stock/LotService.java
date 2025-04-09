package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.OrderLineLot;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.dto.LotJsonValue;
import java.util.Set;

public interface LotService {
    void addLot(LotJsonValue lotJsonValue, DeliveryReceiptItem receiptItem, String receiptRefernce);

    LotJsonValue addLot(LotJsonValue lot);

    LotDTO addLot(LotDTO lot);

    LotDTO editLot(LotDTO lot);

    void remove(LotJsonValue lot);

    void remove(Long lotId);

    void addLot(Set<LotJsonValue> lots, DeliveryReceiptItem receiptItem);
}

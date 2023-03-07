package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.service.dto.LotJsonValue;

public interface LotService {
  void addLot(LotJsonValue lotJsonValue, DeliveryReceiptItem receiptItem, String receiptRefernce);

  LotJsonValue addLot(LotJsonValue lot);

  void remove(LotJsonValue lot);
}

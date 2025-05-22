package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.dto.LotJsonValue;
import java.util.Set;

public interface LotService {
    void addLot(LotJsonValue lotJsonValue, OrderLine orderLine, String receiptRefernce);

    LotJsonValue addLot(LotJsonValue lot);

    LotDTO addLot(LotDTO lot);

    LotDTO editLot(LotDTO lot);

    void remove(LotDTO lot);

    void remove(Long lotId);

    void addLot(Set<LotJsonValue> lots, OrderLine orderLine);
}

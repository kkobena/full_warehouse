package com.kobe.warehouse.service.dto.projection;

import com.kobe.warehouse.domain.OrderLineId;
import java.time.LocalDate;
import java.util.List;

public interface DeliveryReceiptItemProjection {
    String getProduitLibelle();

    String getProduitCip();

    Integer getId();

    LocalDate getOrderDate();

    Integer getQuantityReceived();

    Integer getQuantityRequested();

    Integer getFreeQty();

    Integer getProduitId();

    default OrderLineId getOrderLineId() {
        return new OrderLineId(getId(), getOrderDate());
    }

    List<LotProjection> getLots();
}

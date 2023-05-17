package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryReceiptItemRepository extends JpaRepository<DeliveryReceiptItem, Long> {

    Set<DeliveryReceiptItem> findAllByDeliveryReceiptId(Long deliveryReceiptId);
}

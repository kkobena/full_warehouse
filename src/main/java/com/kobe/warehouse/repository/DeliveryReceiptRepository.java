package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DeliveryReceipt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryReceiptRepository extends JpaRepository<DeliveryReceipt, Long> {
    Optional<DeliveryReceipt> getFirstByOrderReference(String orderRefernce);
}

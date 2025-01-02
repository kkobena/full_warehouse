package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InvoicePaymentItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoicePaymentItemRepository extends JpaRepository<InvoicePaymentItem, Long> {
    List<InvoicePaymentItem> findByInvoicePaymentId(Long id);
}

package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InvoicePaymentItem;
import com.kobe.warehouse.domain.PaymentItemId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoicePaymentItemRepository extends JpaRepository<InvoicePaymentItem, PaymentItemId> {
    List<InvoicePaymentItem> findByInvoicePaymentIdAndInvoicePaymentTransactionDate(Long id, LocalDate transactionDate);
}

package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.InvoicePaymentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoicePaymentItemRepository extends JpaRepository<InvoicePaymentItem, Long> {}

package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DefaultPayment;
import com.kobe.warehouse.domain.PaymentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@SuppressWarnings("unused")
@Repository
public interface DefaultTransactionRepository extends JpaRepository<DefaultPayment, PaymentId> {}

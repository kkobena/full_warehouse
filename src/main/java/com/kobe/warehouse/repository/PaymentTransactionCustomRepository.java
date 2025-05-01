package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.PaymentTransaction;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseProjection;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseSumProjection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface PaymentTransactionCustomRepository {
    Page<MvtCaisseProjection> fetchAll(Specification<PaymentTransaction> specification, Pageable pageable);

    List<MvtCaisseSumProjection> fetchAllSum(Specification<PaymentTransaction> specification);
}

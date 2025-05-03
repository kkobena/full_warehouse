package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.DifferePayment;
import com.kobe.warehouse.service.reglement.differe.dto.CustomerReglementDiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface DifferePaymentDataRepository {
    Page<CustomerReglementDiffereDTO> getDifferePayments(Specification<DifferePayment> specification, Pageable pageable);
    DifferePaymentSummary getDiffereSummary(Specification<DifferePayment> specification);
}

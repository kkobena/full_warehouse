package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.reglement.differe.dto.ClientDiffere;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReglementDiffereService {
    Page<DiffereItem> getDiffereItems(
        Long customerId,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<PaymentStatus> paymentStatuses,
        Pageable pageable
    );

    Page<ClientDiffere> getClientDiffere();

    Page<DiffereDTO> getDiffere(
        Long customerId,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<PaymentStatus> paymentStatuses,
        Pageable pageable
    );

    Optional<DiffereDTO> getOne(Long id);
}

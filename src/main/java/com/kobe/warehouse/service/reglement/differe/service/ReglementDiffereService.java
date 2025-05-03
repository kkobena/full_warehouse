package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.dto.ReportPeriode;
import com.kobe.warehouse.service.reglement.differe.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.core.io.Resource;
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

    Long doReglement(NewDifferePaymentDTO differePayment);

    void printReceipt(long idReglement);

    DiffereSummary getDiffereSummary(
        Long customerId,
        String search,
        LocalDate startDate,
        LocalDate endDate,
        Set<PaymentStatus> paymentStatuses
    );

    DifferePaymentSummary getDifferePaymentSummary(
        Long customerId,
        LocalDate startDate,
        LocalDate endDate
    );

    Resource printListToPdf(Long customerId,
                            String search,
                            LocalDate startDate,
                            LocalDate endDate,
                            Set<PaymentStatus> paymentStatuses);

    Resource printReglementToPdf(Long customerId,
                                 LocalDate startDate,
                                 LocalDate endDate);


    Page<ReglementDiffereWrapperDTO> getReglementsDifferes(
        Long customerId,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable
    );
}

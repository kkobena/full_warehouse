package com.kobe.warehouse.service.reglement.differe.service;

import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.reglement.differe.dto.ClientDiffere;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereItem;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummaryDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.dto.NewDifferePaymentDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereReceiptDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereResponse;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereWrapperDTO;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReglementDiffereService {
    Page<DiffereItem> getDiffereItems(
        Integer customerId,
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        Set<PaymentStatus> paymentStatuses,
        Pageable pageable
    );

    Page<ClientDiffere> getClientDiffere();

    Page<DiffereDTO> getDiffere(Integer customerId, Set<PaymentStatus> paymentStatuses, Pageable pageable);

    Optional<DiffereDTO> getOne(Integer id);

    ReglementDiffereResponse doReglement(NewDifferePaymentDTO differePayment);

    void printReceipt(PaymentId idReglement);

    DiffereSummary getDiffereSummary(Integer customerId, Set<PaymentStatus> paymentStatuses);

    DifferePaymentSummaryDTO getDifferePaymentSummary(Integer customerId, LocalDate fromDate, LocalDate toDate);

    Resource printListToPdf(Integer customerId, Set<PaymentStatus> paymentStatuses);

    Resource printReglementToPdf(Integer customerId, LocalDate fromDate, LocalDate toDate);

    Page<ReglementDiffereWrapperDTO> getReglementsDifferes(Integer customerId, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    ReglementDiffereReceiptDTO getReglementDiffereReceipt(PaymentId id);

    byte[] generateEscPosReceiptForTauri(PaymentId idReglement) throws IOException;
}

package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseWrapper;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface FinancialTransactionService {
    default List<Pair> getTypes() {
        return Stream.of(TypeFinancialTransaction.values()).map(type -> new Pair(type.name(), type.getValue())).toList();
    }

    PaymentId create(FinancialTransactionDTO financialTransactionDTO);

    Page<FinancialTransactionDTO> findAll(FinancielTransactionFilterDTO financielTransactionFilter, Pageable pageable);

    Page<MvtCaisseDTO> findAll(TransactionFilterDTO transactionFilter, Pageable pageable);

    MvtCaisseWrapper getMvtCaisseSum(TransactionFilterDTO transactionFilter);

    Optional<FinancialTransactionDTO> findById(PaymentId id);

    Resource exportToPdf(TransactionFilterDTO transactionFilter) throws IOException;

    byte[] generateEscPosReceiptForTauri(PaymentId idReglement) throws IOException;

    void printReceipt(PaymentId idReglement);

}

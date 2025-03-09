package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.dto.projection.MouvementCaisse;
import com.kobe.warehouse.service.dto.projection.MouvementCaisseGroupByMode;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseWrapper;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

public interface FinancialTransactionService {
    default List<Pair> getTypes() {
        return Stream.of(TypeFinancialTransaction.values()).map(type -> new Pair(type.name(), type.getValue())).toList();
    }

    void create(FinancialTransactionDTO financialTransactionDTO);

    Page<FinancialTransactionDTO> findAll(FinancielTransactionFilterDTO financielTransactionFilter, Pageable pageable);

    Page<MvtCaisseDTO> findAll(TransactionFilterDTO transactionFilter, Pageable pageable);

    List<MvtCaisseDTO> findAll(TransactionFilterDTO transactionFilter);

    MvtCaisseWrapper getMvtCaisseSum(TransactionFilterDTO transactionFilter);

    Optional<FinancialTransactionDTO> findById(Long id);

    Resource exportToPdf(TransactionFilterDTO transactionFilter) throws IOException;

    List<MouvementCaisse> findMouvementsCaisse(LocalDate fromDate, LocalDate toDate);

    List<MouvementCaisseGroupByMode> findMouvementsCaisseGroupBYModeReglement(LocalDate fromDate, LocalDate toDate);
}

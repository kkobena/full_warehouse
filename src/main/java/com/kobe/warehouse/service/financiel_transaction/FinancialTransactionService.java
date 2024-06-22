package com.kobe.warehouse.service.financiel_transaction;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseWrapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FinancialTransactionService {
  default List<Pair> getTypes() {
    return Stream.of(TypeFinancialTransaction.values())
        .map(type -> new Pair(type.name(), type.getValue()))
        .toList();
  }

  void create(FinancialTransactionDTO financialTransactionDTO);

  Page<FinancialTransactionDTO> findAll(
      FinancielTransactionFilterDTO financielTransactionFilter, Pageable pageable);

  Page<MvtCaisseDTO> findAll(TransactionFilterDTO transactionFilter, Pageable pageable);

  List<MvtCaisseDTO> findAll(TransactionFilterDTO transactionFilter);

  MvtCaisseWrapper getMvtCaisseSum(TransactionFilterDTO transactionFilter);

  Optional<FinancialTransactionDTO> findById(Long id);
}

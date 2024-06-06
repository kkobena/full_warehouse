package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseSum;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

public class FinancialTransactionProxy {
  private final FinancialTransactionService financialTransactionService;

  public FinancialTransactionProxy(FinancialTransactionService financialTransactionService) {
    this.financialTransactionService = financialTransactionService;
  }

  public ResponseEntity<Void> create(FinancialTransactionDTO financialTransaction)
      throws URISyntaxException {
    if (financialTransaction.getId() != null) {
      throw new BadRequestAlertException(
          "A new financialTransaction cannot already have an ID",
          "financialTransaction",
          "idexists");
    }
    financialTransactionService.create(financialTransaction);
    return ResponseEntity.status(HttpStatus.CREATED).build();
  }

  public ResponseEntity<List<MvtCaisseDTO>> fetchAllMvtTransactions(
      TransactionFilterDTO transactionFilter, Pageable pageable) {
    Page<MvtCaisseDTO> page = this.financialTransactionService.findAll(transactionFilter, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  public ResponseEntity<List<MvtCaisseSum>> getMvtCaisseSum(
      TransactionFilterDTO transactionFilter) {

    return ResponseEntity.ok(financialTransactionService.getMvtCaisseSum(transactionFilter));
  }

  public ResponseEntity<List<Pair>> getTypes() {
    return ResponseEntity.ok(financialTransactionService.getTypes());
  }

  public ResponseEntity<List<FinancialTransactionDTO>> fetchAllFinancialTransactions(
      FinancielTransactionFilterDTO financielTransactionFilter, Pageable pageable) {
    Page<FinancialTransactionDTO> page =
        this.financialTransactionService.findAll(financielTransactionFilter, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  public ResponseEntity<FinancialTransactionDTO> findById(Long id) {

    return ResponseUtil.wrapOrNotFound(this.financialTransactionService.findById(id));
  }
}

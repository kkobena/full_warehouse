package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import jakarta.validation.Valid;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client")
@Transactional
public class JavaFinancialTransactionResource extends FinancialTransactionProxy {

  public JavaFinancialTransactionResource(FinancialTransactionService financialTransactionService) {
    super(financialTransactionService);
  }

  @PostMapping("/payment-transactions")
  public ResponseEntity<Void> createEvent(
      @Valid @RequestBody FinancialTransactionDTO financialTransaction) throws URISyntaxException {

    return super.create(financialTransaction);
  }

  @GetMapping("/payment-transactions")
  public ResponseEntity<List<FinancialTransactionDTO>> fetchAllFinancialTransactions(
      @RequestParam(value = "search", required = false) String search,
      @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
      @RequestParam(value = "toDate", required = false) LocalDate toDate,
      @RequestParam(value = "userId", required = false) Long userId,
      @RequestParam(value = "typeFinancialTransaction", required = false)
          TypeFinancialTransaction typeFinancialTransaction,
      @RequestParam(value = "categorieChiffreAffaire", required = false)
          CategorieChiffreAffaire categorieChiffreAffaire,
      @RequestParam(value = "paymentMode", required = false) String paymentMode,
      @RequestParam(value = "organismeId", required = false) String organismeId,
      Pageable pageable) {
    return super.fetchAllFinancialTransactions(
        new FinancielTransactionFilterDTO(
            fromDate,
            toDate,
            userId,
            search,
            typeFinancialTransaction,
            categorieChiffreAffaire,
            paymentMode,
            organismeId),
        pageable);
  }

  @GetMapping("/payment-transactions/types")
  public ResponseEntity<List<Pair>> fetchTypes() {
    return super.getTypes();
  }
}

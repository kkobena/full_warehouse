package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.enumeration.Order;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseWrapper;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

public class FinancialTransactionProxy {

    private final FinancialTransactionService financialTransactionService;

    public FinancialTransactionProxy(FinancialTransactionService financialTransactionService) {
        this.financialTransactionService = financialTransactionService;
    }

    public ResponseEntity<Void> create(FinancialTransactionDTO financialTransaction) throws URISyntaxException {
        if (financialTransaction.getId() != null) {
            throw new BadRequestAlertException("A new financialTransaction cannot already have an ID", "financialTransaction", "idexists");
        }
        financialTransactionService.create(financialTransaction);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    public ResponseEntity<List<MvtCaisseDTO>> fetchAllMvtTransactions(TransactionFilterDTO transactionFilter, Pageable pageable) {
        Page<MvtCaisseDTO> page = this.financialTransactionService.findAll(transactionFilter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    public ResponseEntity<MvtCaisseWrapper> getMvtCaisseSum(TransactionFilterDTO transactionFilter) {
        return ResponseEntity.ok(financialTransactionService.getMvtCaisseSum(transactionFilter));
    }

    public ResponseEntity<List<Pair>> getTypes() {
        return ResponseEntity.ok(financialTransactionService.getTypes());
    }

    public ResponseEntity<List<FinancialTransactionDTO>> fetchAllFinancialTransactions(
        FinancielTransactionFilterDTO financielTransactionFilter,
        Pageable pageable
    ) {
        Page<FinancialTransactionDTO> page = this.financialTransactionService.findAll(financielTransactionFilter, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    public ResponseEntity<FinancialTransactionDTO> findById(Long id) {
        return ResponseUtil.wrapOrNotFound(this.financialTransactionService.findById(id));
    }

    @GetMapping("/payment-transactions/pdf")
    public ResponseEntity<Resource> exportToPdf(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam(value = "typeFinancialTransactions", required = false) Set<TypeFinancialTransaction> typeFinancialTransactions,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "paymentModes", required = false) Set<String> paymentModes,
        @RequestParam(value = "order", required = false) Order order,
        @RequestParam(value = "fromTime", required = false) LocalTime fromTime,
        @RequestParam(value = "toTime", required = false) LocalTime toTime,
        HttpServletRequest request
    ) throws IOException {
        final Resource resource =
            this.financialTransactionService.exportToPdf(
                    new TransactionFilterDTO(
                        fromDate,
                        toDate,
                        userId,
                        search,
                        typeFinancialTransactions,
                        categorieChiffreAffaires,
                        paymentModes,
                        order,
                        fromTime,
                        toTime
                    )
                );
        return Utils.printPDF(resource, request);
    }
}

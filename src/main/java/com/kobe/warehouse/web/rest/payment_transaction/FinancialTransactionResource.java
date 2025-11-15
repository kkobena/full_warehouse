package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.PaymentId;
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
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class FinancialTransactionResource {

    private final FinancialTransactionService financialTransactionService;

    public FinancialTransactionResource(FinancialTransactionService financialTransactionService) {
        this.financialTransactionService = financialTransactionService;
    }


    @PostMapping("/payment-transactions")
    public ResponseEntity<PaymentId> createEvent(@Valid @RequestBody FinancialTransactionDTO financialTransaction) throws URISyntaxException {

        if (financialTransaction.getId() != null) {
            throw new BadRequestAlertException("A new financialTransaction cannot already have an ID", "financialTransaction", "idexists");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(financialTransactionService.create(financialTransaction));
    }

    @GetMapping("/payment-transactions/mvt-caisses")
    public ResponseEntity<List<MvtCaisseDTO>> getAllMvtTransactions(
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
        Pageable pageable
    ) {
        Page<MvtCaisseDTO> page = this.financialTransactionService.findAll(new TransactionFilterDTO(
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
        ), pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
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

    @GetMapping("/payment-transactions")
    public ResponseEntity<List<FinancialTransactionDTO>> fetchAllFinancialTransactions(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam(value = "typeFinancialTransaction", required = false) TypeFinancialTransaction typeFinancialTransaction,
        @RequestParam(value = "categorieChiffreAffaire", required = false) CategorieChiffreAffaire categorieChiffreAffaire,
        @RequestParam(value = "paymentMode", required = false) String paymentMode,
        @RequestParam(value = "organismeId", required = false) String organismeId,
        Pageable pageable
    ) {

        Page<FinancialTransactionDTO> page = this.financialTransactionService.findAll(new FinancielTransactionFilterDTO(
            fromDate,
            toDate,
            userId,
            search,
            typeFinancialTransaction,
            categorieChiffreAffaire,
            paymentMode,
            organismeId
        ), pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/payment-transactions/types")
    public ResponseEntity<List<Pair>> fetchTypes() {
        return ResponseEntity.ok(financialTransactionService.getTypes());
    }


    @GetMapping("/payment-transactions/mvt-caisses/sum")
    public ResponseEntity<MvtCaisseWrapper> fetchMvtCaisseSum(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate,
        @RequestParam(value = "userId", required = false) Long userId,
        @RequestParam(value = "typeFinancialTransactions", required = false) Set<TypeFinancialTransaction> typeFinancialTransactions,
        @RequestParam(value = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        @RequestParam(value = "paymentModes", required = false) Set<String> paymentModes,
        @RequestParam(value = "order", required = false) Order order,
        @RequestParam(value = "fromTime", required = false) LocalTime fromTime,
        @RequestParam(value = "toTime", required = false) LocalTime toTime
    ) {

        return ResponseEntity.ok(financialTransactionService.getMvtCaisseSum(new TransactionFilterDTO(
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
        )));
    }

    @GetMapping("/payment-transactions/{id}/{transactionDate}")
    public ResponseEntity<FinancialTransactionDTO> getOne(@PathVariable("id") Long id, @PathVariable("transactionDate") LocalDate transactionDate) {
        return ResponseUtil.wrapOrNotFound(this.financialTransactionService.findById(new PaymentId(id, transactionDate)));

    }


    @GetMapping("/payment-transactions/print-receipt/{id}/{transactionDate}")
    public ResponseEntity<Void> printReceipt(@PathVariable(name = "id") long id,@PathVariable(name = "transactionDate") LocalDate transactionDate) {
        this.financialTransactionService.printReceipt(new PaymentId(id, transactionDate));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/payment-transactions/print-tauri/{id}/{transactionDate}")
    public ResponseEntity<byte[]> getReceiptForTauri(@PathVariable(name = "id") long idReglement,
                                                     @PathVariable(name = "transactionDate") LocalDate transactionDate
    ) {

        try {
            byte[] escPosData = financialTransactionService.
                generateEscPosReceiptForTauri(new PaymentId(idReglement, transactionDate));
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"receipt.bin\"")
                .body(escPosData);
        } catch (IOException e) {

            return ResponseEntity.internalServerError().build();
        }
    }
}

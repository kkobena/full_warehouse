package com.kobe.warehouse.web.rest.payment_transaction;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.service.dto.FinancialTransactionDTO;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.enumeration.Order;
import com.kobe.warehouse.service.dto.filter.FinancielTransactionFilterDTO;
import com.kobe.warehouse.service.dto.filter.TransactionFilterDTO;
import com.kobe.warehouse.service.financiel_transaction.FinancialTransactionService;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseDTO;
import com.kobe.warehouse.service.financiel_transaction.dto.MvtCaisseWrapper;
import jakarta.validation.Valid;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FinancialTransactionResource extends FinancialTransactionProxy {

    public FinancialTransactionResource(FinancialTransactionService financialTransactionService) {
        super(financialTransactionService);
    }

    @PostMapping("/payment-transactions")
    public ResponseEntity<Void> createEvent(@Valid @RequestBody FinancialTransactionDTO financialTransaction) throws URISyntaxException {
        return super.create(financialTransaction);
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
        return super.fetchAllFinancialTransactions(
            new FinancielTransactionFilterDTO(
                fromDate,
                toDate,
                userId,
                search,
                typeFinancialTransaction,
                categorieChiffreAffaire,
                paymentMode,
                organismeId
            ),
            pageable
        );
    }

    @GetMapping("/payment-transactions/types")
    public ResponseEntity<List<Pair>> fetchTypes() {
        return super.getTypes();
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
        return super.fetchAllMvtTransactions(
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
            ),
            pageable
        );
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
        return super.getMvtCaisseSum(
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
    }

    @GetMapping("/payment-transactions/{id}")
    public ResponseEntity<FinancialTransactionDTO> getOne(@PathVariable("id") Long id) {
        return super.findById(id);
    }
}

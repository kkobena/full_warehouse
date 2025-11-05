package com.kobe.warehouse.web.rest.reglement;

import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.reglement.differe.dto.ClientDiffere;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DifferePaymentSummaryDTO;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereSummary;
import com.kobe.warehouse.service.reglement.differe.dto.NewDifferePaymentDTO;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereResponse;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereWrapperDTO;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/differes")
public class DiffereResource {

    private final ReglementDiffereService reglementDiffereService;

    public DiffereResource(ReglementDiffereService reglementDiffereService) {
        this.reglementDiffereService = reglementDiffereService;
    }

    @GetMapping("/customers")
    public ResponseEntity<List<ClientDiffere>> getClients() {
        return ResponseEntity.ok().body(reglementDiffereService.getClientDiffere().getContent());
    }

    @GetMapping
    public ResponseEntity<List<DiffereDTO>> getAllDifferes(
        @RequestParam(name = "customerId", required = false) Long customerId,
        Pageable pageable
    ) {
        Page<DiffereDTO> page = reglementDiffereService.getDiffere(customerId, Set.of(PaymentStatus.IMPAYE), pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);

        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<DiffereDTO> getDiffere(@PathVariable Long id) {

        return ResponseUtil.wrapOrNotFound(reglementDiffereService.getOne(id));
    }

    @GetMapping("/print-receipt/{id}/{transactionDate}")
    public ResponseEntity<Void> printReceipt(@PathVariable(name = "id") long id, LocalDate transactionDate) {
        this.reglementDiffereService.printReceipt(new PaymentId(id, transactionDate));
        return ResponseEntity.ok().build();
    }
    @GetMapping("/print-tauri/{id}/{transactionDate}")
    public ResponseEntity<byte[]> getReceiptForTauri(@PathVariable(name = "id") long idReglement,
                                                     @PathVariable(name = "transactionDate") LocalDate transactionDate
    ) {

        try {
            byte[] escPosData = reglementDiffereService.generateEscPosReceiptForTauri(new PaymentId(idReglement, transactionDate));
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"receipt.bin\"")
                .body(escPosData);
        } catch (IOException e) {

            return ResponseEntity.internalServerError().build();
        }
    }
    @GetMapping("/pdf")
    public ResponseEntity<Resource> exportList(
        HttpServletRequest request,
        @RequestParam(name = "customerId", required = false) Long customerId
    ) {
        return Utils.printPDF(reglementDiffereService.printListToPdf(customerId, Set.of(PaymentStatus.IMPAYE)), request);
    }

    @GetMapping("/reglements/pdf")
    public ResponseEntity<Resource> printReglementToPdf(
        HttpServletRequest request,
        @RequestParam(name = "customerId", required = false) Long customerId,
        @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false) LocalDate toDate
    ) {
        return Utils.printPDF(reglementDiffereService.printReglementToPdf(customerId, fromDate, toDate), request);
    }

    @GetMapping("/reglements")
    public ResponseEntity<List<ReglementDiffereWrapperDTO>> getReglementsDifferes(
        @RequestParam(name = "customerId", required = false) Long customerId,
        @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false) LocalDate toDate,
        Pageable pageable
    ) {
        Page<ReglementDiffereWrapperDTO> page = reglementDiffereService.getReglementsDifferes(customerId, fromDate, toDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/do-reglement")
    public ResponseEntity<ReglementDiffereResponse> doReglement(@RequestBody NewDifferePaymentDTO differePayment) {
        return ResponseEntity.ok().body(reglementDiffereService.doReglement(differePayment));
    }

    @GetMapping("/reglements/summary")
    public ResponseEntity<DifferePaymentSummaryDTO> getReglementDiffereSummary(
        @RequestParam(name = "customerId", required = false) Long customerId,
        @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false) LocalDate toDate
    ) {
        return ResponseEntity.ok().body(reglementDiffereService.getDifferePaymentSummary(customerId, fromDate, toDate));
    }

    @GetMapping("/summary")
    public ResponseEntity<DiffereSummary> getDiffereSummary(
        @RequestParam(name = "customerId", required = false) Long customerId
    ) {
        return ResponseEntity.ok().body(reglementDiffereService.getDiffereSummary(customerId, Set.of(PaymentStatus.IMPAYE)));
    }
}

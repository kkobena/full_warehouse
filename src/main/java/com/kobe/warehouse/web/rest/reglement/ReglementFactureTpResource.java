package com.kobe.warehouse.web.rest.reglement;

import com.kobe.warehouse.domain.PaymentId;
import com.kobe.warehouse.service.reglement.ReglementRegistry;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentItemDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentParam;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import com.kobe.warehouse.service.reglement.service.ReglementDataService;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ReglementFactureTpResource {

    private final ReglementRegistry reglementRegistry;
    private final ReglementDataService reglementDataService;

    public ReglementFactureTpResource(ReglementRegistry reglementRegistry, ReglementDataService reglementDataService) {
        this.reglementRegistry = reglementRegistry;
        this.reglementDataService = reglementDataService;
    }

    @PostMapping("/reglement-factures-tp")
    public ResponseEntity<ResponseReglementDTO> doReglement(@Valid @RequestBody ReglementParam reglementParam) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            reglementRegistry.getService(reglementParam.getMode()).doReglement(reglementParam)
        );
    }

    @GetMapping("/reglements")
    public ResponseEntity<List<InvoicePaymentDTO>> getAll(
        @RequestParam(required = false, name = "fromDate") LocalDate fromDate,
        @RequestParam(required = false, name = "toDate") LocalDate toDate,
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(required = false, name = "organismeId") Long organismeId,
        @RequestParam(required = false, name = "grouped", defaultValue = "false") boolean grouped
    ) {
        return ResponseEntity.ok(
            this.reglementDataService.fetchInvoicesPayments(new InvoicePaymentParam(search, organismeId, fromDate, toDate, grouped))
        );
    }

    @GetMapping("/reglements/items/{idReglement}/{transactionDate}")
    public ResponseEntity<List<InvoicePaymentItemDTO>> getItems(
        @PathVariable(name = "idReglement") long idReglement,
        @PathVariable(name = "transactionDate") LocalDate transactionDate
    ) {
        return ResponseEntity.ok(this.reglementDataService.getInvoicePaymentsItems(new PaymentId(idReglement, transactionDate)));
    }

    @GetMapping("/reglements/group/items/{idReglement}/{transactionDate}")
    public ResponseEntity<List<InvoicePaymentDTO>> getGroupeItems(
        @PathVariable(name = "idReglement") long idReglement,
        @PathVariable(name = "transactionDate") LocalDate transactionDate
    ) {
        return ResponseEntity.ok(this.reglementDataService.getInvoicePaymentsGroupItems(new PaymentId(idReglement, transactionDate)));
    }

    @GetMapping("/reglements/print-receipt/{idReglement}/{transactionDate}")
    public ResponseEntity<Void> printReceipt(
        @PathVariable(name = "idReglement") long idReglement,
        @PathVariable(name = "transactionDate") LocalDate transactionDate
    ) {
        this.reglementDataService.printReceipt(new PaymentId(idReglement, transactionDate));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/reglements/{id}/{transactionDate}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @PathVariable LocalDate transactionDate) {
        this.reglementDataService.deleteReglement(new PaymentId(id, transactionDate));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reglements/all")
    public ResponseEntity<Void> deleteAll(@RequestParam(name = "ids") Set<PaymentId> ids) {
        this.reglementDataService.deleteReglement(ids);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reglements/pdf")
    public ResponseEntity<Resource> exportAllInvoicesPayment(
        HttpServletRequest request,
        @RequestParam(required = false, name = "fromDate") LocalDate fromDate,
        @RequestParam(required = false, name = "toDate") LocalDate toDate,
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(required = false, name = "organismeId") Long organismeId,
        @RequestParam(required = false, name = "grouped", defaultValue = "false") boolean grouped
    ) {
        return Utils.printPDF(
            reglementDataService.printToPdf(new InvoicePaymentParam(search, organismeId, fromDate, toDate, grouped)),
            request
        );
    }
}

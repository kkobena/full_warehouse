package com.kobe.warehouse.web.rest.reglement;

import com.kobe.warehouse.service.reglement.ReglementRegistry;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentItemDTO;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentParam;
import com.kobe.warehouse.service.reglement.dto.ReglementParam;
import com.kobe.warehouse.service.reglement.dto.ResponseReglementDTO;
import com.kobe.warehouse.service.reglement.service.ReglementDataService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/reglements/items/{idReglement}")
    public ResponseEntity<List<InvoicePaymentItemDTO>> getItems(@PathVariable(name = "idReglement") long idReglement) {
        return ResponseEntity.ok(this.reglementDataService.getInvoicePaymentsItems(idReglement));
    }

    @GetMapping("/reglements/group/items/{idReglement}")
    public ResponseEntity<List<InvoicePaymentDTO>> getGroupeItems(@PathVariable(name = "idReglement") long idReglement) {
        return ResponseEntity.ok(this.reglementDataService.getInvoicePaymentsGroupItems(idReglement));
    }

    @GetMapping("/reglements/print-receipt/{idReglement}")
    public ResponseEntity<Void> printReceipt(@PathVariable(name = "idReglement") long idReglement) {
        this.reglementDataService.printReceipt(idReglement);
        return ResponseEntity.ok().build();
    }
}

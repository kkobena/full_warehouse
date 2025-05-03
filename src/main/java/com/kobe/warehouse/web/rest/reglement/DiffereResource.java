package com.kobe.warehouse.web.rest.reglement;

import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.service.reglement.differe.dto.ClientDiffere;
import com.kobe.warehouse.service.reglement.differe.dto.DiffereDTO;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

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
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false) LocalDate toDate,
        @RequestParam(name = "paymentStatuses", required = false) Set<PaymentStatus> paymentStatuses,
        Pageable pageable
    ) {
        Page<DiffereDTO> page = reglementDiffereService.getDiffere(customerId, search, fromDate, toDate, paymentStatuses, pageable);

        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<DiffereDTO> getDiffere(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(reglementDiffereService.getOne(id));
    }

    @GetMapping("/print-receipt/{id}")
    public ResponseEntity<Void> printReceipt(@PathVariable(name = "id") long id) {
        this.reglementDiffereService.printReceipt(id);
        return ResponseEntity.ok().build();
    }
}

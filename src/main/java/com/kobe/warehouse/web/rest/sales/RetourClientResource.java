package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.service.sale.RetourClientService;
import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientRequest;
import com.kobe.warehouse.service.sale.dto.SaleForRetourDTO;
import java.time.LocalDate;
import java.util.List;

import com.kobe.warehouse.web.util.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;


@RestController
@RequestMapping("/api")
public class RetourClientResource {

    private final RetourClientService retourClientService;

    public RetourClientResource(RetourClientService retourClientService) {
        this.retourClientService = retourClientService;
    }

    @GetMapping("/sales/retours/sale")
    public ResponseEntity<SaleForRetourDTO> getSaleForRetour(@RequestParam String ref) {
        return ResponseEntity.ok(retourClientService.findSaleByRef(ref));
    }

    @GetMapping("/sales/retours")
    public ResponseEntity<List<RetourClientDTO>> getRetours(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        Pageable pageable
    ) {
        Page<RetourClientDTO> page = retourClientService.findAll(search, fromDate, toDate, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/sales/retours")
    public ResponseEntity<RetourClientDTO> validerRetour(@RequestBody RetourClientRequest request) {
        return ResponseEntity.ok(retourClientService.validerRetour(request));
    }
}

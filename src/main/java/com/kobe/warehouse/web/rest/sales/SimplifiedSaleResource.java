package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.sale.SimplifiedSaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import jakarta.validation.Valid;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Sales}.
 */
@RestController
@RequestMapping("/api")
public class SimplifiedSaleResource {


    private final SimplifiedSaleService simplifiedSaleService;


    public SimplifiedSaleResource(SimplifiedSaleService simplifiedSaleService) {
        this.simplifiedSaleService = simplifiedSaleService;
    }


    @PostMapping("/sales/simplified/save")
    public ResponseEntity<FinalyseSaleDTO> createSimplifiedCashSale(@Valid @RequestBody CashSaleDTO cashSaleDTO) {
        FinalyseSaleDTO result = simplifiedSaleService.createCashSale(cashSaleDTO);
        return ResponseEntity.ok()
            .body(result);
    }

    @GetMapping("/sales/simplified")
    public ResponseEntity<List<CashSaleDTO>> getAllSales(
        @RequestParam(name = "search", required = false) String search

    ) {

        Slice<CashSaleDTO> slice = simplifiedSaleService.getList(search);

        return ResponseEntity.ok().body(slice.getContent());
    }

}

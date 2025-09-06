package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.web.rest.proxy.SalesResourceProxy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URISyntaxException;
import java.time.LocalDate;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Sales}.
 */
@RestController
@RequestMapping("/java-client")
@Transactional
public class JavaSalesResource extends SalesResourceProxy {

    public JavaSalesResource(SaleService saleService) {
        super(saleService);
    }

    @PutMapping("/sales/comptant/put-on-hold")
    public ResponseEntity<ResponseDTO> putCashSaleOnHold(@Valid @RequestBody CashSaleDTO sale) throws URISyntaxException {
        return super.putCashSaleOnHold(sale);
    }

    @PostMapping("/sales/comptant")
    public ResponseEntity<CashSaleDTO> createCashSale(@Valid @RequestBody CashSaleDTO cashSaleDTO, HttpServletRequest request)
        throws URISyntaxException {
        return super.createCashSale(cashSaleDTO, request);
    }

    @PutMapping("/sales/comptant/save")
    public ResponseEntity<FinalyseSaleDTO> closeCashSale(@Valid @RequestBody CashSaleDTO cashSaleDTO, HttpServletRequest request) {
        return super.closeCashSale(cashSaleDTO, request);
    }

    @PostMapping("/sales/add-item/comptant")
    public ResponseEntity<SaleLineDTO> addItemComptant(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        return super.addItemComptant(saleLineDTO);
    }

    @PutMapping("/sales/update-item/quantity-requested")
    public ResponseEntity<SaleLineDTO> updateItemQtyRequested(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        return super.updateItemQtyRequested(saleLineDTO);
    }

    @PutMapping("/sales/update-item/price")
    public ResponseEntity<SaleLineDTO> updateItemPrice(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        return super.updateItemPrice(saleLineDTO);
    }

    @PutMapping("/sales/update-item/quantity-sold")
    public ResponseEntity<SaleLineDTO> updateItemQtySold(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        return super.updateItemQtySold(saleLineDTO);
    }

    @DeleteMapping("/sales/delete-item/{id}/{saleDate}")
    public ResponseEntity<Void> deleteSaleItem(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        return super.deleteSaleItem(new SaleLineId(id, saleDate));
    }

    @DeleteMapping("/sales/prevente/{id}/{saleDate}")
    public ResponseEntity<Void> deleteSalePrevente(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        return super.deleteSalePrevente(new SaleId(id, saleDate));
    }

    @DeleteMapping("/sales/cancel/comptant/{id}/{saleDate}")
    public ResponseEntity<Void> cancelCashSale(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        return super.cancelCashSale(new SaleId(id, saleDate));
    }

    @PutMapping("/sales/comptant/add-customer")
    public ResponseEntity<Void> addCustommerToCashSale(@Valid @RequestBody UpdateSaleInfo updateSaleInfo) {
        return super.addCustommerToCashSale(updateSaleInfo);
    }

    @DeleteMapping("/sales/comptant/remove-customer/{id}/{saleDate}")
    public ResponseEntity<Void> removeCustommerToCashSale(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        return super.removeCustommerToCashSale(new SaleId(id, saleDate));
    }
}

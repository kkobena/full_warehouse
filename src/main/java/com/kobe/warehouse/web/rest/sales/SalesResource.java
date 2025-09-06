package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import com.kobe.warehouse.web.util.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Sales}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class SalesResource {

    private static final String ENTITY_NAME = "sales";

    private final SaleService saleService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public SalesResource(SaleService saleService) {
        this.saleService = saleService;
    }

    @PutMapping("/sales/comptant/put-on-hold")
    public ResponseEntity<ResponseDTO> putCashSaleOnHold(@Valid @RequestBody CashSaleDTO sale) {
        if (sale.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        ResponseDTO result = saleService.putCashSaleOnHold(sale);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, sale.getId().toString()))
            .body(result);
    }

    @PostMapping("/sales/comptant")
    public ResponseEntity<CashSaleDTO> createCashSale(@Valid @RequestBody CashSaleDTO cashSaleDTO, HttpServletRequest request)
        throws URISyntaxException {
        if (cashSaleDTO.getId() != null) {
            throw new BadRequestAlertException("A new sales cannot already have an ID", ENTITY_NAME, "idexists");
        }
        cashSaleDTO.setCaisseNum(request.getRemoteAddr()).setPosteName(request.getRemoteHost());
        cashSaleDTO.setCaisseEndNum(cashSaleDTO.getCaisseNum());

        CashSaleDTO result = saleService.createCashSale(cashSaleDTO);
        return ResponseEntity.created(new URI("/api/sales/comptant/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/comptant/save")
    public ResponseEntity<FinalyseSaleDTO> closeCashSale(@Valid @RequestBody CashSaleDTO cashSaleDTO, HttpServletRequest request) {
        cashSaleDTO.setCaisseEndNum(request.getRemoteAddr()).setPosteName(request.getRemoteHost());
        FinalyseSaleDTO result = saleService.save(cashSaleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, cashSaleDTO.getId().toString()))
            .body(result);
    }

    @PostMapping("/sales/add-item/comptant")
    public ResponseEntity<SaleLineDTO> addItemComptant(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.addOrUpdateSaleLine(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/add-item/comptant/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/update-item/quantity-requested")
    public ResponseEntity<SaleLineDTO> updateItemQtyRequested(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.updateItemQuantityRequested(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/update-item/quantity-requested/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/update-item/price")
    public ResponseEntity<SaleLineDTO> updateItemPrice(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.updateItemRegularPrice(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/update-item/price/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/update-item/quantity-sold")
    public ResponseEntity<SaleLineDTO> updateItemQtySold(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.updateItemQuantitySold(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/update-item/quantity-sold/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @DeleteMapping("/sales/delete-item/{id}/{saleDate}")
    public ResponseEntity<Void> deleteSaleItem(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleService.deleteSaleLineById(new SaleLineId(id, saleDate));
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/sales/prevente/{id}/{saleDate}")
    public ResponseEntity<Void> deleteSalePrevente(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleService.deleteSalePrevente(new SaleId(id, saleDate));
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/sales/cancel/comptant/{id}/{saleDate}")
    public ResponseEntity<Void> cancelCashSale(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleService.cancelCashSale(new SaleId(id, saleDate));
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PutMapping("/sales/comptant/add-customer")
    public ResponseEntity<Void> addCustommerToCashSale(@Valid @RequestBody UpdateSaleInfo updateSaleInfo) {
        saleService.setCustomer(updateSaleInfo);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/sales/comptant/remove-customer/{id}/{saleDate}")
    public ResponseEntity<Void> removeCustommerToCashSale(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleService.removeCustomer(new SaleId(id, saleDate));
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/sales/comptant/authorize-action")
    public ResponseEntity<Void> authorizeAction(
        @Valid @RequestBody UtilisationCleSecuriteDTO utilisationCleSecurite,
        HttpServletRequest request
    ) {
        utilisationCleSecurite.setCaisse(request.getRemoteHost());
        saleService.authorizeAction(utilisationCleSecurite);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/sales/comptant/add-remise")
    public ResponseEntity<Void> addRemise(@Valid @RequestBody UpdateSaleInfo updateSaleInfo) {
        saleService.processDiscount(updateSaleInfo);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/sales/comptant/remove-remise/{id}/{saleDate}")
    public ResponseEntity<Void> removeRemiseFromCashSale(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleService.removeRemiseFromCashSale(new SaleId(id, saleDate));
        return ResponseEntity.ok().build();
    }
}

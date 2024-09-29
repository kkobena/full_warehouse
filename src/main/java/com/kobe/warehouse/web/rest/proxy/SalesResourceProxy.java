package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.sale.SaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.web.util.HeaderUtil;

@Transactional
public class SalesResourceProxy {

    private static final String ENTITY_NAME = "sales";
    private final Logger log = LoggerFactory.getLogger(SalesResourceProxy.class);
    private final SaleService saleService;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public SalesResourceProxy(SaleService saleService) {
        this.saleService = saleService;
    }

    public ResponseEntity<ResponseDTO> putCashSaleOnHold(CashSaleDTO sale) throws URISyntaxException {
        if (sale.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        ResponseDTO result = saleService.putCashSaleOnHold(sale);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, sale.getId().toString()))
            .body(result);
    }

    public ResponseEntity<CashSaleDTO> createCashSale(CashSaleDTO cashSaleDTO, HttpServletRequest request) throws URISyntaxException {
        log.debug("REST request to save cashSaleDTO : {}", cashSaleDTO);
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

    public ResponseEntity<FinalyseSaleDTO> closeCashSale(CashSaleDTO cashSaleDTO, HttpServletRequest request) {
        if (cashSaleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        cashSaleDTO.setCaisseEndNum(request.getRemoteAddr()).setPosteName(request.getRemoteHost());
        FinalyseSaleDTO result = saleService.save(cashSaleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, cashSaleDTO.getId().toString()))
            .body(result);
    }

    public ResponseEntity<SaleLineDTO> addItemComptant(SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.addOrUpdateSaleLine(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/add-item/comptant/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    public ResponseEntity<SaleLineDTO> updateItemQtyRequested(SaleLineDTO saleLineDTO) throws URISyntaxException {
        log.debug("REST request to save saleLineDTO : {}", saleLineDTO);
        SaleLineDTO result = saleService.updateItemQuantityRequested(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/update-item/quantity-requested/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    public ResponseEntity<SaleLineDTO> updateItemPrice(SaleLineDTO saleLineDTO) throws URISyntaxException {
        log.debug("REST request to save saleLineDTO : {}", saleLineDTO);
        SaleLineDTO result = saleService.updateItemRegularPrice(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/update-item/price/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    public ResponseEntity<SaleLineDTO> updateItemQtySold(SaleLineDTO saleLineDTO) throws URISyntaxException {
        log.debug("REST request to save saleLineDTO : {}", saleLineDTO);
        SaleLineDTO result = saleService.updateItemQuantitySold(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/update-item/quantity-sold/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    public ResponseEntity<Void> deleteSaleItem(Long id) {
        log.debug("REST request to delete Sales : {}", id);
        saleService.deleteSaleLineById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    public ResponseEntity<Void> deleteSalePrevente(Long id) {
        log.debug("REST request to delete Sales : {}", id);
        saleService.deleteSalePrevente(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    public ResponseEntity<Void> cancelCashSale(Long id) {
        log.debug("REST request to delete Sales : {}", id);
        saleService.cancelCashSale(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    public ResponseEntity<Void> addCustommerToCashSale(KeyValue keyValue) {
        saleService.setCustomer(keyValue);
        return ResponseEntity.accepted().build();
    }

    public ResponseEntity<Void> removeCustommerToCashSale(Long id) {
        saleService.removeCustomer(id);
        return ResponseEntity.accepted().build();
    }
}

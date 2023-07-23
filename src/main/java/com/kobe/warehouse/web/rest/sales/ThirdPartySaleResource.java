package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.service.ThirdPartySaleService;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import tech.jhipster.web.util.HeaderUtil;

/**
 * REST controller for managing {@link Sales}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class ThirdPartySaleResource {

    private static final String ENTITY_NAME = "sales";
    private final Logger log = LoggerFactory.getLogger(ThirdPartySaleResource.class);
    private final ThirdPartySaleService saleService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public ThirdPartySaleResource(ThirdPartySaleService saleService) {
        this.saleService = saleService;
    }

    @PutMapping("/sales/assurance/put-on-hold")
    public ResponseEntity<ResponseDTO> putSaleOnHold(@Valid @RequestBody ThirdPartySaleDTO sale) throws URISyntaxException {
        if (sale.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        ResponseDTO result = saleService.putThirdPartySaleOnHold(sale);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, sale.getId().toString()))
            .body(result);
    }

    @PostMapping("/sales/assurance")
    public ResponseEntity<ThirdPartySaleDTO> createSale(
        @Valid @RequestBody ThirdPartySaleDTO thirdPartySaleDTO,
        HttpServletRequest request
    ) throws URISyntaxException {
        log.debug("REST request to save thirdPartySaleDTO : {}", thirdPartySaleDTO);
        if (thirdPartySaleDTO.getId() != null) {
            throw new BadRequestAlertException("A new sales cannot already have an ID", ENTITY_NAME, "idexists");
        }
        thirdPartySaleDTO.setCaisseNum(request.getRemoteHost());
        thirdPartySaleDTO.setCaisseEndNum(request.getRemoteHost());

        ThirdPartySaleDTO result = saleService.createSale(thirdPartySaleDTO);
        return ResponseEntity
            .created(new URI("/api/sales/assurance/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/assurance/save")
    public ResponseEntity<ResponseDTO> closeSale(@Valid @RequestBody ThirdPartySaleDTO thirdPartySaleDTO) throws URISyntaxException {
        if (thirdPartySaleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        ResponseDTO result = saleService.save(thirdPartySaleDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, thirdPartySaleDTO.getId().toString()))
            .body(result);
    }

    @PostMapping("/sales/add-item/assurance")
    public ResponseEntity<SaleLineDTO> addItem(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.createOrUpdateSaleLine(saleLineDTO);
        return ResponseEntity
            .created(new URI("/api/sales/add-item/assurance/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/update-item/quantity-requested/assurance")
    public ResponseEntity<SaleLineDTO> updateItemQtyRequested(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        log.debug("REST request to save saleLineDTO : {}", saleLineDTO);
        SaleLineDTO result = saleService.updateItemQuantityRequested(saleLineDTO);
        return ResponseEntity
            .created(new URI("/api/sales/update-item/quantity-requested/assurance" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/update-item/price/assurance")
    public ResponseEntity<SaleLineDTO> updateItemPrice(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        log.debug("REST request to save saleLineDTO : {}", saleLineDTO);
        SaleLineDTO result = saleService.updateItemRegularPrice(saleLineDTO);
        return ResponseEntity
            .created(new URI("/api/sales/update-item/price/assurance" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/update-item/quantity-sold/assurance")
    public ResponseEntity<SaleLineDTO> updateItemQtySold(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        log.debug("REST request to save saleLineDTO : {}", saleLineDTO);
        SaleLineDTO result = saleService.updateItemQuantitySold(saleLineDTO);
        return ResponseEntity
            .created(new URI("/api/sales/update-item/quantity-sold/assurance" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @DeleteMapping("/sales/delete-item/assurance/{id}")
    public ResponseEntity<Void> deleteSaleItem(@PathVariable Long id) {
        log.debug("REST request to delete Sales : {}", id);
        saleService.deleteSaleLineById(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/sales/prevente/assurance/{id}")
    public ResponseEntity<Void> deleteSalePrevente(@PathVariable Long id) {
        log.debug("REST request to delete Sales : {}", id);
        saleService.deleteSalePrevente(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/sales/cancel/assurance/{id}")
    public ResponseEntity<Void> cancelSale(@PathVariable Long id) {
        log.debug("REST request to delete Sales : {}", id);
        saleService.cancelSale(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/sales/remove-tiers-payant/assurance/{id}/{saleId}")
    public ResponseEntity<Void> removeThirdPartySaleLineToSales(
        @PathVariable("id") Long clientTiersPayantId,
        @PathVariable("saleId") Long saleId
    ) {
        saleService.removeThirdPartySaleLineToSales(clientTiersPayantId, saleId);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, saleId.toString()))
            .build();
    }

    @PutMapping("/sales/add-assurance/assurance/{id}")
    public ResponseEntity<ThirdPartySaleDTO> addThirdPartySaleLineToSales(
        @PathVariable Long id,
        @Valid @RequestBody ClientTiersPayantDTO dto
    ) throws URISyntaxException {
        ThirdPartySaleDTO result = saleService.addThirdPartySaleLineToSales(dto, id);
        return ResponseEntity
            .created(new URI("/api/sales/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }
}

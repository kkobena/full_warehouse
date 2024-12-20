package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.KeyValue;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.UtilisationCleSecuriteDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.errors.PlafondVenteException;
import com.kobe.warehouse.service.sale.ThirdPartySaleService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<ResponseDTO> putSaleOnHold(@Valid @RequestBody ThirdPartySaleDTO sale) {
        if (sale.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        ResponseDTO result = saleService.putThirdPartySaleOnHold(sale);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, sale.getId().toString()))
            .body(result);
    }

    @PostMapping("/sales/assurance")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
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
        return ResponseEntity.created(new URI("/api/sales/assurance/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/assurance/save")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<FinalyseSaleDTO> closeSale(@Valid @RequestBody ThirdPartySaleDTO thirdPartySaleDTO) {
        if (thirdPartySaleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        FinalyseSaleDTO result = saleService.save(thirdPartySaleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, thirdPartySaleDTO.getId().toString()))
            .body(result);
    }

    @PostMapping("/sales/add-item/assurance")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<SaleLineDTO> addItem(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.createOrUpdateSaleLine(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/add-item/assurance/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/update-item/quantity-requested/assurance")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<SaleLineDTO> updateItemQtyRequested(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.updateItemQuantityRequested(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/update-item/quantity-requested/assurance" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/update-item/price/assurance")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<SaleLineDTO> updateItemPrice(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.updateItemRegularPrice(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/update-item/price/assurance" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/sales/update-item/quantity-sold/assurance")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<SaleLineDTO> updateItemQtySold(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleService.updateItemQuantitySold(saleLineDTO);
        return ResponseEntity.created(new URI("/api/sales/update-item/quantity-sold/assurance" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @DeleteMapping("/sales/delete-item/assurance/{id}")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<Void> deleteSaleItem(@PathVariable Long id) {
        saleService.deleteSaleLineById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/sales/prevente/assurance/{id}")
    public ResponseEntity<Void> deleteSalePrevente(@PathVariable Long id) {
        saleService.deleteSalePrevente(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/sales/cancel/assurance/{id}")
    public ResponseEntity<Void> cancelSale(@PathVariable Long id) {
        saleService.cancelSale(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @DeleteMapping("/sales/remove-tiers-payant/assurance/{id}/{saleId}")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<Void> removeThirdPartySaleLineToSales(
        @PathVariable("id") Long clientTiersPayantId,
        @PathVariable("saleId") Long saleId
    ) {
        saleService.removeThirdPartySaleLineToSales(clientTiersPayantId, saleId);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, saleId.toString()))
            .build();
    }

    @PutMapping("/sales/add-assurance/assurance/{id}")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<Void> addThirdPartySaleLineToSales(@PathVariable Long id, @Valid @RequestBody ClientTiersPayantDTO dto) {
        saleService.addThirdPartySaleLineToSales(dto, id);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/sales/assurance/transform")
    public ResponseEntity<Long> transform(
        @RequestParam(name = "natureVente") NatureVente natureVente,
        @RequestParam(name = "saleId") Long saleId
    ) {
        if (saleId == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Long id = saleService.changeCashSaleToThirdPartySale(saleId, natureVente);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, saleId.toString()))
            .body(id);
    }

    @PutMapping("/sales/assurance/transform/add-customer")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<Void> updateTransformedSale(@Valid @RequestBody ThirdPartySaleDTO thirdPartySale) {
        saleService.updateTransformedSale(thirdPartySale);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/sales/assurance/change/customer")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<Void> changeCustomer(@Valid @RequestBody KeyValue keyValue) {
        saleService.changeCustomer(keyValue);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/sales/assurance/save/completed-sale")
    @Transactional(noRollbackFor = { PlafondVenteException.class })
    public ResponseEntity<FinalyseSaleDTO> editSale(@Valid @RequestBody ThirdPartySaleDTO thirdPartySaleDTO) {
        if (thirdPartySaleDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        FinalyseSaleDTO result = saleService.editSale(thirdPartySaleDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, thirdPartySaleDTO.getId().toString()))
            .body(result);
    }

    @PostMapping("/sales/assurance/authorize-action")
    public ResponseEntity<Void> authorizeAction(
        @Valid @RequestBody UtilisationCleSecuriteDTO utilisationCleSecurite,
        HttpServletRequest request
    ) {
        utilisationCleSecurite.setCaisse(request.getRemoteHost());
        saleService.authorizeAction(utilisationCleSecurite);
        return ResponseEntity.accepted().build();
    }

    @PutMapping("/sales/assurance/add-remise")
    public ResponseEntity<Void> addRemise(@Valid @RequestBody KeyValue keyValue) {
        saleService.processDiscount(keyValue);
        return ResponseEntity.accepted().build();
    }
}

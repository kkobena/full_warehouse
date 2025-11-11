package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.SaleLineId;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.records.UpdateSaleInfo;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.sale.SaleDepotExtensionService;
import com.kobe.warehouse.service.sale.dto.FinalyseSaleDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URISyntaxException;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Sales}.
 */
@RestController
@RequestMapping("/api/vente-depot")
public class VenteDepotResource {

    private static final String ENTITY_NAME = "sales";

    private final SaleDepotExtensionService saleDepotExtensionService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public VenteDepotResource(SaleDepotExtensionService saleDepotExtensionService) {
        this.saleDepotExtensionService = saleDepotExtensionService;
    }

    @PostMapping
    public ResponseEntity<DepotExtensionSaleDTO> createSale(
        @Valid @RequestBody DepotExtensionSaleDTO depotExtensionSaleDTO,
        HttpServletRequest request
    ) {
        if (depotExtensionSaleDTO.getId() != null) {
            throw new BadRequestAlertException("A new sales cannot already have an ID", ENTITY_NAME, "idexists");
        }
        depotExtensionSaleDTO.setCaisseNum(request.getRemoteAddr()).setPosteName(request.getRemoteHost());
        depotExtensionSaleDTO.setCaisseEndNum(depotExtensionSaleDTO.getCaisseNum());

        DepotExtensionSaleDTO result = saleDepotExtensionService.create(depotExtensionSaleDTO);
        return ResponseEntity.accepted().body(result);
    }

    @PutMapping("/save")
    public ResponseEntity<FinalyseSaleDTO> closeSale(
        @Valid @RequestBody DepotExtensionSaleDTO depotExtensionSaleDTO,
        HttpServletRequest request
    ) {
        depotExtensionSaleDTO.setCaisseEndNum(request.getRemoteAddr()).setPosteName(request.getRemoteHost());
        FinalyseSaleDTO result = saleDepotExtensionService.save(depotExtensionSaleDTO);
        return ResponseEntity.accepted().body(result);
    }

    @PostMapping("/add-item")
    public ResponseEntity<SaleLineDTO> addItem(@Valid @RequestBody SaleLineDTO saleLineDTO) {
        SaleLineDTO result = saleDepotExtensionService.addOrUpdateSaleLine(saleLineDTO);
        return ResponseEntity.accepted().body(result);
    }

    @PutMapping("/update-item/quantity-requested")
    public ResponseEntity<SaleLineDTO> updateItemQtyRequested(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleDepotExtensionService.updateItemQuantityRequested(saleLineDTO);
        return ResponseEntity.accepted().body(result);
    }

    @PutMapping("/update-item/price")
    public ResponseEntity<SaleLineDTO> updateItemPrice(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleDepotExtensionService.updateItemRegularPrice(saleLineDTO);
        return ResponseEntity.accepted().body(result);
    }

    @PutMapping("/update-item/quantity-sold")
    public ResponseEntity<SaleLineDTO> updateItemQtySold(@Valid @RequestBody SaleLineDTO saleLineDTO) throws URISyntaxException {
        SaleLineDTO result = saleDepotExtensionService.updateItemQuantitySold(saleLineDTO);
        return ResponseEntity.accepted().body(result);
    }

    @DeleteMapping("/delete-item/{id}/{saleDate}")
    public ResponseEntity<Void> deleteSaleItem(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleDepotExtensionService.deleteSaleLineById(new SaleLineId(id, saleDate));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/prevente/{id}/{saleDate}")
    public ResponseEntity<Void> deleteSalePrevente(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleDepotExtensionService.deleteSalePrevente(new SaleId(id, saleDate));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cancel/{id}/{saleDate}")
    public ResponseEntity<Void> cancel(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleDepotExtensionService.cancel(new SaleId(id, saleDate));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/add-remise")
    public ResponseEntity<Void> addRemise(@Valid @RequestBody UpdateSaleInfo updateSaleInfo) {
        saleDepotExtensionService.processDiscount(updateSaleInfo);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/remove-remise/{id}/{saleDate}")
    public ResponseEntity<Void> removeRemiseSale(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        saleDepotExtensionService.removeRemiseFromSale(new SaleId(id, saleDate));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/change-depot")
    public ResponseEntity<SaleId> changeDepot(
        @RequestParam(name = "saleId") Long saleId,
        @RequestParam(name = "saleDate") LocalDate SaleDate,
        @RequestParam(name = "depotId") Integer depotId
    ) {
        if (saleId == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        var id = new SaleId(saleId, SaleDate);
        saleDepotExtensionService.changeDepot(id, depotId);
        return ResponseEntity.ok().body(id);
    }
}

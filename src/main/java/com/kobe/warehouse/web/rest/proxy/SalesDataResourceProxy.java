package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class SalesDataResourceProxy {

    private final Logger log = LoggerFactory.getLogger(SalesDataResourceProxy.class);
    private final SaleDataService saleDataService;

    public SalesDataResourceProxy(SaleDataService saleDataService) {
        this.saleDataService = saleDataService;
    }

    public ResponseEntity<SaleDTO> getSales(SaleId id) {
        log.debug("REST request to get Sales : {}", id);
        SaleDTO sale = saleDataService.fetchPurchaseBy(id.getId(), id.getSaleDate());
        return ResponseEntity.ok().body(sale);
    }

    public ResponseEntity<SaleDTO> getSalesForEdit(SaleId id) {
        log.debug("REST request to get Sales : {}", id);
        Optional<SaleDTO> saleDTO = saleDataService.fetchPurchaseForEditBy(id.getId(), id.getSaleDate());
        if (saleDTO.isEmpty()) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok().body(saleDTO.get());
    }

    public ResponseEntity<List<SaleDTO>> getAllSalesPreventes(String search, String typeVente, Long userId) {
        log.debug("REST request to get a page of Sales");
        List<SaleDTO> data = saleDataService.allPrevente(search, typeVente, userId);
        return ResponseEntity.ok().body(data);
    }

    public ResponseEntity<List<SaleDTO>> getAllSales(
        String search,
        LocalDate fromDate,
        LocalDate toDate,
        String fromHour,
        String toHour,
        Boolean global,
        Long userId,
        String type,
        Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of Sales");
        Page<SaleDTO> page = saleDataService.listVenteTerminees(
            search,
            fromDate,
            toDate,
            fromHour,
            toHour,
            global,
            userId,
            type,
            null,
            null,
            categorieChiffreAffaires,
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}

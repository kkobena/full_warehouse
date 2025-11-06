package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.stock.GestionStockDepotService;
import com.kobe.warehouse.web.util.PaginationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stock-depots")
public class StockDepotResource {
    private final GestionStockDepotService gestionStockDepotService;

    public StockDepotResource(GestionStockDepotService gestionStockDepotService) {
        this.gestionStockDepotService = gestionStockDepotService;
    }

    @GetMapping
    public ResponseEntity<List<ProduitDTO>> getStock(
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(name = "magasinId") Long magasinId,
        @RequestParam(required = false, name = "storageId") Long storageId,
        @RequestParam(required = false, name = "rayonId") Long rayonId,
        @RequestParam(required = false, name = "deconditionne") Boolean deconditionne,
        @RequestParam(required = false, name = "deconditionnable") Boolean deconditionnable,
        @RequestParam(required = false, name = "status") Status status,
        @RequestParam(required = false, name = "tableauNot") Long tableauNot,
        Pageable pageable
    ) {
        Page<ProduitDTO> page = gestionStockDepotService.findAll(
            new ProduitCriteria()
                .setSearch(search)
                .setStatus(status)
                .setDeconditionnable(deconditionnable)
                .setDeconditionne(deconditionne)
                .setMagasinId(magasinId)
                .setTableauNot(tableauNot)
                .setRayonId(rayonId)
                .setStorageId(storageId),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/sales")
    public ResponseEntity<List<SaleDTO>> getAllSales(
        @RequestParam(name = "magasinId",required = false) Long magasinId,
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false) LocalDate toDate,
        @RequestParam(name = "fromHour", required = false) String fromHour,
        @RequestParam(name = "toHour", required = false) String toHour,
        @RequestParam(name = "global", required = false) Boolean global,
        @RequestParam(name = "userId", required = false) Long userId,

        Pageable pageable
    ) {

        Page<SaleDTO> page = gestionStockDepotService.getVenteDepot(null, magasinId,
            search,
            fromDate,
            toDate,
            fromHour,
            toHour,
            global,
            userId,
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}

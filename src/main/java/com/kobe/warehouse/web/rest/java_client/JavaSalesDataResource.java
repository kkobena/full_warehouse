package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.web.rest.proxy.SalesDataResourceProxy;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client")
public class JavaSalesDataResource extends SalesDataResourceProxy {

    public JavaSalesDataResource(SaleDataService saleDataService) {
        super(saleDataService);
    }

    /**
     * {@code GET /sales/:id} : get the "id" sales.
     *
     * @param id the id of the sales to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the sales, or
     * with status {@code 404 (Not Found)}.
     */
    @GetMapping("/sales/{id}/{saleDate}")
    public ResponseEntity<SaleDTO> getSales(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        return super.getSales(new SaleId(id, saleDate));
    }

    @GetMapping("/sales/edit/{id}/{saleDate}")
    public ResponseEntity<SaleDTO> getSalesForEdit(@PathVariable("id") Long id, @PathVariable("saleDate") LocalDate saleDate) {
        return super.getSalesForEdit(new SaleId(id, saleDate));
    }

    @GetMapping("/sales/prevente")
    public ResponseEntity<List<SaleDTO>> getAllSalesPreventes(
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "type", required = false) String typeVente,
        @RequestParam(name = "userId", required = false) Long userId
    ) {
        return super.getAllSalesPreventes(search, typeVente, userId);
    }

    @GetMapping("/sales")
    public ResponseEntity<List<SaleDTO>> getAllSales(
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false) LocalDate toDate,
        @RequestParam(name = "fromHour", required = false) String fromHour,
        @RequestParam(name = "toHour", required = false) String toHour,
        @RequestParam(name = "global", required = false) Boolean global,
        @RequestParam(name = "userId", required = false) Long userId,
        @RequestParam(name = "type", required = false) String type,
        @RequestParam(name = "categorieChiffreAffaires", required = false) Set<CategorieChiffreAffaire> categorieChiffreAffaires,
        Pageable pageable
    ) {
        return super.getAllSales(search, fromDate, toDate, fromHour, toHour, global, userId, type, categorieChiffreAffaires, pageable);
    }
}

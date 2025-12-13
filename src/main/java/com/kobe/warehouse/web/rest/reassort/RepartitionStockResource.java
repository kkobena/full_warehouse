package com.kobe.warehouse.web.rest.reassort;

import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.reassort.dto.ManualRepartitionRequest;
import com.kobe.warehouse.service.reassort.dto.RepartionQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartitionStockProduitDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for managing stock repartition history
 */
@RestController
@RequestMapping("/api/repartition-stock")
public class RepartitionStockResource {

    private static final Logger LOG = LoggerFactory.getLogger(RepartitionStockResource.class);
    private final RepartitionStockService repartitionStockService;

    public RepartitionStockResource(RepartitionStockService repartitionStockService) {
        this.repartitionStockService = repartitionStockService;
    }

    /**
     * {@code GET /api/repartition-stock} : Get stock repartition history with filters
     *
     * @param storageId       filter by storage
     * @param userId          filter by user
     * @param searchTerm      search in product name/code
     * @param dateDebut       start date filter
     * @param dateFin         end date filter
     * @param typeRepartition filter by repartition type
     * @param stockProduitId  filter by stock product
     * @param pageable        pagination information
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of repartitions in body
     */
    @GetMapping("")
    public ResponseEntity<List<RepartitionStockProduitDto>> getRepartitionStock(
        @RequestParam(required = false) Integer storageId,
        @RequestParam(required = false) Integer userId,
        @RequestParam(required = false) String searchTerm,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @RequestParam(required = false) String typeRepartition,
        @RequestParam(required = false) Integer stockProduitId,
        Pageable pageable
    ) {
        LOG.debug("REST request to get repartition stock history");

        RepartionSearchQueryDto searchQuery = new RepartionSearchQueryDto(
            storageId,
            userId,
            searchTerm,
            dateDebut,
            dateFin,
            typeRepartition != null ? com.kobe.warehouse.domain.enumeration.TypeRepartition.valueOf(typeRepartition) : null,
            stockProduitId
        );

        Page<RepartitionStockProduitDto> page = repartitionStockService.fetchRepartitionStockProduits(searchQuery, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code POST /api/repartition-stock/manual} : Manually process stock repartition (single or multiple)
     *
     * @param requests the list of manual repartition requests containing source, destination and quantity
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}
     */
    @PostMapping("/manual")
    public ResponseEntity<Void> processManualRepartition(@RequestBody List<ManualRepartitionRequest> requests) {
        LOG.debug("REST request to manually process {} stock repartitions", requests.size());

        List<RepartionQueryDto> queryDtos = requests.stream()
            .map(request -> new RepartionQueryDto(
                request.stockSourceId(),
                request.stockDestinationId(),
                request.quantity()
            ))
            .toList();

        repartitionStockService.process(queryDtos);
        return ResponseEntity.ok().build();
    }
}

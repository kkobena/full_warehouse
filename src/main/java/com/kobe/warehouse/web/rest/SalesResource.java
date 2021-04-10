package com.kobe.warehouse.web.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.SaleService;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Sales}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class SalesResource {

    private final Logger log = LoggerFactory.getLogger(SalesResource.class);

    private static final String ENTITY_NAME = "sales";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;
    @Autowired
    private SalesRepository salesRepository;
    @Autowired
    private SaleService saleService;

    /**
     * {@code POST  /sales} : Create a new sales.
     *
     * @param sales the sales to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with
     * body the new sales, or with status {@code 400 (Bad Request)} if the
     * sales has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/sales")
    public ResponseEntity<SaleDTO> createSales(@Valid @RequestBody SaleDTO sales) throws URISyntaxException {
        log.debug("REST request to save Sales : {}", sales);
        if (sales.getId() != null) {
            throw new BadRequestAlertException("A new sales cannot already have an ID", ENTITY_NAME, "idexists");
        }
        SaleDTO result = saleService.createSale(sales);
        return ResponseEntity
            .created(new URI("/api/sales/" + result.getId())).headers(HeaderUtil
                .createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /sales} : Updates an existing sales.
     *
     * @param sales the sales to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     * the updated sales, or with status {@code 400 (Bad Request)} if the
     * sales is not valid, or with status
     * {@code 500 (Internal Server Error)} if the sales couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/sales")
    public ResponseEntity<Sales> updateSales(@Valid @RequestBody Sales sales) throws URISyntaxException {
        log.debug("REST request to update Sales : {}", sales);
        if (sales.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        Sales result = salesRepository.save(sales);
        return ResponseEntity.ok().headers(
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, sales.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /sales} : get all the sales.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list
     * of sales in body.
     */
    @GetMapping("/sales")
    public ResponseEntity<List<SaleDTO>> getAllSales(@RequestParam(name = "query", required = false) String query,
                                                     @RequestParam(name = "fromDate", required = false)
                                                         LocalDate fromDate, @RequestParam(name = "toDate", required = false)
                                                         LocalDate toDate, Pageable pageable) {
        log.debug("REST request to get a page of Sales");
        List<SaleDTO> data = saleService.customerPurchases(query, fromDate, toDate);
        return ResponseEntity.ok().body(data);
    }

    /**
     * {@code GET  /sales/:id} : get the "id" sales.
     *
     * @param id the id of the sales to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body
     * the sales, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/sales/{id}")
    public ResponseEntity<SaleDTO> getSales(@PathVariable Long id) {
        log.debug("REST request to get Sales : {}", id);
        SaleDTO sale = saleService.purchaseBy(id);
        return ResponseEntity.ok().body(sale);
    }

    /**
     * {@code DELETE  /sales/:id} : delete the "id" sales.
     *
     * @param id the id of the sales to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/sales/{id}")
    public ResponseEntity<Void> deleteSales(@PathVariable Long id) {
        log.debug("REST request to delete Sales : {}", id);
        salesRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PutMapping("/sales/save")
    public ResponseEntity<SaleDTO> saveSale(@Valid @RequestBody SaleDTO sale) throws URISyntaxException {

        if (sale.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        SaleDTO result = saleService.save(sale);
        return ResponseEntity.ok()
            .headers(
                HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, sale.getId().toString()))
            .body(result);
    }

    @GetMapping("/sales/print/{id}")
    public ResponseEntity<Resource> print(@PathVariable Long id, HttpServletRequest request) throws IOException {
        String gereratefilePath = saleService.printInvoice(id);
        Path filePath = Paths.get(gereratefilePath);
        Resource resource = new UrlResource(filePath.toUri());
        String contentType = null;
        try {
            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            log.info("Could not determine file type.");
        }
        if (contentType == null) {
            contentType = "application/pdf";
        }
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }
}

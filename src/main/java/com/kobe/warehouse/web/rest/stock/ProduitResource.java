package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.ProductActivityService;
import com.kobe.warehouse.service.dto.ProductActivityDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import com.kobe.warehouse.web.rest.proxy.ProduitResourceProxy;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Produit}.
 */
@RestController
@RequestMapping("/api")
public class ProduitResource extends ProduitResourceProxy {

    private static final String ENTITY_NAME = "produit";
    private final Logger log = LoggerFactory.getLogger(ProduitResource.class);
    private final ProduitRepository produitRepository;
    private final ProduitService produitService;
    private final ProductActivityService productActivityService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public ProduitResource(
        ProduitRepository produitRepository,
        ProduitService produitService,
        ProductActivityService productActivityService
    ) {
        super(produitService);
        this.produitRepository = produitRepository;
        this.produitService = produitService;
        this.productActivityService = productActivityService;
    }

    /**
     * {@code POST /produits} : Create a new produit.
     *
     * @param produitDTO the produit to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new
     * produit, or with status {@code 400 (Bad Request)} if the produit has already an ID.
     */
    @PostMapping("/produits")
    public ResponseEntity<Void> createProduit(@Valid @RequestBody ProduitDTO produitDTO) {
        log.debug("REST request to save Produit : {}", produitDTO);
        if (produitDTO.getId() != null) {
            throw new BadRequestAlertException("A new produit cannot already have an ID", ENTITY_NAME, "idexists");
        }
        produitService.save(produitDTO);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code PUT /produits} : Updates an existing produit.
     *
     * @param produitDTO the produit to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated
     * produit, or with status {@code 400 (Bad Request)} if the produit is not valid, or with status
     * {@code 500 (Internal Server Error)} if the produit couldn't be updated.
     */
    @PutMapping("/produits")
    public ResponseEntity<Void> updateProduit(@Valid @RequestBody ProduitDTO produitDTO) {
        log.debug("REST request to update Produit : {}", produitDTO);
        if (produitDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        produitService.update(produitDTO);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code GET /produits} : get all the produits.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of produits in
     * body.
     */

    @GetMapping("/produits")
    public ResponseEntity<List<ProduitDTO>> getAllProduits(
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(required = false, name = "storageId") Integer storageId,
        @RequestParam(required = false, name = "rayonId") Integer rayonId,
        @RequestParam(required = false, name = "deconditionne") Boolean deconditionne,
        @RequestParam(required = false, name = "deconditionnable") Boolean deconditionnable,
        @RequestParam(required = false, name = "status") Status status,
        @RequestParam(required = false, name = "familleId") Integer familleId,
        @RequestParam(required = false, name = "tableauId") Integer tableauId,
        @RequestParam(required = false, name = "tableauNot") Integer tableauNot,
        Pageable pageable
    ) {
        Page<ProduitDTO> page = produitService.findAll(
            new ProduitCriteria()
                .setSearch(search)
                .setStatus(status)
                .setDeconditionnable(deconditionnable)
                .setDeconditionne(deconditionne)
                .setFamilleId(familleId)
                .setTableauId(tableauId)
                .setTableauNot(tableauNot)
                .setRayonId(rayonId)
                .setStorageId(storageId),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET /produits/:id} : get the "id" produit.
     *
     * @param id the id of the produit to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the produit, or
     * with status {@code 404 (Not Found)}.
     */

    @GetMapping("/produits/{id}")
    public ResponseEntity<ProduitDTO> getProduit(@PathVariable Integer id) {
        log.debug("REST request to get Produit : {}", id);
        Optional<ProduitDTO> produit = produitService.findOne(id);
        return ResponseUtil.wrapOrNotFound(produit);
    }

    /**
     * {@code DELETE /produits/:id} : delete the "id" produit.
     *
     * @param id the id of the produit to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/produits/{id}")
    public ResponseEntity<Void> deleteProduit(@PathVariable Integer id) {
        log.debug("REST request to delete Produit : {}", id);
        produitRepository.deleteById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PutMapping("/produits/detail")
    public ResponseEntity<Void> updateDetail(@Valid @RequestBody ProduitDTO produitDTO) {
        log.debug("REST request to update Produit : {}", produitDTO);
        if (produitDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        produitService.updateDetail(produitDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/produits/lite")
    public ResponseEntity<List<ProduitDTO>> getAllLite(
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(required = false, name = "storageId") Integer storageId,
        @RequestParam(required = false, name = "rayonId") Integer rayonId,
        @RequestParam(required = false, name = "deconditionne") Boolean deconditionne,
        @RequestParam(required = false, name = "deconditionnable") Boolean deconditionnable,
        @RequestParam(required = false, name = "status") Status status,
        @RequestParam(required = false, name = "familleId") Integer familleId,
        @RequestParam(required = false, name = "tableauId") Integer tableauId,
        @RequestParam(required = false, name = "tableauNot") Integer tableauNot,
        @RequestParam(required = false, name = "remisable") Boolean remisable,
        Pageable pageable
    ) {
        return super.getAllLite(
            new ProduitCriteria()
                .setSearch(search)
                .setStatus(status)
                .setDeconditionnable(deconditionnable)
                .setDeconditionne(deconditionne)
                .setFamilleId(familleId)
                .setTableauId(tableauId)
                .setTableauNot(tableauNot)
                .setRayonId(rayonId)
                .setStorageId(storageId)
                .setRemisable(remisable),
            pageable
        );
    }

    @GetMapping("/produits/code/{code}")
    public ResponseEntity<List<ProduitSearch>> findByCode(@PathVariable("code") String code

    ) {
        return super.search(code, null, Pageable.ofSize(1));
    }

    @GetMapping("/produits/search")
    public ResponseEntity<List<ProduitSearch>> search(
        @RequestParam(name = "search") String search,
        @RequestParam(required = false, name = "magasinId") Integer magasinId,
        Pageable pageable
    ) {
        return super.search(search, magasinId, pageable);
    }

    @GetMapping("/produits/activity")
    public ResponseEntity<List<ProductActivityDTO>> getProduitActivity(
        @RequestParam(name = "produitId") Long produitId,
        @RequestParam(name = "fromDate") LocalDate fromDate,
        @RequestParam(name = "toDate") LocalDate toDate
    ) {
        return ResponseEntity.ok().body(this.productActivityService.getProductActivity(produitId, fromDate, toDate));
    }
}

package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.service.dto.produit.merge.ProduitMergePreviewDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeRequestDTO;
import com.kobe.warehouse.service.dto.produit.merge.ProduitMergeResultDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.stock.ProduitMergeService;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller pour la fusion de produits en doublon du catalogue.
 * La restriction d'accès se fait côté frontend via le privilège {@code pr-fusion-produit}.
 */
@RestController
@RequestMapping("/api/produits/merge")
public class ProduitMergeResource {

    private static final String ENTITY_NAME = "produit";
    private final Logger log = LoggerFactory.getLogger(ProduitMergeResource.class);
    private final ProduitMergeService produitMergeService;

    public ProduitMergeResource(ProduitMergeService produitMergeService) {
        this.produitMergeService = produitMergeService;
    }

    /**
     * {@code POST /api/produits/merge/preview} : simule la fusion sans rien modifier.
     */
    @PostMapping("/preview")
    public ResponseEntity<ProduitMergePreviewDTO> preview(
        @RequestParam("targetId") Integer targetId,
        @RequestBody List<Integer> sourceIds
    ) {
        log.debug("REST request to preview produit merge : target={}, sources={}", targetId, sourceIds);
        if (targetId == null || sourceIds == null || sourceIds.isEmpty()) {
            throw new BadRequestAlertException("Cible et sources sont obligatoires", ENTITY_NAME, "mergemissingargs");
        }
        return ResponseEntity.ok(produitMergeService.preview(targetId, sourceIds));
    }

    /**
     * {@code POST /api/produits/merge} : exécute la fusion.
     */
    @PostMapping
    public ResponseEntity<ProduitMergeResultDTO> merge(@Valid @RequestBody ProduitMergeRequestDTO request) {
        log.debug("REST request to merge produits : {}", request);
        return ResponseEntity.ok(produitMergeService.merge(request));
    }
}

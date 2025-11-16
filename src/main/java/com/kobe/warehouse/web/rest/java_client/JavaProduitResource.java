package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import com.kobe.warehouse.web.rest.proxy.ProduitResourceProxy;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Produit}.
 */
@RestController
@RequestMapping("/java-client")
public class JavaProduitResource extends ProduitResourceProxy {

    public JavaProduitResource(ProduitService produitService) {
        super(produitService);
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

    @GetMapping("/produits/search")
    public ResponseEntity<List<ProduitSearch>> search(
        @RequestParam(name = "search") String search,
        @RequestParam(required = false, name = "magasinId") Integer magasinId,
        Pageable pageable
    ) {
        return super.search(search, magasinId, pageable);
    }
}

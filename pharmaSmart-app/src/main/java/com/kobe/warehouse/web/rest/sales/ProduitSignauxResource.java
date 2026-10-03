package com.kobe.warehouse.web.rest.sales;

import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.sale.ProduitSignauxService;
import com.kobe.warehouse.service.sale.dto.ProduitSignauxDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiresNavAccess({ "ventes", "nouvelle-vente", "nouvelle-prevente" })
public class ProduitSignauxResource {

    private final ProduitSignauxService produitSignauxService;

    public ProduitSignauxResource(ProduitSignauxService produitSignauxService) {
        this.produitSignauxService = produitSignauxService;
    }

    @GetMapping("/sales/produits-signaux")
    public ResponseEntity<List<ProduitSignauxDTO>> getSignaux(@RequestParam("ids") List<Integer> ids) {
        return ResponseEntity.ok(produitSignauxService.chargerSignaux(ids));
    }
}

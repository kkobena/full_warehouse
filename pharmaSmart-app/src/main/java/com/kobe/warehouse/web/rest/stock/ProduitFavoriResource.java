package com.kobe.warehouse.web.rest.stock;

import com.kobe.warehouse.security.navaccess.NavAccessExempt;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.stock.ProduitFavoriService;
import com.kobe.warehouse.service.stock.dto.FavoriSuggere;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Grille des produits favoris du comptoir (docs/PLAN-ANALYSE-COMPARATIVE-INTERFACE-VENTE-COMPTOIR.md §3). La lire est le
 * fait de tout caissier ; l'épingler, la retirer ou la réordonner relève du catalogue.
 */
@RestController
@RequestMapping("/api/produits-favoris")
@RequiresNavAccess("catalogue")
public class ProduitFavoriResource {

    private final ProduitFavoriService produitFavoriService;

    public ProduitFavoriResource(ProduitFavoriService produitFavoriService) {
        this.produitFavoriService = produitFavoriService;
    }

    @GetMapping
    @NavAccessExempt("lecture, nécessaire à la vente pour tous les rôles")
    public ResponseEntity<List<ProduitSearch>> lister() {
        return ResponseEntity.ok(produitFavoriService.lister());
    }

    /** Pour remplir la grille : les produits les plus vendus au comptoir, pas encore épinglés. Relève du catalogue. */
    @GetMapping("/suggestions")
    public ResponseEntity<List<FavoriSuggere>> suggerer(@RequestParam(defaultValue = "12") int limit) {
        return ResponseEntity.ok(produitFavoriService.suggerer(Math.max(1, Math.min(limit, 50))));
    }

    @PutMapping("/{produitId}")
    @RequiresNavAccess({"catalogue", "ventes.favoris.gerer"})
    public ResponseEntity<Void> ajouter(@PathVariable Integer produitId) {
        produitFavoriService.ajouter(produitId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{produitId}")
    @RequiresNavAccess({"catalogue", "ventes.favoris.gerer"})
    public ResponseEntity<Void> retirer(@PathVariable Integer produitId) {
        produitFavoriService.retirer(produitId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/ordre")
    @RequiresNavAccess({"catalogue", "ventes.favoris.gerer"})
    public ResponseEntity<Void> reordonner(@RequestBody List<Integer> produitIds) {
        produitFavoriService.reordonner(produitIds);
        return ResponseEntity.noContent().build();
    }
}

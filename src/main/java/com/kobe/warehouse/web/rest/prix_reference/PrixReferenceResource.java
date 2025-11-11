package com.kobe.warehouse.web.rest.prix_reference;

import com.kobe.warehouse.service.produit_prix.dto.PrixReferenceDTO;
import com.kobe.warehouse.service.produit_prix.service.PrixRererenceService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prix-reference")
public class PrixReferenceResource {

    private final PrixRererenceService prixRererenceService;

    public PrixReferenceResource(PrixRererenceService prixRererenceService) {
        this.prixRererenceService = prixRererenceService;
    }

    @GetMapping("/{produitId}")
    public ResponseEntity<List<PrixReferenceDTO>> getAll(@PathVariable(name = "produitId") Integer produitId) {
        return ResponseEntity.ok().body(this.prixRererenceService.findAllByProduitId(produitId));
    }

    @PostMapping
    public ResponseEntity<Void> add(@RequestBody @Validated PrixReferenceDTO prixReference) {
        this.prixRererenceService.add(prixReference);
        return ResponseEntity.accepted().build();
    }

    @PutMapping
    public ResponseEntity<Void> update(@RequestBody PrixReferenceDTO prixReference) {
        this.prixRererenceService.update(prixReference);
        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        this.prixRererenceService.delete(id);
        return ResponseEntity.accepted().build();
    }
}

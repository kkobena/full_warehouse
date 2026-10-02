package com.kobe.warehouse.web.rest.dci;

import com.kobe.warehouse.security.navaccess.NavAccessExempt;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.referentiel.ProduitReferentielDTO;
import com.kobe.warehouse.service.referentiel.RapprochementDTO;
import com.kobe.warehouse.service.referentiel.RapprochementProduitService;
import com.kobe.warehouse.web.util.PaginationUtil;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Référentiel médicament au comptoir (fiche, substituts, RCP) et relecture des rapprochements que
 * les règles n'ont pas acceptés seules. La relecture relève de l'écran DCI.
 */
@RestController
@RequestMapping("/api/referentiel-medicament")
@RequiresNavAccess("dci")
public class ReferentielMedicamentResource {

    private final RapprochementProduitService service;

    public ReferentielMedicamentResource(RapprochementProduitService service) {
        this.service = service;
    }

    @GetMapping("/produits/{produitId}")
    @NavAccessExempt("lecture de référentiel, consultée au comptoir et dans la fiche produit")
    public ResponseEntity<ProduitReferentielDTO> lireFiche(@PathVariable Integer produitId) {
        return ResponseEntity.ok(service.lireFiche(produitId));
    }

    @GetMapping("/rapprochements")
    public ResponseEntity<List<RapprochementDTO>> listerRapprochementsARelire(Pageable pageable) {
        Page<RapprochementDTO> page = service.listerRapprochementsARelire(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/rapprochements/{produitId}/valider")
    public ResponseEntity<Void> valider(@PathVariable Integer produitId) {
        service.valider(produitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rapprochements/{produitId}/rejeter")
    public ResponseEntity<Void> rejeter(@PathVariable Integer produitId) {
        service.rejeter(produitId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/rapprochements/{produitId}/reinitialiser")
    public ResponseEntity<Void> reinitialiser(@PathVariable Integer produitId) {
        service.reinitialiser(produitId);
        return ResponseEntity.noContent().build();
    }
}

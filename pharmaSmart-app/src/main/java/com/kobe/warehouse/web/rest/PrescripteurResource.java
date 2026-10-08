package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.ordonnance.PrescripteurDTO;
import com.kobe.warehouse.service.ordonnance.PrescripteurService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Référentiel local des prescripteurs (docs/PLAN-EXTRACTION-ORDONNANCE-OCR.md, §13.3). */
@RestController
@RequestMapping("/api/prescripteurs")
@RequiresNavAccess({ "customer", "ventes", "nouvelle-vente", "nouvelle-prevente" })
public class PrescripteurResource {

    private final PrescripteurService prescripteurService;

    public PrescripteurResource(PrescripteurService prescripteurService) {
        this.prescripteurService = prescripteurService;
    }

    @GetMapping
    public ResponseEntity<List<PrescripteurDTO>> rechercher(@RequestParam(required = false) String q, @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(prescripteurService.rechercher(q, limite));
    }

    /** Création à la volée depuis l'écran de vente : le nom seul est exigé. */
    @PostMapping
    public ResponseEntity<PrescripteurDTO> creer(@Valid @RequestBody PrescripteurDTO dto) {
        PrescripteurDTO cree = prescripteurService.creer(dto);
        return ResponseEntity.created(URI.create("/api/prescripteurs/" + cree.id())).body(cree);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PrescripteurDTO> modifier(@PathVariable Integer id, @Valid @RequestBody PrescripteurDTO dto) {
        return ResponseEntity.ok(prescripteurService.modifier(id, dto));
    }

    @PutMapping("/{id}/actif")
    public ResponseEntity<Void> definirActif(@PathVariable Integer id, @RequestParam boolean actif) {
        prescripteurService.definirActif(id, actif);
        return ResponseEntity.noContent().build();
    }

    /** Les ordonnances de {@code id} passent sur {@code cibleId} ; la fiche {@code id} est désactivée. */
    @PostMapping("/{id}/fusion")
    public ResponseEntity<PrescripteurDTO> fusionner(@PathVariable Integer id, @RequestParam Integer cibleId) {
        return ResponseEntity.ok(prescripteurService.fusionner(id, cibleId));
    }
}

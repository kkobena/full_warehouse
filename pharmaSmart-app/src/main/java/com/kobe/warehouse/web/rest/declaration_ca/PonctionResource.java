package com.kobe.warehouse.web.rest.declaration_ca;

import com.kobe.warehouse.aop.license.RequiresFeature;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.service.declaration_ca.PonctionService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionAssietteDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionLigneDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionParamDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionParametresDTO;
import com.kobe.warehouse.service.declaration_ca.dto.PonctionSimulationDTO;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/declaration-ca/ponctions")
@RequiresFeature(Feature.CALLEBASSE)
@PreAuthorize("hasAuthority('pr-declaration-ca-ponction')")
public class PonctionResource {

    private final PonctionService ponctionService;

    public PonctionResource(PonctionService ponctionService) {
        this.ponctionService = ponctionService;
    }

    /**
     * Calcule sans rien écrire.
     *
     * <p>{@code POST} malgré l'absence d'effet de bord : les paramètres forment un objet structuré
     * qui n'a pas sa place dans une chaîne de requête.
     */
    @PostMapping("/simulation")
    public ResponseEntity<PonctionSimulationDTO> simuler(@Valid @RequestBody PonctionParamDTO param) {
        return ResponseEntity.ok(ponctionService.simuler(param));
    }

    @PostMapping
    public ResponseEntity<PonctionDTO> valider(@Valid @RequestBody PonctionParamDTO param) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ponctionService.valider(param));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<PonctionDTO> annuler(@PathVariable Integer id) {
        return ResponseEntity.ok(ponctionService.annuler(id));
    }


    /**
     * L'assiette d'une période. {@code GET} et non {@code POST} : trois scalaires, aucune écriture,
     * et un résultat que le navigateur peut mettre en cache le temps d'un aller-retour.
     */
    @GetMapping("/assiette")
    public ResponseEntity<PonctionAssietteDTO> getAssiette(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin,
        @RequestParam(required = false) BigDecimal plafondParVente
    ) {
        return ResponseEntity.ok(ponctionService.assiette(dateDebut, dateFin, plafondParVente));
    }

    @GetMapping("/parametres")
    public ResponseEntity<PonctionParametresDTO> getParametres() {
        return ResponseEntity.ok(ponctionService.getParametres());
    }

    /**
     * L'historique, éventuellement borné à une période.
     *
     * <p>Les bornes sont facultatives : sans elles, l'écran s'ouvre sur la totalité de l'historique,
     * comme avant l'ajout du filtre.
     */
    @GetMapping
    public ResponseEntity<List<PonctionDTO>> getHistorique(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin
    ) {
        return ResponseEntity.ok(ponctionService.getHistorique(dateDebut, dateFin));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> exportToPdf(@PathVariable Integer id) {
        return Utils.printPDF(ponctionService.exportToPdf(id), "justificatif_ponction.pdf");
    }

    @GetMapping("/{id}/detail")
    public ResponseEntity<List<PonctionLigneDTO>> getDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(ponctionService.getDetail(id));
    }
}

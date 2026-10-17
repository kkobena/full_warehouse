package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.domain.enumeration.StatutOrdonnance;
import com.kobe.warehouse.service.dto.ordonnance.AppariementDTO;
import com.kobe.warehouse.service.dto.ordonnance.LigneVenteRefDTO;
import com.kobe.warehouse.service.dto.ordonnance.OrdonnanceCreationDTO;
import com.kobe.warehouse.service.dto.ordonnance.OrdonnanceDTO;
import com.kobe.warehouse.service.dto.ordonnance.PrescripteurVenteDTO;
import com.kobe.warehouse.service.dto.ordonnance.VenteRefDTO;
import com.kobe.warehouse.domain.enumeration.FormatEtiquette;
import com.kobe.warehouse.domain.enumeration.ResolutionEtiquette;
import com.kobe.warehouse.service.etiquette.EtiquetteUsageService;
import com.kobe.warehouse.service.ordonnance.OrdonnanceService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

/** Suivi d'ordonnance  */
@RestController
@RequestMapping("/api/ordonnances")
@RequiresNavAccess({ "customer", "ventes", "nouvelle-vente", "nouvelle-prevente" })
public class OrdonnanceResource {

    private final OrdonnanceService ordonnanceService;
    private final EtiquetteUsageService etiquetteUsageService;

    public OrdonnanceResource(OrdonnanceService ordonnanceService, EtiquetteUsageService etiquetteUsageService) {
        this.ordonnanceService = ordonnanceService;
        this.etiquetteUsageService = etiquetteUsageService;
    }

    /**
     * {@code GET /api/ordonnances/{id}/etiquettes/posologie} : une étiquette par ligne, ou une
     * seule avec {@code ligneId}. Patient, prescripteur et instructions viennent de l'ordonnance.
     */
    @GetMapping("/{id}/etiquettes/posologie")
    public ResponseEntity<byte[]> posologie(
        @PathVariable Integer id,
        @RequestParam(required = false) Integer ligneId,
        @RequestParam(defaultValue = "1") int qty,
        @RequestParam(defaultValue = "1") int startAt,
        @RequestParam(required = false) FormatEtiquette format
    ) {
        byte[] pdf = etiquetteUsageService.imprimerPosologie(id, ligneId, qty, startAt, formatOuDefaut(format));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "posologie-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    /** La même posologie, en ZPL, pour une imprimante à rouleau pilotée en direct. */
    @GetMapping(value = "/{id}/etiquettes/posologie/zpl", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> posologieZpl(
        @PathVariable Integer id,
        @RequestParam(required = false) Integer ligneId,
        @RequestParam(defaultValue = "1") int qty,
        @RequestParam(required = false) FormatEtiquette format,
        @RequestParam(required = false) ResolutionEtiquette resolution
    ) {
        return ResponseEntity.ok()
            .body(
                etiquetteUsageService.genererZplPosologie(
                    id,
                    ligneId,
                    qty,
                    formatOuDefaut(format),
                    resolution == null ? ResolutionEtiquette.parDefaut() : resolution
                )
            );
    }

    private FormatEtiquette formatOuDefaut(FormatEtiquette format) {
        return format == null ? FormatEtiquette.parDefaut() : format;
    }

    @PostMapping
    public ResponseEntity<OrdonnanceDTO> creer(@Valid @RequestBody OrdonnanceCreationDTO dto) {
        OrdonnanceDTO cree = ordonnanceService.creer(dto);
        return ResponseEntity.created(URI.create("/api/ordonnances/" + cree.id())).body(cree);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdonnanceDTO> detail(@PathVariable Integer id) {
        return ResponseEntity.ok(ordonnanceService.detail(id));
    }

    /** Ordonnances d'un client ; {@code statut=EN_COURS} pour celles qui se proposent à la reprise. */
    @GetMapping
    public ResponseEntity<List<OrdonnanceDTO>> duClient(@RequestParam Integer customerId, @RequestParam(required = false) StatutOrdonnance statut) {
        return ResponseEntity.ok(ordonnanceService.duClient(customerId, statut));
    }

    @PutMapping("/{id}/cloture")
    public ResponseEntity<OrdonnanceDTO> cloturer(@PathVariable Integer id, @RequestParam boolean cloturee) {
        return ResponseEntity.ok(ordonnanceService.cloturer(id, cloturee));
    }

    @PostMapping("/{id}/ventes")
    public ResponseEntity<OrdonnanceDTO> rattacherVente(@PathVariable Integer id, @Valid @RequestBody VenteRefDTO vente) {
        return ResponseEntity.ok(ordonnanceService.rattacherVente(id, vente.salesId(), vente.salesDate()));
    }

    /** Rattache la vente et lie ses lignes aux lignes prescrites : même produit, ou même groupe générique. */
    @PostMapping("/{id}/ventes/apparier")
    public ResponseEntity<AppariementDTO> apparier(@PathVariable Integer id, @Valid @RequestBody VenteRefDTO vente) {
        return ResponseEntity.ok(ordonnanceService.apparierVente(id, vente.salesId(), vente.salesDate()));
    }

    /** Déclare le prescripteur d'une vente sans ordonnance (produit sur ordonnance). */
    @PostMapping("/ventes/prescripteur")
    public ResponseEntity<Void> definirPrescripteurDeVente(@Valid @RequestBody PrescripteurVenteDTO demande) {
        ordonnanceService.definirPrescripteurDeVente(demande.salesId(), demande.salesDate(), demande.prescripteurId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/ventes/{salesId}")
    public ResponseEntity<OrdonnanceDTO> detacherVente(
        @PathVariable Integer id,
        @PathVariable Long salesId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate salesDate
    ) {
        return ResponseEntity.ok(ordonnanceService.detacherVente(id, salesId, salesDate));
    }

    @PostMapping("/{id}/lignes/{ligneId}/delivrances")
    public ResponseEntity<OrdonnanceDTO> lierLigne(@PathVariable Integer id, @PathVariable Integer ligneId, @Valid @RequestBody LigneVenteRefDTO ligne) {
        return ResponseEntity.ok(ordonnanceService.lierLigneDeVente(id, ligneId, ligne.salesLineId(), ligne.salesLineDate()));
    }
}

package com.kobe.warehouse.web.rest.exports;

import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.RequiresNavAccess;
import com.kobe.warehouse.service.dto.exports.CatalogueExportsDTO;
import com.kobe.warehouse.service.dto.exports.DemandeExportDTO;
import com.kobe.warehouse.service.dto.exports.ExportFichierDTO;
import com.kobe.warehouse.service.dto.exports.ExportModeleDTO;
import com.kobe.warehouse.service.dto.exports.FichierExporteDTO;
import com.kobe.warehouse.service.exports.CatalogueExportsService;
import com.kobe.warehouse.service.exports.ExportsFichiersService;
import com.kobe.warehouse.service.exports.ModelesExportService;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Menu Exports. Le droit de chaque rubrique (données, nominatif, BI) est vérifié par le service, export par export ; ici, l'accès
 * à l'écran. Demander un export ou enregistrer un modèle ne crée rien dans les données de l'officine : l'accès suffit.
 */
@RestController
@RequestMapping("/api/exports")
@RequiresNavAccess("exports")
public class ExportsResource {

    private final CatalogueExportsService catalogueExportsService;
    private final ExportsFichiersService exportsFichiersService;
    private final ModelesExportService modelesExportService;

    public ExportsResource(
        CatalogueExportsService catalogueExportsService,
        ExportsFichiersService exportsFichiersService,
        ModelesExportService modelesExportService
    ) {
        this.catalogueExportsService = catalogueExportsService;
        this.exportsFichiersService = exportsFichiersService;
        this.modelesExportService = modelesExportService;
    }

    @GetMapping("/catalogue")
    public ResponseEntity<CatalogueExportsDTO> lireCatalogue() {
        return ResponseEntity.ok(catalogueExportsService.lireCatalogue());
    }

    @PostMapping("/fichiers")
    @RequiresNavAccess(value = "exports", action = NavAction.ACCESS)
    public ResponseEntity<ExportFichierDTO> demander(@Valid @RequestBody DemandeExportDTO demande) {
        return ResponseEntity.ok(exportsFichiersService.demander(demande));
    }

    @GetMapping("/fichiers")
    public ResponseEntity<List<ExportFichierDTO>> listerHistorique(Pageable pageable) {
        Page<ExportFichierDTO> page = exportsFichiersService.listerHistorique(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/fichiers/{id}/contenu")
    public ResponseEntity<Resource> telecharger(@PathVariable Long id) {
        FichierExporteDTO fichier = exportsFichiersService.lireFichier(id);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(fichier.typeMime()))
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fichier.nom()).build().toString())
            .body(new FileSystemResource(fichier.chemin()));
    }

    @GetMapping("/modeles")
    public ResponseEntity<List<ExportModeleDTO>> listerModeles() {
        return ResponseEntity.ok(modelesExportService.listerModeles());
    }

    @PostMapping("/modeles")
    @RequiresNavAccess(value = "exports", action = NavAction.ACCESS)
    public ResponseEntity<ExportModeleDTO> enregistrerModele(@Valid @RequestBody ExportModeleDTO modele) {
        return ResponseEntity.ok(modelesExportService.enregistrerModele(modele));
    }

    @DeleteMapping("/modeles/{id}")
    @RequiresNavAccess(value = "exports", action = NavAction.ACCESS)
    public ResponseEntity<Void> supprimerModele(@PathVariable Long id) {
        modelesExportService.supprimerModele(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/modeles/{id}/executions")
    @RequiresNavAccess(value = "exports", action = NavAction.ACCESS)
    public ResponseEntity<ExportFichierDTO> executerModele(@PathVariable Long id) {
        return ResponseEntity.ok(modelesExportService.executerModele(id));
    }
}

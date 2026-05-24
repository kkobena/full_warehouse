package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.service.dto.AvoirFournisseurCommand;
import com.kobe.warehouse.service.dto.AvoirFournisseurDTO;
import com.kobe.warehouse.service.dto.RetourBonBatchResultDTO;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonFromLotRequest;
import com.kobe.warehouse.service.dto.RetourBonFromLotsRequest;
import com.kobe.warehouse.service.dto.RetourBonLotResolutionDTO;
import com.kobe.warehouse.service.dto.RetourBonGroupeDTO;
import com.kobe.warehouse.service.dto.RetourCompletCommandeRequest;
import com.kobe.warehouse.service.pharmaml.service.PharmaMlService;
import com.kobe.warehouse.service.report.excel.RetourBonExcelCsvReportService;
import com.kobe.warehouse.service.report.pdf.RetourBonPdfReportService;
import com.kobe.warehouse.service.stock.RetourBonService;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.RetourBon}.
 */
@RestController
@RequestMapping("/api")
public class RetourBonResource {

    private static final String ENTITY_NAME = "retourBon";
    private final Logger log = LoggerFactory.getLogger(RetourBonResource.class);
    private final RetourBonService retourBonService;
    private final PharmaMlService pharmaMlService;
    private final RetourBonExcelCsvReportService retourBonExcelCsvReportService;

    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;

    public RetourBonResource(
        RetourBonService retourBonService,
        RetourBonPdfReportService retourBonPdfReportService,
        PharmaMlService pharmaMlService,
        RetourBonExcelCsvReportService retourBonExcelCsvReportService
    ) {
        this.retourBonService = retourBonService;
        this.pharmaMlService = pharmaMlService;
        this.retourBonExcelCsvReportService = retourBonExcelCsvReportService;
    }

    /**
     * {@code POST  /retour-bons} : Create a new retour bon.
     *
     * @param retourBonDTO the retourBonDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new retourBonDTO,
     * or with status {@code 400 (Bad Request)} if the retourBon has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/retour-bons")
    public ResponseEntity<RetourBonDTO> createRetourBon(@Valid @RequestBody RetourBonDTO retourBonDTO) throws URISyntaxException {
        log.debug("REST request to save RetourBon : {}", retourBonDTO);
        if (retourBonDTO.getId() != null) {
            return ResponseEntity.badRequest()
                .headers(
                    HeaderUtil.createFailureAlert(
                        applicationName,
                        true,
                        ENTITY_NAME,
                        "idexists",
                        "A new retour bon cannot already have an ID"
                    )
                )
                .body(null);
        }
        RetourBonDTO result = retourBonService.create(retourBonDTO);
        return ResponseEntity.created(new URI("/api/retour-bons/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /retour-bons} : get all the retour bons with optional filters.
     *
     * @param statut   optional status filter.
     * @param dtStart  optional start date filter.
     * @param dtEnd    optional end date filter.
     * @param search   optional text search on fournisseur/reference.
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of retour bons in body.
     */
    @GetMapping("/retour-bons")
    public ResponseEntity<List<RetourBonDTO>> getAllRetourBons(
        @RequestParam(required = false, name = "statut") RetourStatut statut,
        @RequestParam(required = false, name = "excludeStatut") RetourStatut excludeStatut,
        @RequestParam(required = false, name = "dtStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtStart,
        @RequestParam(required = false, name = "dtEnd") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtEnd,
        @RequestParam(required = false, name = "search") String search,
        Pageable pageable
    ) {
        log.debug("REST request to get a page of RetourBons: statut={}, excludeStatut={}, dtStart={}, dtEnd={}, search={}", statut, excludeStatut, dtStart, dtEnd, search);
        Page<RetourBonDTO> page = retourBonService.findAll(statut, excludeStatut, dtStart, dtEnd, search, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /retour-bons/:id/pdf} : generate a PDF for the retour bon.
     *
     * @param id the id of the retour bon.
     * @return the PDF as byte array.
     */
    @GetMapping("/retour-bons/{id}/pdf")
    public ResponseEntity<byte[]> getRetourBonPdf(@PathVariable Integer id) {
        log.debug("REST request to generate PDF for RetourBon : {}", id);

        byte[] pdf = retourBonService.export(id);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"retour-" + id + ".pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    /**
     * {@code PATCH  /retour-bons/:id/processing} : mark a retour bon as processing.
     *
     * @param id the id of the retour bon.
     * @return the updated retour bon.
     */
    @PatchMapping("/retour-bons/{id}/processing")
    public ResponseEntity<RetourBonDTO> markAsProcessing(@PathVariable Integer id) {
        log.debug("REST request to mark RetourBon as PROCESSING : {}", id);
        RetourBonDTO result = retourBonService.markAsProcessing(id);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code PUT  /retour-bons/:id} : update an existing retour bon (statut VALIDATED only).
     *
     * @param id           the id of the retour bon to update.
     * @param retourBonDTO the updated retourBonDTO.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the updated entity.
     */
    @PutMapping("/retour-bons/{id}")
    public ResponseEntity<RetourBonDTO> updateRetourBon(@PathVariable Integer id, @Valid @RequestBody RetourBonDTO retourBonDTO) {
        log.debug("REST request to update RetourBon : {}", id);
        retourBonDTO.setId(id);
        RetourBonDTO result = retourBonService.update(retourBonDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code DELETE  /retour-bons/:id} : delete a retour bon (statut VALIDATED only).
     *
     * @param id the id of the retour bon to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/retour-bons/{id}")
    public ResponseEntity<Void> deleteRetourBon(@PathVariable Integer id) {
        log.debug("REST request to delete RetourBon : {}", id);
        retourBonService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code GET  /retour-bons/:id} : get the "id" retour bon.
     *
     * @param id the id of the retourBonDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the retourBonDTO,
     * or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/retour-bons/{id}")
    public ResponseEntity<RetourBonDTO> getRetourBon(@PathVariable Integer id) {
        log.debug("REST request to get RetourBon : {}", id);
        Optional<RetourBonDTO> retourBonDTO = retourBonService.findOne(id);
        return ResponseUtil.wrapOrNotFound(retourBonDTO);
    }

    /**
     * {@code GET  /retour-bons/by-commande/:commandeId/:orderDate} : get retour bons by commande.
     *
     * @param commandeId the commande id.
     * @param orderDate  the order date.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of retour bons.
     */
    @GetMapping("/retour-bons/by-commande/{commandeId}/{orderDate}")
    public ResponseEntity<List<RetourBonDTO>> getRetourBonsByCommande(
        @PathVariable Integer commandeId,
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate orderDate
    ) {
        log.debug("REST request to get RetourBons by commande : {}, {}", commandeId, orderDate);
        List<RetourBonDTO> result = retourBonService.findAllByCommande(commandeId, orderDate);
        return ResponseEntity.ok().body(result);
    }

    /**
     * {@code POST  /retour-bons/:id/send-edi} : envoie un bon de retour via EDI PharmaML.
     *
     * @param id the id of the retour bon.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/retour-bons/{id}/send-edi")
    public ResponseEntity<Void> sendEdi(@PathVariable Integer id) {
        log.debug("REST request to send RetourBon via EDI PharmaML : {}", id);
        pharmaMlService.envoiRetourBon(id);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    /**
     * {@code GET /retour-bons/resolution-lot} : pré-résout un lot pour déterminer
     * l'état initial du formulaire "Retour fournisseur".
     *
     * @param lotId l'identifiant du lot.
     * @return le DTO de résolution (COMMANDE_TROUVEE / HORS_COMMANDE_UN_FOURN / HORS_COMMANDE_MULTI / FOURNISSEUR_INCONNU).
     */
    @GetMapping("/retour-bons/resolution-lot")
    public ResponseEntity<RetourBonLotResolutionDTO> resolveLot(@RequestParam Integer lotId) {
        log.debug("REST request to resolve lot {} for retour fournisseur", lotId);
        return ResponseEntity.ok(retourBonService.resolveLot(lotId));
    }

    /**
     * {@code POST /retour-bons/from-expired-lots} : Crée plusieurs RetourBon depuis une liste de lots périmés (batch).
     * Traitement "best-effort" : retourne les succès ET les erreurs.
     *
     * @param request la requête batch.
     * @return le {@link ResponseEntity} avec statut {@code 200 (OK)} et le résultat batch.
     */
    @PostMapping("/retour-bons/from-expired-lots")
    public ResponseEntity<RetourBonBatchResultDTO> createFromExpiredLots(@Valid @RequestBody RetourBonFromLotsRequest request) {
        log.debug("REST request to create RetourBons from expired lots batch: {} lots", request.getLots().size());
        RetourBonBatchResultDTO result = retourBonService.createFromExpiredLots(request);
        return ResponseEntity.ok().body(result);
    }

    /**
     * {@code POST /retour-bons/from-expired-lot} : Crée un RetourBon depuis un lot périmé.
     * Résout automatiquement la Commande source via Lot → OrderLine → Commande.
     *
     * @param request la requête contenant lotId, motifRetourId et quantity.
     * @return le {@link ResponseEntity} avec statut {@code 201 (Created)} et le RetourBonDTO créé.
     * @throws URISyntaxException si la syntaxe de l'URI de localisation est incorrecte.
     */
    @PostMapping("/retour-bons/from-expired-lot")
    public ResponseEntity<RetourBonDTO> createFromExpiredLot(@Valid @RequestBody RetourBonFromLotRequest request)
        throws URISyntaxException {
        log.debug("REST request to create RetourBon from expired lot: {}", request);
        RetourBonDTO result = retourBonService.createFromExpiredLot(request);
        return ResponseEntity.created(new URI("/api/retour-bons/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET /retour-bons/export} : export Excel ou CSV des bons de retour.
     *
     * @param format  "excel" ou "csv"
     * @param statut  filtre optionnel sur le statut
     * @param dtStart filtre date début
     * @param dtEnd   filtre date fin
     * @param search  recherche textuelle
     * @return le fichier en bytes.
     */
    @GetMapping("/retour-bons/export")
    public ResponseEntity<byte[]> exportRetourBons(
        @RequestParam(defaultValue = "excel") String format,
        @RequestParam(required = false) RetourStatut statut,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtStart,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dtEnd,
        @RequestParam(required = false) String search
    ) {
        log.debug("REST request to export RetourBons: format={}, statut={}, dtStart={}, dtEnd={}", format, statut, dtStart, dtEnd);
        try {
            if ("csv".equalsIgnoreCase(format)) {
                byte[] csv = retourBonExcelCsvReportService.exportToCsv(statut, dtStart, dtEnd, search);
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"retours-fournisseur.csv\"")
                    .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                    .body(csv);
            } else {
                byte[] excel = retourBonExcelCsvReportService.exportToExcel(statut, dtStart, dtEnd, search);
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"retours-fournisseur.xlsx\"")
                    .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(excel);
            }
        } catch (Exception e) {
            log.error("Error exporting RetourBons", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * {@code PATCH  /retour-bons/:id/close-manually} : clôture manuellement un retour partiellement accepté.
     */
    @PatchMapping("/retour-bons/{id}/close-manually")
    public ResponseEntity<RetourBonDTO> closeManually(@PathVariable Integer id) {
        log.debug("REST request to close manually RetourBon : {}", id);
        RetourBonDTO result = retourBonService.closeManually(id);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .body(result);
    }

    /**
     * {@code POST  /retour-bons/retour-complet} : Crée un RetourBon couvrant toutes les lignes d'une commande.
     */
    @PostMapping("/retour-bons/retour-complet")
    public ResponseEntity<RetourBonDTO> createRetourComplet(@Valid @RequestBody RetourCompletCommandeRequest request)
        throws URISyntaxException {
        log.debug("REST request to create retour complet from commande: {}/{}", request.getCommandeId(), request.getCommandeOrderDate());
        RetourBonDTO result = retourBonService.createRetourCompletFromCommande(request);
        return ResponseEntity.created(new URI("/api/retour-bons/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /retour-bons/count-en-attente} : nombre de retours en attente (statut ≠ CLOSED).
     */
    @GetMapping("/retour-bons/count-en-attente")
    public ResponseEntity<Long> countEnAttente() {
        return ResponseEntity.ok(retourBonService.countEnAttente());
    }

    /**
     * {@code GET  /retour-bons/grouped-by-fournisseur} : get all open retour bons grouped by fournisseur.
     */
    @GetMapping("/retour-bons/grouped-by-fournisseur")
    public ResponseEntity<List<RetourBonGroupeDTO>> getAllGroupedByFournisseur() {
        log.debug("REST request to get RetourBons grouped by fournisseur");
        return ResponseEntity.ok(retourBonService.findAllGroupedByFournisseur());
    }

    /**
     * {@code POST  /retour-bons/export-groupe} : generate a grouped PDF for the given retour bons.
     */
    @PostMapping("/retour-bons/export-groupe")
    public ResponseEntity<byte[]> exportGroupe(@RequestBody List<Integer> ids) {
        log.debug("REST request to export groupe PDF for RetourBons : {}", ids);
        byte[] pdf = retourBonService.exportGroupe(ids);
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"bordereau-groupe-retours.pdf\"")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
    }

    /**
     * {@code POST  /retour-bons/supplier-response} : Create an avoir fournisseur from a supplier response.
     *
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/retour-bons/supplier-response")
    public ResponseEntity<AvoirFournisseurDTO> createSupplierResponse(@RequestBody AvoirFournisseurCommand command)
        throws URISyntaxException {
        log.debug("REST request to create avoir fournisseur from retour bon : {}", command.retourBonId());
        AvoirFournisseurDTO result = retourBonService.createSupplierResponse(command);
        return ResponseEntity.created(new URI("/api/avoirs-fournisseur/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, "avoirFournisseur", result.getId().toString()))
            .body(result);
    }
}

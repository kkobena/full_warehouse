package com.kobe.warehouse.web.rest.facturation;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.facturation.dto.DossierFactureDto;
import com.kobe.warehouse.service.facturation.dto.DossierFactureProjection;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FacturationDossier;
import com.kobe.warehouse.service.facturation.dto.FacturationGroupeDossier;
import com.kobe.warehouse.service.facturation.dto.FactureDto;
import com.kobe.warehouse.service.facturation.dto.FactureDtoWrapper;
import com.kobe.warehouse.service.facturation.dto.FactureEditionResponse;
import com.kobe.warehouse.service.facturation.dto.InvoiceSearchParams;
import com.kobe.warehouse.service.facturation.dto.ModeEditionEnum;
import com.kobe.warehouse.service.facturation.dto.ModeEditionSort;
import com.kobe.warehouse.service.facturation.dto.TiersPayantDossierFactureDto;
import com.kobe.warehouse.service.facturation.registry.FacturationServiceRegistry;
import com.kobe.warehouse.service.facturation.service.EditionDataService;
import com.kobe.warehouse.web.rest.Utils;
import com.kobe.warehouse.web.util.PaginationUtil;
import com.kobe.warehouse.web.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class EditionFactureResource {

    private final EditionDataService editionService;
    private final FacturationServiceRegistry facturationServiceRegistry;

    public EditionFactureResource(EditionDataService editionService, FacturationServiceRegistry facturationServiceRegistry) {
        this.editionService = editionService;
        this.facturationServiceRegistry = facturationServiceRegistry;
    }

    @GetMapping("/edition-factures/data")
    public ResponseEntity<List<TiersPayantDossierFactureDto>> getEditionData(
        @RequestParam(name = "sort", required = false) ModeEditionSort sort,
        @RequestParam(name = "startDate") LocalDate startDate,
        @RequestParam(name = "endDate") LocalDate endDate,
        @RequestParam(name = "modeEdition", required = false, defaultValue = "ALL") ModeEditionEnum modeEdition,
        @RequestParam(name = "groupIds", required = false) Set<Integer> groupIds,
        @RequestParam(name = "tiersPayantIds", required = false) Set<Integer> tiersPayantIds,
        @RequestParam(name = "categorieTiersPayants", required = false) Set<TiersPayantCategorie> categorieTiersPayants,
        @RequestParam(name = "all", required = false, defaultValue = "false") Boolean all,
        @RequestParam(name = "factureProvisoire", required = false, defaultValue = "false") Boolean factureProvisoire,
        Pageable pageable
    ) {
        Page<TiersPayantDossierFactureDto> page = editionService.getEditionData(
            new EditionSearchParams(
                sort,
                modeEdition,
                startDate,
                endDate,
                groupIds,
                tiersPayantIds,
                Set.of(),
                all,
                categorieTiersPayants,
                factureProvisoire
            ),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/edition-factures/bons")
    public ResponseEntity<List<DossierFactureDto>> getEditionBon(
        @RequestParam(name = "sort", required = false) ModeEditionSort sort,
        @RequestParam(name = "startDate") LocalDate startDate,
        @RequestParam(name = "endDate") LocalDate endDate,
        @RequestParam(name = "modeEdition", required = false) ModeEditionEnum modeEdition,
        @RequestParam(name = "groupIds", required = false) Set<Integer> groupIds,
        @RequestParam(name = "tiersPayantIds", required = false) Set<Integer> tiersPayantIds,
        @RequestParam(name = "categorieTiersPayants", required = false) Set<TiersPayantCategorie> categorieTiersPayants,
        @RequestParam(name = "all", required = false, defaultValue = "false") Boolean all,
        @RequestParam(name = "factureProvisoire", required = false, defaultValue = "false") Boolean factureProvisoire,
        Pageable pageable
    ) {
        Page<DossierFactureDto> page = editionService.getSales(
            new EditionSearchParams(
                sort,
                modeEdition,
                startDate,
                endDate,
                groupIds,
                tiersPayantIds,
                Set.of(),
                all,
                categorieTiersPayants,
                factureProvisoire
            ),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @PostMapping("/edition-factures/edit")
    public ResponseEntity<FactureEditionResponse> createFactureEdition(@Valid @RequestBody EditionSearchParams editionSearchParams) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            facturationServiceRegistry.getService(editionSearchParams.modeEdition()).createFactureEdition(editionSearchParams)
        );
    }

    @GetMapping("/edition-factures/print-all")
    public ResponseEntity<Void> printAllInvoices(
        @RequestParam(name = "sort", required = false) ModeEditionSort sort,
        @RequestParam(name = "createdate") LocalDateTime startDate,
        @RequestParam(name = "invoicesIds", required = false) Set<Long> invoicesIds,
        @RequestParam(name = "factureProvisoire", required = false, defaultValue = "false") Boolean factureProvisoire
    ) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/edition-factures/delete")
    public ResponseEntity<Void> deleteInvoices(@RequestParam(name = "invoicesIds") Set<FactureItemId> invoicesIds) {
        editionService.deleteFacture(invoicesIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/edition-factures")
    public ResponseEntity<List<FactureDto>> getInvoicies(
        @RequestParam(name = "startDate", required = false) LocalDate startDate,
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "endDate", required = false) LocalDate endDate,
        @RequestParam(name = "statuts", required = false) Set<InvoiceStatut> statuts,
        @RequestParam(name = "tiersPayantIds", required = false) Set<Integer> tiersPayantIds,
        @RequestParam(name = "factureProvisoire", required = false, defaultValue = "false") Boolean factureProvisoire,
        Pageable pageable
    ) {
        Page<FactureDto> page = editionService.getInvoicies(
            new InvoiceSearchParams(startDate, endDate, Set.of(), tiersPayantIds, factureProvisoire, statuts, search),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/edition-factures/groupes")
    public ResponseEntity<List<FactureDto>> getGroupInvoicies(
        @RequestParam(name = "startDate", required = false) LocalDate startDate,
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "endDate", required = false) LocalDate endDate,
        @RequestParam(name = "statuts", required = false) Set<InvoiceStatut> statuts,
        @RequestParam(name = "groupIds", required = false) Set<Integer> groupIds,
        @RequestParam(name = "factureProvisoire", required = false, defaultValue = "false") Boolean factureProvisoire,
        Pageable pageable
    ) {
        Page<FactureDto> page = editionService.getGroupInvoicies(
            new InvoiceSearchParams(startDate, endDate, groupIds, Set.of(), factureProvisoire, statuts, search),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/edition-factures/pdf/{id}/{invoiceDate}")
    public ResponseEntity<Resource> exportToPdf(HttpServletRequest request, @PathVariable Long id, @PathVariable LocalDate invoiceDate) {
        return Utils.printPDF(editionService.printToPdf(new FactureItemId(id, invoiceDate)), request);
    }

    @GetMapping("/edition-factures/pdf")
    public ResponseEntity<Resource> exportAllInvoices(
        HttpServletRequest request,
        @RequestParam(name = "generationCode") Integer generationCode,
        @RequestParam(name = "isGroup", required = false, defaultValue = "false") Boolean isGroup
    ) {
        return Utils.printPDF(editionService.printToPdf(new FactureEditionResponse(generationCode, isGroup)), request);
    }

    @GetMapping("/edition-factures/{id}/{invoiceDate}")
    public ResponseEntity<FactureDtoWrapper> getone(@PathVariable Long id, @PathVariable LocalDate invoiceDate) {
        return ResponseUtil.wrapOrNotFound(editionService.getFacture(new FactureItemId(id, invoiceDate)));
    }

    @DeleteMapping("/edition-factures/{id}/{invoiceDate}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @PathVariable LocalDate invoiceDate) {
        editionService.deleteFacture(new FactureItemId(id, invoiceDate));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/edition-factures/reglement/groupes/{id}/{invoiceDate}")
    public ResponseEntity<List<FacturationGroupeDossier>> getGroupReglementData(
        @PathVariable Long id,
        @PathVariable LocalDate invoiceDate,
        Pageable pageable
    ) {
        Page<FacturationGroupeDossier> page = editionService.findGroupeFactureReglementData(new FactureItemId(id, invoiceDate), pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/edition-factures/reglement/single/{id}/{invoiceDate}")
    public ResponseEntity<List<FacturationDossier>> getReglementData(
        @PathVariable Long id,
        @PathVariable LocalDate invoiceDate,
        Pageable pageable
    ) {
        Page<FacturationDossier> page = editionService.findFactureReglementData(new FactureItemId(id, invoiceDate), pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/edition-factures/reglement/{id}/{invoiceDate}")
    public ResponseEntity<DossierFactureProjection> findDossierFacture(
        @PathVariable Long id,
        @PathVariable LocalDate invoiceDate,
        @RequestParam(name = "isGroup", required = false, defaultValue = "false") Boolean isGroup
    ) {
        return ResponseEntity.ok().body(editionService.findDossierFacture(new FactureItemId(id, invoiceDate), isGroup));
    }
}

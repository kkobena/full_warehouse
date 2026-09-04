package com.kobe.warehouse.web.rest.facturation;

import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.OrigineGeneration;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.dto.enumeration.TypeFacture;
import com.kobe.warehouse.service.facturation.dto.DossierFactureDto;
import com.kobe.warehouse.service.facturation.dto.DossierFactureProjection;
import com.kobe.warehouse.service.facturation.dto.EditionSearchParams;
import com.kobe.warehouse.service.facturation.dto.FacturationDossier;
import com.kobe.warehouse.service.facturation.dto.FacturationGroupeDossier;
import com.kobe.warehouse.service.facturation.dto.FacturationKpiDto;
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
import jakarta.validation.Valid;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

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
                factureProvisoire,
                OrigineGeneration.MANUELLE
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
                factureProvisoire,
                OrigineGeneration.MANUELLE
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



    @DeleteMapping("/edition-factures/delete")
    public ResponseEntity<Void> deleteInvoices(@RequestParam(name = "invoicesIds") Set<FactureItemId> invoicesIds) {
        editionService.deleteFacture(invoicesIds);
        return ResponseEntity.ok().build();
    }

    /**
     * Recherche des factures tiers-payant.
     *
     * <p>{@code typeFacture} remplace l'ancien couple « un endpoint par type » : {@link
     * TypeFacture#GROUPED} renvoie les factures groupées parentes, toute autre valeur les factures
     * individuelles. {@link TypeFacture#ALL} n'a de sens que pour les indicateurs — les deux
     * familles n'ayant ni les mêmes jointures ni les mêmes colonnes, aucune requête ne les réunit —
     * et retombe donc ici sur les factures individuelles.
     *
     * <p>{@code factureProvisoire} est volontairement sans valeur par défaut : absent, il ne filtre
     * pas, ce qui donne le « Toutes » du sélecteur de l'écran.
     */
    @GetMapping("/edition-factures")
    public ResponseEntity<List<FactureDto>> getInvoicies(
        @RequestParam(name = "startDate", required = false) LocalDate startDate,
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "endDate", required = false) LocalDate endDate,
        @RequestParam(name = "statuts", required = false) Set<InvoiceStatut> statuts,
        @RequestParam(name = "tiersPayantIds", required = false) Set<Integer> tiersPayantIds,
        @RequestParam(name = "groupIds", required = false) Set<Integer> groupIds,
        @RequestParam(name = "factureProvisoire", required = false) Boolean factureProvisoire,
        @RequestParam(name = "typeFacture", required = false, defaultValue = "INDIVIDUAL") TypeFacture typeFacture,
        Pageable pageable
    ) {
        return searchInvoices(
            new InvoiceSearchParams(
                startDate,
                endDate,
                nullSafe(groupIds),
                nullSafe(tiersPayantIds),
                factureProvisoire,
                statuts,
                search,
                typeFacture
            ),
            pageable
        );
    }

    /**
     * Indicateurs de facturation.
     *
     * <p>{@code factureProvisoire} obéit à la même règle que la recherche : absent, il ne filtre
     * pas. C'est ce qui permet à la bannière de couvrir exactement le périmètre de la liste, y
     * compris quand l'écran demande les provisoires.
     */
    @GetMapping("/edition-factures/kpi")
    public ResponseEntity<FacturationKpiDto> getKpi(
        @RequestParam(name = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(name = "toDate", required = false) LocalDate toDate,
        @RequestParam(name = "organismeId", required = false) Integer organismeId,
        @RequestParam(name = "groupeId", required = false) Integer groupeId,
        @RequestParam(name = "typeFacture", required = false, defaultValue = "INDIVIDUAL") TypeFacture typeFacture,
        @RequestParam(name = "factureProvisoire", required = false) Boolean factureProvisoire
    ) {
        LocalDate effectiveFrom = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate effectiveTo = toDate != null ? toDate : LocalDate.now();
        return ResponseEntity.ok(
            editionService.getKpi(effectiveFrom, effectiveTo, organismeId, groupeId, typeFacture, factureProvisoire)
        );
    }

    /**
     * @deprecated conservé pour les appelants historiques ;
     * {@code GET /edition-factures?typeFacture=GROUPED} fait la même chose.
     */
    @Deprecated
    @GetMapping("/edition-factures/groupes")
    public ResponseEntity<List<FactureDto>> getGroupInvoicies(
        @RequestParam(name = "startDate", required = false) LocalDate startDate,
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "endDate", required = false) LocalDate endDate,
        @RequestParam(name = "statuts", required = false) Set<InvoiceStatut> statuts,
        @RequestParam(name = "groupIds", required = false) Set<Integer> groupIds,
        @RequestParam(name = "factureProvisoire", required = false) Boolean factureProvisoire,
        Pageable pageable
    ) {
        return searchInvoices(
            new InvoiceSearchParams(
                startDate,
                endDate,
                nullSafe(groupIds),
                Set.of(),
                factureProvisoire,
                statuts,
                search,
                TypeFacture.GROUPED
            ),
            pageable
        );
    }

    @GetMapping("/edition-factures/pdf/{id}/{invoiceDate}")
    public ResponseEntity<byte[]> exportToPdf(@PathVariable Long id, @PathVariable LocalDate invoiceDate) {
        String fileName = "facture_" + id + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss")) + ".pdf";
        return Utils.printPDF(editionService.printToPdf(new FactureItemId(id, invoiceDate)), fileName);
    }

    @GetMapping("/edition-factures/pdf")
    public ResponseEntity<byte[]> exportAllInvoices(
        @RequestParam(name = "generationCode") Integer generationCode,
        @RequestParam(name = "isGroup", required = false, defaultValue = "false") Boolean isGroup
    ) {
        String fileName = "factures_" + generationCode + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss")) + ".pdf";
        return Utils.printPDF(editionService.printToPdf(new FactureEditionResponse(generationCode, isGroup)), fileName);
    }

    @GetMapping("/edition-factures/{id}/{invoiceDate}")
    public ResponseEntity<FactureDtoWrapper> getOne(@PathVariable Long id, @PathVariable LocalDate invoiceDate) {
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


    @GetMapping("/edition-factures/export")
    public ResponseEntity<byte[]> exportInvoicesToExcel(
        @RequestParam(name = "startDate", required = false) LocalDate startDate,
        @RequestParam(name = "endDate", required = false) LocalDate endDate,
        @RequestParam(name = "statuts", required = false) Set<InvoiceStatut> statuts,
        @RequestParam(name = "tiersPayantIds", required = false) Set<Integer> tiersPayantIds,
        @RequestParam(name = "groupIds", required = false) Set<Integer> groupIds,
        @RequestParam(name = "factureProvisoire", required = false) Boolean factureProvisoire,
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "typeFacture", required = false, defaultValue = "INDIVIDUAL") TypeFacture typeFacture
    ) {
        return exportInvoices(
            new InvoiceSearchParams(
                startDate != null ? startDate : LocalDate.now().withDayOfMonth(1),
                endDate != null ? endDate : LocalDate.now(),
                nullSafe(groupIds),
                nullSafe(tiersPayantIds),
                factureProvisoire,
                statuts,
                search,
                typeFacture
            )
        );
    }

    /**
     * @deprecated conservé pour les appelants historiques ;
     * {@code GET /edition-factures/export?typeFacture=GROUPED} fait la même chose.
     */
    @Deprecated
    @GetMapping("/edition-factures/groupes/export")
    public ResponseEntity<byte[]> exportGroupInvoicesToExcel(
        @RequestParam(name = "startDate", required = false) LocalDate startDate,
        @RequestParam(name = "endDate", required = false) LocalDate endDate,
        @RequestParam(name = "statuts", required = false) Set<InvoiceStatut> statuts,
        @RequestParam(name = "groupIds", required = false) Set<Integer> groupIds,
        @RequestParam(name = "factureProvisoire", required = false) Boolean factureProvisoire,
        @RequestParam(name = "search", required = false) String search
    ) {
        return exportInvoices(
            new InvoiceSearchParams(
                startDate != null ? startDate : LocalDate.now().withDayOfMonth(1),
                endDate != null ? endDate : LocalDate.now(),
                nullSafe(groupIds),
                Set.of(),
                factureProvisoire,
                statuts,
                search,
                TypeFacture.GROUPED
            )
        );
    }

    private ResponseEntity<List<FactureDto>> searchInvoices(InvoiceSearchParams params, Pageable pageable) {
        Page<FactureDto> page = isGroup(params)
            ? editionService.getGroupInvoicies(params, pageable)
            : editionService.getInvoicies(params, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    private ResponseEntity<byte[]> exportInvoices(InvoiceSearchParams params) {
        boolean group = isGroup(params);
        String fileName =
            (group ? "factures_groupes_" : "factures_") +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm")) +
            ".xlsx";
        return Utils.exportExcel(editionService.exportInvoicesToExcel(params, group), fileName);
    }

    private static boolean isGroup(InvoiceSearchParams params) {
        return params.typeFacture() == TypeFacture.GROUPED;
    }

    private static Set<Integer> nullSafe(Set<Integer> ids) {
        return ids != null ? ids : Set.of();
    }
}

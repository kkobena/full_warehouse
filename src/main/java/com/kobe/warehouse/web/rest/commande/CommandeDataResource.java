package com.kobe.warehouse.web.rest.commande;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.CommandeEntryDTO;
import com.kobe.warehouse.service.dto.CommandeLiteDTO;
import com.kobe.warehouse.service.dto.FilterCommaneEnCours;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import com.kobe.warehouse.service.dto.Sort;
import com.kobe.warehouse.service.dto.filter.CommandeFilterDTO;
import com.kobe.warehouse.service.stock.CommandeDataService;
import com.kobe.warehouse.web.rest.Utils;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/** REST controller for managing {@link Commande}. */
@RestController
@RequestMapping("/api")
public class CommandeDataResource {

    private final Logger log = LoggerFactory.getLogger(CommandeDataResource.class);
    private final CommandeDataService commandeDataService;

    public CommandeDataResource(CommandeDataService commandeDataService) {
        this.commandeDataService = commandeDataService;
    }

    @GetMapping("/commandes/commandes-without-order-lines")
    public ResponseEntity<List<CommandeLiteDTO>> getAllCommandes(
        @RequestParam(required = false, name = "typeSuggession") String typeSuggession,
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(required = false, name = "searchCommande") String searchCommande,
        @RequestParam(required = false, name = "orderStatuts") Set<OrderStatut> orderStatuts,
        Pageable pageable
    ) {
        Page<CommandeLiteDTO> page = commandeDataService.fetchCommandes(
            new CommandeFilterDTO()
                .setTypeSuggession(typeSuggession)
                .setOrderStatuts(orderStatuts)
                .setSearch(search)
                .setSearchCommande(searchCommande),
            pageable
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/commandes/{id}")
    public ResponseEntity<CommandeDTO> getCommande(@PathVariable Long id) {
        Optional<CommandeDTO> commande = Optional.ofNullable(commandeDataService.findOneById(id));
        return ResponseUtil.wrapOrNotFound(commande);
    }

    @GetMapping("/commandes/filter-order-lines")
    public ResponseEntity<List<OrderLineDTO>> filterCommandeLines(
        @RequestParam(name = "commandeId") Long commandeId,
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(required = false, name = "searchCommande") String searchCommande,
        @RequestParam(required = false, name = "orderStatuts") Set<OrderStatut> orderStatuts,
        @RequestParam(required = false, name = "orderBy") Sort orderBy,
        @RequestParam(required = false, name = "filterCommaneEnCours") FilterCommaneEnCours filterCommaneEnCours
    ) {
        return ResponseEntity.ok(
            commandeDataService.filterCommandeLines(
                new CommandeFilterDTO()
                    .setCommandeId(commandeId)
                    .setOrderStatuts(orderStatuts)
                    .setSearchCommande(searchCommande)
                    .setSearch(search)
                    .setOrderBy(orderBy)
                    .setFilterCommaneEnCours(filterCommaneEnCours)
            )
        );
    }

    @GetMapping("/commandes/csv/{id}")
    public ResponseEntity<Resource> getCsv(@PathVariable Long id, HttpServletRequest request) throws IOException {
        final Resource resource = commandeDataService.exportCommandeToCsv(id);
        return Utils.exportCsv(resource, request);
    }

    @GetMapping("/commandes/pdf/{id}")
    public ResponseEntity<Resource> getPdf(@PathVariable Long id, HttpServletRequest request) throws IOException {
        final Resource resource = commandeDataService.exportCommandeToPdf(id);
        return Utils.printPDF(resource, request);
    }

    @GetMapping("/commandes/pageable-order-lines/{id}")
    public ResponseEntity<List<OrderLineDTO>> getOrderLinesByCommandeId(@PathVariable Long id, Pageable pageable) {
        Page<OrderLineDTO> page = commandeDataService.filterCommandeLines(id, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/commandes/rupture-csv/{reference}")
    public ResponseEntity<Resource> getRuptureCsv(@PathVariable("reference") String reference, HttpServletRequest request) {
        final Resource resource = commandeDataService.getRuptureCsv(reference);
        return Utils.exportCsv(resource, request);
    }

    @GetMapping("/commandes/entree-stock/{id}")
    public ResponseEntity<CommandeEntryDTO> getCommandeEntreeStock(@PathVariable Long id) {
        log.debug("REST request to get Commande : {}", id);
        return ResponseUtil.wrapOrNotFound(commandeDataService.getCommandeById(id));
    }
}

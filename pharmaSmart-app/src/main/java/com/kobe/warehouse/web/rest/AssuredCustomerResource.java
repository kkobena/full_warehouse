package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.web.util.HeaderUtil;
import com.kobe.warehouse.web.util.PaginationUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AssuredCustomerResource  {
    private final AssuredCustomerService assuredCustomerService;
    private final CustomerDataService customerDataService;
    @Value("${pharma-smart.clientApp.name}")
    private String applicationName;
    private static final String ENTITY_NAME = "customer";
    public AssuredCustomerResource(AssuredCustomerService assuredCustomerService, CustomerDataService customerDataService) {
        this.assuredCustomerService = assuredCustomerService;
        this.customerDataService = customerDataService;
    }


    @PostMapping("/customers/assured")
    public ResponseEntity<AssuredCustomerDTO> createCustomer(@Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {

        if (customer.getId() != null) {
            throw new BadRequestAlertException("A new customer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappEntityToDto(assuredCustomerService.createFromDto(customer));
        return ResponseEntity.created(new URI("/api/customers/assured/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/customers/assured")
    public ResponseEntity<AssuredCustomerDTO> updateCustomer(@Valid @RequestBody AssuredCustomerDTO customer) {

        if (customer.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappEntityToDto(assuredCustomerService.updateFromDto(customer));
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, customer.getId().toString()))
            .body(result);
    }

    @PostMapping("/customers/ayant-droit")
    public ResponseEntity<AssuredCustomerDTO> createAyantDroit(@Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {

        if (customer.getId() != null) {
            throw new BadRequestAlertException("A new customer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappAyantDroitEntityToDto(
            assuredCustomerService.createAyantDroitFromDto(customer)
        );
        return ResponseEntity.created(new URI("/api/customers/ayant-droit/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/customers/ayant-droit")
    public ResponseEntity<AssuredCustomerDTO> updateAyantDroit(@Valid @RequestBody AssuredCustomerDTO customer) {

        if (customer.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappAyantDroitEntityToDto(
            assuredCustomerService.updateAyantDroitFromDto(customer)
        );
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, customer.getId().toString()))
            .body(result);
    }

    @DeleteMapping("/customers/assured/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {

        assuredCustomerService.deleteCustomerById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/customers/assured/tiers-payants")
    public ResponseEntity<AssuredCustomerDTO> addTiersPayant(@Valid @RequestBody ClientTiersPayantDTO clientTiersPayant)
        throws URISyntaxException {

        if (clientTiersPayant.getId() != null) {
            throw new BadRequestAlertException("A new customer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappEntityToDto(assuredCustomerService.addTiersPayant(clientTiersPayant));

        return ResponseEntity.created(new URI("/api/customers/assured/tiers-payants/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/customers/assured/tiers-payants")
    public ResponseEntity<AssuredCustomerDTO> updateTiersPayant(@Valid @RequestBody ClientTiersPayantDTO clientTiersPayant) {

        if (clientTiersPayant.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappEntityToDto(assuredCustomerService.updateTiersPayant(clientTiersPayant));

        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, clientTiersPayant.getId().toString()))
            .body(result);
    }

    @DeleteMapping("/customers/assured/tiers-payants/{id}")
    public ResponseEntity<Void> deleteTiersPayant(@PathVariable Integer id) {

        assuredCustomerService.deleteTiersPayant(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @GetMapping("/customers/assured")
    public ResponseEntity<List<AssuredCustomerDTO>> getAllAssuredCustomers(
        @RequestParam(value = "search", required = false) String search,
        @RequestParam(value = "typeTiersPayant", required = false) TiersPayantCategorie typeTiersPayant,
        Pageable pageable
    ) {
        Page<AssuredCustomerDTO> page = assuredCustomerService.fetch(search, typeTiersPayant, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
    @GetMapping("/customers/tiers-payants/{id}")
    public ResponseEntity<List<ClientTiersPayantDTO>> getAssuredTiersPayants(@PathVariable("id") Integer id) {
        return ResponseEntity.ok().body(customerDataService.fetchCustomersTiersPayant(id));
    }

    @GetMapping("/customers/ayant-droits/{id}")
    public ResponseEntity<List<AssuredCustomerDTO>> getAyantDroits(@PathVariable("id") Integer id) {
        return ResponseEntity.ok().body(customerDataService.fetchAyantDroit(id));
    }
}

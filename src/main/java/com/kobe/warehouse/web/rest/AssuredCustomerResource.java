package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.HeaderUtil;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api")
public class AssuredCustomerResource {

    private static final String ENTITY_NAME = "customer";
    private final Logger log = LoggerFactory.getLogger(AssuredCustomerResource.class);
    private final AssuredCustomerService assuredCustomerService;
    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public AssuredCustomerResource(AssuredCustomerService assuredCustomerService) {
        this.assuredCustomerService = assuredCustomerService;
    }

    @PostMapping("/customers/assured")
    public ResponseEntity<AssuredCustomerDTO> createCustomer(@Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {
        log.debug("REST request to save Customer : {}", customer);
        if (customer.getId() != null) {
            throw new BadRequestAlertException("A new customer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappEntityToDto(assuredCustomerService.createFromDto(customer));
        return ResponseEntity
            .created(new URI("/api/customers/assured/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/customers/assured")
    public ResponseEntity<AssuredCustomerDTO> updateCustomer(@Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {
        log.debug("REST request to update Customer : {}", customer);
        if (customer.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappEntityToDto(assuredCustomerService.updateFromDto(customer));
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, customer.getId().toString()))
            .body(result);
    }

    @PostMapping("/customers/ayant-droit")
    public ResponseEntity<AssuredCustomerDTO> createAyantDroit(@Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {
        log.debug("REST request to save Customer : {}", customer);
        if (customer.getId() != null) {
            throw new BadRequestAlertException("A new customer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappAyantDroitEntityToDto(
            assuredCustomerService.createAyantDroitFromDto(customer)
        );
        return ResponseEntity
            .created(new URI("/api/customers/ayant-droit/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/customers/ayant-droit")
    public ResponseEntity<AssuredCustomerDTO> updateAyantDroit(@Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {
        log.debug("REST request to update Customer : {}", customer);
        if (customer.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        AssuredCustomerDTO result = assuredCustomerService.mappAyantDroitEntityToDto(
            assuredCustomerService.updateAyantDroitFromDto(customer)
        );
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, customer.getId().toString()))
            .body(result);
    }

    @DeleteMapping("/customers/assured/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.debug("REST request to delete Customer : {}", id);
        assuredCustomerService.deleteCustomerById(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}

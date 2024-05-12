package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import java.net.URI;
import java.net.URISyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import tech.jhipster.web.util.HeaderUtil;

public class AssuredCustomerResourceProxy {

  private static final String ENTITY_NAME = "customer";
  private final Logger log = LoggerFactory.getLogger(AssuredCustomerResourceProxy.class);
  private final AssuredCustomerService assuredCustomerService;

  @Value("${jhipster.clientApp.name}")
  private String applicationName;

  public AssuredCustomerResourceProxy(AssuredCustomerService assuredCustomerService) {
    this.assuredCustomerService = assuredCustomerService;
  }

  public ResponseEntity<AssuredCustomerDTO> createCustomer(AssuredCustomerDTO customer)
      throws URISyntaxException {
    log.debug("REST request to save Customer : {}", customer);
    if (customer.getId() != null) {
      throw new BadRequestAlertException(
          "A new customer cannot already have an ID", ENTITY_NAME, "idexists");
    }
    AssuredCustomerDTO result =
        assuredCustomerService.mappEntityToDto(assuredCustomerService.createFromDto(customer));
    return ResponseEntity.created(new URI("/api/customers/assured/" + result.getId()))
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  public ResponseEntity<AssuredCustomerDTO> updateCustomer(AssuredCustomerDTO customer)
      throws URISyntaxException {
    log.debug("REST request to update Customer : {}", customer);
    if (customer.getId() == null) {
      throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
    }
    AssuredCustomerDTO result =
        assuredCustomerService.mappEntityToDto(assuredCustomerService.updateFromDto(customer));
    return ResponseEntity.ok()
        .headers(
            HeaderUtil.createEntityUpdateAlert(
                applicationName, true, ENTITY_NAME, customer.getId().toString()))
        .body(result);
  }

  public ResponseEntity<AssuredCustomerDTO> createAyantDroit(AssuredCustomerDTO customer)
      throws URISyntaxException {
    log.debug("REST request to save Customer : {}", customer);
    if (customer.getId() != null) {
      throw new BadRequestAlertException(
          "A new customer cannot already have an ID", ENTITY_NAME, "idexists");
    }
    AssuredCustomerDTO result =
        assuredCustomerService.mappAyantDroitEntityToDto(
            assuredCustomerService.createAyantDroitFromDto(customer));
    return ResponseEntity.created(new URI("/api/customers/ayant-droit/" + result.getId()))
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  public ResponseEntity<AssuredCustomerDTO> updateAyantDroit(AssuredCustomerDTO customer)
      throws URISyntaxException {
    log.debug("REST request to update Customer : {}", customer);
    if (customer.getId() == null) {
      throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
    }
    AssuredCustomerDTO result =
        assuredCustomerService.mappAyantDroitEntityToDto(
            assuredCustomerService.updateAyantDroitFromDto(customer));
    return ResponseEntity.ok()
        .headers(
            HeaderUtil.createEntityUpdateAlert(
                applicationName, true, ENTITY_NAME, customer.getId().toString()))
        .body(result);
  }

  public ResponseEntity<Void> deleteCustomer(Long id) {
    log.debug("REST request to delete Customer : {}", id);
    assuredCustomerService.deleteCustomerById(id);
    return ResponseEntity.noContent()
        .headers(
            HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
        .build();
  }
}

package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.ImportationCustomer;
import com.kobe.warehouse.service.SaleDataService;
import com.kobe.warehouse.service.UninsuredCustomerService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.CustomerDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;
import java.io.IOException;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/** REST controller for managing {@link com.kobe.warehouse.domain.Customer}. */
public class CustomerResourceProxy {

  private static final String ENTITY_NAME = "customer";
  private final Logger log = LoggerFactory.getLogger(CustomerResourceProxy.class);
  private final CustomerDataService customerDataService;
  private final SaleDataService saleService;
  private final UninsuredCustomerService uninsuredCustomerService;
  private final ImportationCustomer importationCustomer;
  private final AssuredCustomerService assuredCustomerService;

  @Value("${jhipster.clientApp.name}")
  private String applicationName;

  public CustomerResourceProxy(
      CustomerDataService customerDataService,
      SaleDataService saleService,
      UninsuredCustomerService uninsuredCustomerService,
      ImportationCustomer importationCustomer,
      AssuredCustomerService assuredCustomerService) {
    this.customerDataService = customerDataService;
    this.saleService = saleService;
    this.uninsuredCustomerService = uninsuredCustomerService;
    this.importationCustomer = importationCustomer;
    this.assuredCustomerService = assuredCustomerService;
  }

  public ResponseEntity<List<CustomerDTO>> getAllCustomers(
      Pageable pageable, Status status, String search, String type) {
    log.debug("REST request to get a page of Customers");
    Page<CustomerDTO> page = customerDataService.fetchAllCustomers(type, search, status, pageable);
    HttpHeaders headers =
        PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
    return ResponseEntity.ok().headers(headers).body(page.getContent());
  }

  public ResponseEntity<CustomerDTO> getCustomer(Long id) {
    log.debug("REST request to get Customer : {}", id);
    Optional<CustomerDTO> customer = customerDataService.getOneCustomer(id);
    return ResponseUtil.wrapOrNotFound(customer);
  }

  public ResponseEntity<List<SaleDTO>> customerPurchases(
      long id, LocalDate fromDate, LocalDate toDate) {
    List<SaleDTO> data = saleService.customerPurchases(id, fromDate, toDate);
    return ResponseEntity.ok().body(data);
  }

  public ResponseEntity<UninsuredCustomerDTO> createUninsuredCustomer(UninsuredCustomerDTO customer)
      throws URISyntaxException {
    log.debug("REST request to save Customer : {}", customer);
    if (customer.getId() != null) {
      throw new BadRequestAlertException(
          "A new customer cannot already have an ID", ENTITY_NAME, "idexists");
    }
    UninsuredCustomerDTO result = uninsuredCustomerService.create(customer);
    return ResponseEntity.created(new URI("/api/customers/" + result.getId()))
        .headers(
            HeaderUtil.createEntityCreationAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  public ResponseEntity<UninsuredCustomerDTO> updateUninsuredCustomer(
      UninsuredCustomerDTO uninsuredCustomerDTO) throws URISyntaxException {
    log.debug("REST request to update Customer : {}", uninsuredCustomerDTO);
    if (uninsuredCustomerDTO.getId() == null) {
      throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
    }
    UninsuredCustomerDTO result = uninsuredCustomerService.update(uninsuredCustomerDTO);
    return ResponseEntity.ok()
        .headers(
            HeaderUtil.createEntityUpdateAlert(
                applicationName, true, ENTITY_NAME, result.getId().toString()))
        .body(result);
  }

  public ResponseEntity<List<UninsuredCustomerDTO>> getAllUninsuredCustomers(
      @RequestParam(value = "search", required = false) String search) {
    log.debug("REST request to get a page of Customers");
    List<UninsuredCustomerDTO> dtoList = uninsuredCustomerService.fetch(search);
    return ResponseEntity.ok().body(dtoList);
  }

  public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
    log.debug("REST request to delete Customer : {}", id);
    uninsuredCustomerService.deleteCustomerById(id);
    return ResponseEntity.noContent()
        .headers(
            HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
        .build();
  }

  public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importjson") MultipartFile file)
      throws URISyntaxException, IOException {
    ResponseDTO responseDTO = importationCustomer.updateStocFromJSON(file.getInputStream());
    return ResponseEntity.ok(responseDTO);
  }

  public ResponseEntity<List<AssuredCustomerDTO>> getAllAssuredCustomers(
      @RequestParam(value = "search", required = false) String search,
      @RequestParam(value = "typeTiersPayant", required = false) String typeTiersPayant) {
    log.debug("REST request to get a page of Customers");
    List<AssuredCustomerDTO> dtoList = assuredCustomerService.fetch(search, typeTiersPayant);
    return ResponseEntity.ok().body(dtoList);
  }

  public ResponseEntity<List<ClientTiersPayantDTO>> getAssuredTiersPayants(
      @PathVariable("id") Long id) {
    List<ClientTiersPayantDTO> dtoList = customerDataService.fetchCustomersTiersPayant(id);
    return ResponseEntity.ok().body(dtoList);
  }

  public ResponseEntity<List<AssuredCustomerDTO>> getAyantDroits(@PathVariable("id") Long id) {
    List<AssuredCustomerDTO> dtoList = customerDataService.fetchAyantDroit(id);
    return ResponseEntity.ok().body(dtoList);
  }
}

package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.ImportationCustomer;
import com.kobe.warehouse.service.UninsuredCustomerService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.CustomerDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.sale.SaleDataService;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Customer}.
 */
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
        AssuredCustomerService assuredCustomerService
    ) {
        this.customerDataService = customerDataService;
        this.saleService = saleService;
        this.uninsuredCustomerService = uninsuredCustomerService;
        this.importationCustomer = importationCustomer;
        this.assuredCustomerService = assuredCustomerService;
    }

    @GetMapping("/customers")
    public ResponseEntity<List<CustomerDTO>> getAllCustomers(
        Pageable pageable,
        @RequestParam(required = false, defaultValue = "ENABLE", name = "status") Status status,
        @RequestParam(required = false, name = "search") String search,
        @RequestParam(required = false, name = "type", defaultValue = "TOUT") String type
    ) {
        Page<CustomerDTO> page = customerDataService.fetchAllCustomers(type, search, status, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/customers/{id}")
    public ResponseEntity<CustomerDTO> getCustomer(@PathVariable Long id) {
        log.debug("REST request to get Customer : {}", id);
        Optional<CustomerDTO> customer = customerDataService.getOneCustomer(id);
        return ResponseUtil.wrapOrNotFound(customer);
    }

    @GetMapping("/customers/purchases")
    public ResponseEntity<List<SaleDTO>> customerPurchases(
        @RequestParam(value = "customerId") long id,
        @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) LocalDate toDate
    ) {
        List<SaleDTO> data = saleService.customerPurchases(id, fromDate, toDate);
        return ResponseEntity.ok().body(data);
    }

    @PostMapping("/customers/uninsured")
    public ResponseEntity<UninsuredCustomerDTO> createUninsuredCustomer(@Valid @RequestBody UninsuredCustomerDTO customer)
        throws URISyntaxException {
        log.debug("REST request to save Customer : {}", customer);
        if (customer.getId() != null) {
            throw new BadRequestAlertException("A new customer cannot already have an ID", ENTITY_NAME, "idexists");
        }
        UninsuredCustomerDTO result = uninsuredCustomerService.create(customer);
        return ResponseEntity.created(new URI("/api/customers/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/customers/uninsured")
    public ResponseEntity<UninsuredCustomerDTO> updateUninsuredCustomer(@Valid @RequestBody UninsuredCustomerDTO uninsuredCustomerDTO) {
        log.debug("REST request to update Customer : {}", uninsuredCustomerDTO);
        if (uninsuredCustomerDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        UninsuredCustomerDTO result = uninsuredCustomerService.update(uninsuredCustomerDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @GetMapping("/customers/uninsured")
    public ResponseEntity<List<UninsuredCustomerDTO>> getAllUninsuredCustomers(
        @RequestParam(value = "search", required = false) String search
    ) {
        log.debug("REST request to get a page of Customers");
        List<UninsuredCustomerDTO> dtoList = uninsuredCustomerService.fetch(search);
        return ResponseEntity.ok().body(dtoList);
    }

    @DeleteMapping("/customers/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        log.debug("REST request to delete Customer : {}", id);
        uninsuredCustomerService.deleteCustomerById(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    @PostMapping("/customers/importjson")
    public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importjson") MultipartFile file) throws IOException {
        ResponseDTO responseDTO = importationCustomer.updateStocFromJSON(file.getInputStream());
        return ResponseEntity.ok(responseDTO);
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
    public ResponseEntity<List<ClientTiersPayantDTO>> getAssuredTiersPayants(@PathVariable("id") Long id) {
        return ResponseEntity.ok().body(customerDataService.fetchCustomersTiersPayant(id));
    }

    @GetMapping("/customers/ayant-droits/{id}")
    public ResponseEntity<List<AssuredCustomerDTO>> getAyantDroits(@PathVariable("id") Long id) {
        return ResponseEntity.ok().body(customerDataService.fetchAyantDroit(id));
    }
}

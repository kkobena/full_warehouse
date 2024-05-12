package com.kobe.warehouse.web.rest;

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
import com.kobe.warehouse.web.rest.proxy.CustomerResourceProxy;
import jakarta.validation.Valid;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** REST controller for managing {@link com.kobe.warehouse.domain.Customer}. */
@RestController
@RequestMapping("/api")
@Transactional
public class CustomerResource extends CustomerResourceProxy {

  public CustomerResource(
      CustomerDataService customerDataService,
      SaleDataService saleService,
      UninsuredCustomerService uninsuredCustomerService,
      ImportationCustomer importationCustomer,
      AssuredCustomerService assuredCustomerService) {
    super(
        customerDataService,
        saleService,
        uninsuredCustomerService,
        importationCustomer,
        assuredCustomerService);
  }

  @GetMapping("/customers")
  public ResponseEntity<List<CustomerDTO>> getAllCustomers(
      Pageable pageable,
      @RequestParam(required = false, defaultValue = "ENABLE", name = "status") Status status,
      @RequestParam(required = false, name = "search") String search,
      @RequestParam(required = false, name = "type", defaultValue = "TOUT") String type) {

    return super.getAllCustomers(pageable, status, search, type);
  }

  @GetMapping("/customers/{id}")
  public ResponseEntity<CustomerDTO> getCustomer(@PathVariable Long id) {

    return super.getCustomer(id);
  }

  @GetMapping("/customers/purchases")
  public ResponseEntity<List<SaleDTO>> customerPurchases(
      @RequestParam(value = "customerId") long id,
      @RequestParam(value = "fromDate", required = false) LocalDate fromDate,
      @RequestParam(value = "toDate", required = false) LocalDate toDate) {

    return super.customerPurchases(id, fromDate, toDate);
  }

  @PostMapping("/customers/uninsured")
  public ResponseEntity<UninsuredCustomerDTO> createUninsuredCustomer(
      @Valid @RequestBody UninsuredCustomerDTO customer) throws URISyntaxException {

    return super.createUninsuredCustomer(customer);
  }

  @PutMapping("/customers/uninsured")
  public ResponseEntity<UninsuredCustomerDTO> updateUninsuredCustomer(
      @Valid @RequestBody UninsuredCustomerDTO uninsuredCustomerDTO) throws URISyntaxException {

    return super.updateUninsuredCustomer(uninsuredCustomerDTO);
  }

  @GetMapping("/customers/uninsured")
  public ResponseEntity<List<UninsuredCustomerDTO>> getAllUninsuredCustomers(
      @RequestParam(value = "search", required = false) String search) {

    return super.getAllUninsuredCustomers(search);
  }

  @DeleteMapping("/customers/{id}")
  public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {

    return super.deleteCustomer(id);
  }

  @PostMapping("/customers/importjson")
  public ResponseEntity<ResponseDTO> uploadFile(@RequestPart("importjson") MultipartFile file)
      throws URISyntaxException, IOException {
    return super.uploadFile(file);
  }

  @GetMapping("/customers/assured")
  public ResponseEntity<List<AssuredCustomerDTO>> getAllAssuredCustomers(
      @RequestParam(value = "search", required = false) String search,
      @RequestParam(value = "typeTiersPayant", required = false) String typeTiersPayant) {

    return super.getAllAssuredCustomers(search, typeTiersPayant);
  }

  @GetMapping("/customers/tiers-payants/{id}")
  public ResponseEntity<List<ClientTiersPayantDTO>> getAssuredTiersPayants(
      @PathVariable("id") Long id) {

    return super.getAssuredTiersPayants(id);
  }

  @GetMapping("/customers/ayant-droits/{id}")
  public ResponseEntity<List<AssuredCustomerDTO>> getAyantDroits(@PathVariable("id") Long id) {

    return super.getAyantDroits(id);
  }
}

package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.web.rest.proxy.AssuredCustomerResourceProxy;
import jakarta.validation.Valid;
import java.net.URISyntaxException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client")
public class JavaAssuredCustomerResource extends AssuredCustomerResourceProxy {

  public JavaAssuredCustomerResource(AssuredCustomerService assuredCustomerService) {
    super(assuredCustomerService);
  }

  @PostMapping("/customers/assured")
  public ResponseEntity<AssuredCustomerDTO> createCustomer(
      @Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {
    return super.createCustomer(customer);
  }

  @PutMapping("/customers/assured")
  public ResponseEntity<AssuredCustomerDTO> updateCustomer(
      @Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {
    return super.updateCustomer(customer);
  }

  @PostMapping("/customers/ayant-droit")
  public ResponseEntity<AssuredCustomerDTO> createAyantDroit(
      @Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {
    return super.createAyantDroit(customer);
  }

  @PutMapping("/customers/ayant-droit")
  public ResponseEntity<AssuredCustomerDTO> updateAyantDroit(
      @Valid @RequestBody AssuredCustomerDTO customer) throws URISyntaxException {
    return super.updateAyantDroit(customer);
  }

  @DeleteMapping("/customers/assured/{id}")
  public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
    return super.deleteCustomer(id);
  }
}

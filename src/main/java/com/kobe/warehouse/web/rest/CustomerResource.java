package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.service.SaleService;
import com.kobe.warehouse.service.dto.CustomerDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.web.rest.errors.BadRequestAlertException;

import io.github.jhipster.web.util.HeaderUtil;
import io.github.jhipster.web.util.PaginationUtil;
import io.github.jhipster.web.util.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Customer}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class CustomerResource {

	private final Logger log = LoggerFactory.getLogger(CustomerResource.class);

	private static final String ENTITY_NAME = "customer";

	@Value("${jhipster.clientApp.name}")
	private String applicationName;

	private final CustomerRepository customerRepository;
	private final SaleService saleService;

	public CustomerResource(CustomerRepository customerRepository, SaleService saleService) {
		this.customerRepository = customerRepository;
		this.saleService = saleService;
	}

	
	@PostMapping("/customers")
	public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) throws URISyntaxException {
		log.debug("REST request to save Customer : {}", customer);
		if (customer.getId() != null) {
			throw new BadRequestAlertException("A new customer cannot already have an ID", ENTITY_NAME, "idexists");
		}
		Customer result = customerRepository.save(customer);
		return ResponseEntity
				.created(new URI("/api/customers/" + result.getId())).headers(HeaderUtil
						.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
				.body(result);
	}

	
	@PutMapping("/customers")
	public ResponseEntity<Customer> updateCustomer(@Valid @RequestBody Customer customer) throws URISyntaxException {
		log.debug("REST request to update Customer : {}", customer);
		if (customer.getId() == null) {
			throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
		}
		Customer result = customerRepository.save(customer);
		return ResponseEntity.ok().headers(
				HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, customer.getId().toString()))
				.body(result);
	}

	
	@GetMapping("/customers")
	public ResponseEntity<List<Customer>> getAllCustomers(Pageable pageable,
			@RequestParam(required = false, defaultValue = "false") boolean eagerload) {
		log.debug("REST request to get a page of Customers");
		Page<Customer> page;
		if (eagerload) {
			page = customerRepository.findAllWithEagerRelationships(pageable);
		} else {
			page = customerRepository.findAll(pageable);
		}
		HttpHeaders headers = PaginationUtil
				.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
		return ResponseEntity.ok().headers(headers).body(page.getContent());
	}

	
	@GetMapping("/customers/{id}")
	public ResponseEntity<Customer> getCustomer(@PathVariable Long id) {
		log.debug("REST request to get Customer : {}", id);
		Optional<Customer> customer = customerRepository.findOneWithEagerRelationships(id);
		return ResponseUtil.wrapOrNotFound(customer);
	}

	
	@DeleteMapping("/customers/{id}")
	public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
		log.debug("REST request to delete Customer : {}", id);
		customerRepository.deleteById(id);
		return ResponseEntity.noContent()
				.headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
				.build();
	}

	@GetMapping("/customers/purchases")
	public ResponseEntity<List<SaleDTO>> customerPurchases(@RequestParam(value = "customerId", required = true) long id,
			@RequestParam(value = "fromDate", required = false) LocalDate fromDate,
			@RequestParam(value = "toDate", required = false) LocalDate toDate) {
		
		List<SaleDTO> data = saleService.customerPurchases(id, fromDate, toDate);
		return ResponseEntity.ok().body(data);
	}
	@GetMapping("/customers/ventes")
	public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
		log.debug("REST request to get a page of Customers");
		List<CustomerDTO> customer=customerRepository.findAll().stream().map(CustomerDTO::new).collect(Collectors.toList());
		return ResponseEntity.ok().body(customer);
	}

}

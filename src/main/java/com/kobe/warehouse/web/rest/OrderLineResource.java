package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.jhipster.web.util.HeaderUtil;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.OrderLine}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class OrderLineResource {

    private static final String ENTITY_NAME = "orderLine";
    private final Logger log = LoggerFactory.getLogger(OrderLineResource.class);
    private final OrderLineRepository orderLineRepository;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    public OrderLineResource(OrderLineRepository orderLineRepository) {
        this.orderLineRepository = orderLineRepository;
    }

    /**
     * {@code POST /order-lines} : Create a new orderLine.
     *
     * @param orderLine the orderLine to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new
     * orderLine, or with status {@code 400 (Bad Request)} if the orderLine has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/order-lines")
    public ResponseEntity<OrderLine> createOrderLine(@Valid @RequestBody OrderLine orderLine) throws URISyntaxException {
        log.debug("REST request to save OrderLine : {}", orderLine);
        if (orderLine.getId() != null) {
            throw new BadRequestAlertException("A new orderLine cannot already have an ID", ENTITY_NAME, "idexists");
        }
        OrderLine result = orderLineRepository.save(orderLine);
        return ResponseEntity.created(new URI("/api/order-lines/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT /order-lines} : Updates an existing orderLine.
     *
     * @param orderLine the orderLine to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated
     * orderLine, or with status {@code 400 (Bad Request)} if the orderLine is not valid, or with
     * status {@code 500 (Internal Server Error)} if the orderLine couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/order-lines")
    public ResponseEntity<OrderLine> updateOrderLine(@Valid @RequestBody OrderLine orderLine) throws URISyntaxException {
        log.debug("REST request to update OrderLine : {}", orderLine);
        if (orderLine.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        OrderLine result = orderLineRepository.save(orderLine);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, orderLine.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET /order-lines} : get all the orderLines.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of orderLines in
     * body.
     */
    @GetMapping("/order-lines/{id}")
    public ResponseEntity<List<OrderLine>> getAllOrderLines(@PathVariable Long id) {
        log.debug("REST request to get a page of OrderLines");
        List<OrderLine> listOrderLines = orderLineRepository.findByCommandeIdOrderByFournisseurProduitProduitLibelleAsc(id);
        return ResponseEntity.ok().body(listOrderLines);
    }
}

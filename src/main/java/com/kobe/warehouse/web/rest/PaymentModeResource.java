package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.repository.PaymentModeRepository;
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
import java.util.List;
import java.util.Optional;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.PaymentMode}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class PaymentModeResource {

    private final Logger log = LoggerFactory.getLogger(PaymentModeResource.class);

    private static final String ENTITY_NAME = "paymentMode";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PaymentModeRepository paymentModeRepository;

    public PaymentModeResource(PaymentModeRepository paymentModeRepository) {
        this.paymentModeRepository = paymentModeRepository;
    }

    /**
     * {@code POST  /payment-modes} : Create a new paymentMode.
     *
     * @param paymentMode the paymentMode to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new paymentMode, or with status {@code 400 (Bad Request)} if the paymentMode has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/payment-modes")
    public ResponseEntity<PaymentMode> createPaymentMode(@Valid @RequestBody PaymentMode paymentMode) throws URISyntaxException {
        log.debug("REST request to save PaymentMode : {}", paymentMode);
        if (paymentMode.getId() != null) {
            throw new BadRequestAlertException("A new paymentMode cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PaymentMode result = paymentModeRepository.save(paymentMode);
        return ResponseEntity.created(new URI("/api/payment-modes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /payment-modes} : Updates an existing paymentMode.
     *
     * @param paymentMode the paymentMode to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated paymentMode,
     * or with status {@code 400 (Bad Request)} if the paymentMode is not valid,
     * or with status {@code 500 (Internal Server Error)} if the paymentMode couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/payment-modes")
    public ResponseEntity<PaymentMode> updatePaymentMode(@Valid @RequestBody PaymentMode paymentMode) throws URISyntaxException {
        log.debug("REST request to update PaymentMode : {}", paymentMode);
        if (paymentMode.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        PaymentMode result = paymentModeRepository.save(paymentMode);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, paymentMode.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /payment-modes} : get all the paymentModes.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of paymentModes in body.
     */
    @GetMapping("/payment-modes")
    public ResponseEntity<List<PaymentMode>> getAllPaymentModes(Pageable pageable) {
        log.debug("REST request to get a page of PaymentModes");
        Page<PaymentMode> page = paymentModeRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /payment-modes/:id} : get the "id" paymentMode.
     *
     * @param id the id of the paymentMode to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the paymentMode, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/payment-modes/{id}")
    public ResponseEntity<PaymentMode> getPaymentMode(@PathVariable Long id) {
        log.debug("REST request to get PaymentMode : {}", id);
        Optional<PaymentMode> paymentMode = paymentModeRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(paymentMode);
    }

    /**
     * {@code DELETE  /payment-modes/:id} : delete the "id" paymentMode.
     *
     * @param id the id of the paymentMode to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/payment-modes/{id}")
    public ResponseEntity<Void> deletePaymentMode(@PathVariable Long id) {
        log.debug("REST request to delete PaymentMode : {}", id);
        paymentModeRepository.deleteById(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }
}

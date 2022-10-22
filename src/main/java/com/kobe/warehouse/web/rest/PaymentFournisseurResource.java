package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.domain.PaymentFournisseur;
import com.kobe.warehouse.repository.PaymentFournisseurRepository;
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
 * REST controller for managing {@link com.kobe.warehouse.domain.PaymentFournisseur}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class PaymentFournisseurResource {

    private final Logger log = LoggerFactory.getLogger(PaymentFournisseurResource.class);

    private static final String ENTITY_NAME = "paymentFournisseur";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PaymentFournisseurRepository paymentFournisseurRepository;

    public PaymentFournisseurResource(PaymentFournisseurRepository paymentFournisseurRepository) {
        this.paymentFournisseurRepository = paymentFournisseurRepository;
    }

    /**
     * {@code POST  /payment-fournisseurs} : Create a new paymentFournisseur.
     *
     * @param paymentFournisseur the paymentFournisseur to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new paymentFournisseur, or with status {@code 400 (Bad Request)} if the paymentFournisseur has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("/payment-fournisseurs")
    public ResponseEntity<PaymentFournisseur> createPaymentFournisseur(@Valid @RequestBody PaymentFournisseur paymentFournisseur) throws URISyntaxException {
        log.debug("REST request to save PaymentFournisseur : {}", paymentFournisseur);
        if (paymentFournisseur.getId() != null) {
            throw new BadRequestAlertException("A new paymentFournisseur cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PaymentFournisseur result = paymentFournisseurRepository.save(paymentFournisseur);
        return ResponseEntity.created(new URI("/api/payment-fournisseurs/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /payment-fournisseurs} : Updates an existing paymentFournisseur.
     *
     * @param paymentFournisseur the paymentFournisseur to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated paymentFournisseur,
     * or with status {@code 400 (Bad Request)} if the paymentFournisseur is not valid,
     * or with status {@code 500 (Internal Server Error)} if the paymentFournisseur couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/payment-fournisseurs")
    public ResponseEntity<PaymentFournisseur> updatePaymentFournisseur(@Valid @RequestBody PaymentFournisseur paymentFournisseur) throws URISyntaxException {
        log.debug("REST request to update PaymentFournisseur : {}", paymentFournisseur);
        if (paymentFournisseur.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        PaymentFournisseur result = paymentFournisseurRepository.save(paymentFournisseur);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, paymentFournisseur.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /payment-fournisseurs} : get all the paymentFournisseurs.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of paymentFournisseurs in body.
     */
    @GetMapping("/payment-fournisseurs")
    public ResponseEntity<List<PaymentFournisseur>> getAllPaymentFournisseurs(Pageable pageable) {
        log.debug("REST request to get a page of PaymentFournisseurs");
        Page<PaymentFournisseur> page = paymentFournisseurRepository.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /payment-fournisseurs/:id} : get the "id" paymentFournisseur.
     *
     * @param id the id of the paymentFournisseur to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the paymentFournisseur, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/payment-fournisseurs/{id}")
    public ResponseEntity<PaymentFournisseur> getPaymentFournisseur(@PathVariable Long id) {
        log.debug("REST request to get PaymentFournisseur : {}", id);
        Optional<PaymentFournisseur> paymentFournisseur = paymentFournisseurRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(paymentFournisseur);
    }

    /**
     * {@code DELETE  /payment-fournisseurs/:id} : delete the "id" paymentFournisseur.
     *
     * @param id the id of the paymentFournisseur to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/payment-fournisseurs/{id}")
    public ResponseEntity<Void> deletePaymentFournisseur(@PathVariable Long id) {
        log.debug("REST request to delete PaymentFournisseur : {}", id);
        paymentFournisseurRepository.deleteById(id);
        return ResponseEntity.noContent().headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString())).build();
    }
}

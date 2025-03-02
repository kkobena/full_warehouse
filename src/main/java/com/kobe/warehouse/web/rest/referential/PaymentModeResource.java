package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.service.PaymentModeService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.PaymentMode}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class PaymentModeResource {

    private final PaymentModeService paymentModeService;

    public PaymentModeResource(PaymentModeService paymentModeService) {
        this.paymentModeService = paymentModeService;
    }

    /* @PostMapping("/payment-modes")
    public ResponseEntity<PaymentMode> createPaymentMode(@Valid @RequestBody PaymentMode paymentMode) throws URISyntaxException {
        log.debug("REST request to save PaymentMode : {}", paymentMode);
        if (paymentMode.getCode() != null) {
            throw new BadRequestAlertException("A new paymentMode cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PaymentMode result = paymentModeRepository.save(paymentMode);
        return ResponseEntity
            .created(new URI("/api/payment-modes/" + result.getCode()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getCode()))
            .body(result);
    }*/

    /*   @PutMapping("/payment-modes")
    public ResponseEntity<PaymentMode> updatePaymentMode(@Valid @RequestBody PaymentMode paymentMode) throws URISyntaxException {
        log.debug("REST request to update PaymentMode : {}", paymentMode);
        if (paymentMode.getCode() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        PaymentMode result = paymentModeRepository.save(paymentMode);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, paymentMode.getCode()))
            .body(result);
    }*/

    @GetMapping("/payment-modes")
    public ResponseEntity<List<PaymentMode>> getAllPaymentModes() {
        return ResponseEntity.ok().body(this.paymentModeService.fetch());
    }
    /*

    @DeleteMapping("/payment-modes/{id}")
    public ResponseEntity<Void> deletePaymentMode(@PathVariable String id) {
        log.debug("REST request to delete PaymentMode : {}", id);
        paymentModeRepository.deleteById(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }*/
}

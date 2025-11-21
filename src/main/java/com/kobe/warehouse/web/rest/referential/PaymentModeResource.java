package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.service.PaymentModeService;
import java.util.List;

import com.kobe.warehouse.service.dto.projection.QrCodeResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.PaymentMode}.
 */
@RestController
@RequestMapping("/api")

public class PaymentModeResource {

    private final PaymentModeService paymentModeService;

    public PaymentModeResource(PaymentModeService paymentModeService) {
        this.paymentModeService = paymentModeService;
    }

    @GetMapping("/payment-modes")
    public ResponseEntity<List<PaymentMode>> getAllPaymentModes() {
        return ResponseEntity.ok().body(this.paymentModeService.fetchAll());
    }

    @GetMapping("/payment-restricts-modes")
    public ResponseEntity<List<PaymentMode>> fetch() {
        return ResponseEntity.ok().body(this.paymentModeService.fetch());
    }
    @GetMapping("/payment-modes/{code}/qr-code")
    public ResponseEntity<QrCodeResponse> getPaymentQrCode(@PathVariable String code) {
        return ResponseEntity.ok().body(this.paymentModeService.getPaymentQrCode(code));
    }
}

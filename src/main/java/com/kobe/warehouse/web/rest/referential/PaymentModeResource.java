package com.kobe.warehouse.web.rest.referential;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.service.PaymentModeService;
import java.io.IOException;
import java.util.List;

import com.kobe.warehouse.service.dto.PaymentModeUpdateDTO;
import com.kobe.warehouse.service.dto.projection.QrCodeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/payment-modes/{code}")
    public ResponseEntity<PaymentMode> getPaymentMode(@PathVariable String code) {
        return this.paymentModeService.findByCode(code)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/payment-modes/{code}/qr-code")
    public ResponseEntity<QrCodeResponse> getPaymentQrCode(@PathVariable String code) {
        return ResponseEntity.ok().body(this.paymentModeService.getPaymentQrCode(code));
    }

    /**
     * PUT /payment-modes : Update an existing payment mode with optional QR code.
     *
     * @param code the payment mode code
     * @param libelle the payment mode label
     * @param order the display order
     * @param qrCodeFile the QR code image file (optional)
     * @return the ResponseEntity with status 200 (OK) and the updated payment mode, or status 404 (Not Found)
     */
    @PutMapping(value = "/payment-modes/{code}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PaymentMode> updatePaymentMode(
        @PathVariable String code,
        @RequestParam("libelle") String libelle,
        @RequestParam("order") Short order,
        @RequestParam(value = "qrCodeFile", required = false) MultipartFile qrCodeFile
    ) {
        try {
            PaymentModeUpdateDTO dto = new PaymentModeUpdateDTO()
                .setCode(code)
                .setLibelle(libelle)
                .setOrder(order);

            PaymentMode result = this.paymentModeService.update(dto, qrCodeFile);
            return ResponseEntity.ok().body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /payment-modes/{code}/qr-code : Remove QR code from a payment mode.
     *
     * @param code the payment mode code
     * @return the ResponseEntity with status 200 (OK)
     */
    @DeleteMapping("/payment-modes/{code}/qr-code")
    public ResponseEntity<PaymentMode> removeQrCode(@PathVariable String code) {
        try {
            PaymentMode result = this.paymentModeService.removeQrCode(code);
            return ResponseEntity.ok().body(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

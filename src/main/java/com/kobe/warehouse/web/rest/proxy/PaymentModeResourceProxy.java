package com.kobe.warehouse.web.rest.proxy;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.service.PaymentModeService;
import java.util.List;
import org.springframework.http.ResponseEntity;

/** REST controller for managing {@link PaymentMode}. */
public class PaymentModeResourceProxy {

  private final PaymentModeService paymentModeService;

  public PaymentModeResourceProxy(PaymentModeService paymentModeService) {

    this.paymentModeService = paymentModeService;
  }

  public ResponseEntity<List<PaymentMode>> getAllPaymentModes() {
    return ResponseEntity.ok().body(this.paymentModeService.fetch());
  }
}

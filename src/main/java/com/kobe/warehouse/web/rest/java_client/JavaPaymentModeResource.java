package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.service.PaymentModeService;
import com.kobe.warehouse.web.rest.proxy.PaymentModeResourceProxy;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for managing {@link PaymentMode}. */
@RestController
@RequestMapping("/java-client")
public class JavaPaymentModeResource extends PaymentModeResourceProxy {

  public JavaPaymentModeResource(PaymentModeService paymentModeService) {
    super(paymentModeService);
  }

  @GetMapping("/payment-modes")
  public ResponseEntity<List<PaymentMode>> getAllPaymentModes() {
    return super.getAllPaymentModes();
  }
}

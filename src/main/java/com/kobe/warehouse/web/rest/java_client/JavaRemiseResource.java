package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.service.remise.RemiseService;
import com.kobe.warehouse.web.rest.proxy.RemiseResourceProxy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link PaymentMode}.
 */
@RestController
@RequestMapping("/java-client")
public class JavaRemiseResource extends RemiseResourceProxy {

    public JavaRemiseResource(RemiseService remiseService) {
        super(remiseService);
    }
}

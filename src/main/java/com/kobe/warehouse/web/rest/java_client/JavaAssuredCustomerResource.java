package com.kobe.warehouse.web.rest.java_client;

import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.web.rest.proxy.AssuredCustomerResourceProxy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client")
public class JavaAssuredCustomerResource extends AssuredCustomerResourceProxy {

  public JavaAssuredCustomerResource(AssuredCustomerService assuredCustomerService) {
    super(assuredCustomerService);
  }
}

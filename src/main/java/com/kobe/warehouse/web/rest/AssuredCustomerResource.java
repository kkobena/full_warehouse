package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.web.rest.proxy.AssuredCustomerResourceProxy;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AssuredCustomerResource extends AssuredCustomerResourceProxy {

  public AssuredCustomerResource(AssuredCustomerService assuredCustomerService) {
    super(assuredCustomerService);
  }
}

package com.kobe.warehouse.web.rest;

import com.kobe.warehouse.service.AssuredCustomerService;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.ImportationCustomer;
import com.kobe.warehouse.service.UninsuredCustomerService;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.web.rest.proxy.CustomerResourceProxy;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing {@link com.kobe.warehouse.domain.Customer}.
 */
@RestController
@RequestMapping("/api")
@Transactional
public class CustomerResource extends CustomerResourceProxy {

    public CustomerResource(
        CustomerDataService customerDataService,
        SaleDataService saleService,
        UninsuredCustomerService uninsuredCustomerService,
        ImportationCustomer importationCustomer,
        AssuredCustomerService assuredCustomerService
    ) {
        super(customerDataService, saleService, uninsuredCustomerService, importationCustomer, assuredCustomerService);
    }
}

package com.kobe.warehouse.web.rest.cash_register;

import com.kobe.warehouse.service.cash_register.CashRegisterService;
import com.kobe.warehouse.service.cash_register.TicketingService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/java-client")
public class JavaCashRegisterResource extends CashRegisterResourceProxy {

    public JavaCashRegisterResource(CashRegisterService cashRegisterService, TicketingService ticketingService) {
        super(cashRegisterService, ticketingService);
    }
}

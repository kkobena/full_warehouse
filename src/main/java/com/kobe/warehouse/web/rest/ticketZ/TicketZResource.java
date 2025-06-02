package com.kobe.warehouse.web.rest.ticketZ;

import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import com.kobe.warehouse.service.tiketz.service.TicketZService;
import java.awt.print.PrinterException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ticketz")
public class TicketZResource {

    private final TicketZService ticketZService;

    public TicketZResource(TicketZService ticketZService) {
        this.ticketZService = ticketZService;
    }


    @GetMapping
    public ResponseEntity<TicketZ> getTicketZ(TicketZParam param) {
        return ResponseEntity.ok().body(ticketZService.getTicketZ(param));
    }

    @GetMapping("/print")
    public ResponseEntity<Void> printReceipt(TicketZParam param) throws PrinterException {
        ticketZService.printTicketZ(null, param);
        return ResponseEntity.ok().build();
    }
}

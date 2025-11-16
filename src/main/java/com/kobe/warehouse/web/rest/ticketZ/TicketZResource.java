package com.kobe.warehouse.web.rest.ticketZ;

import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import com.kobe.warehouse.service.tiketz.service.TicketZService;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> generatePdf(TicketZParam param) {
        return ticketZService.generatePdf(param);
    }

    @GetMapping("/email")
    public ResponseEntity<Void> sentToEmail(TicketZParam param) {
        ticketZService.sentToEmail(param);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/print-tauri")
    public ResponseEntity<byte[]> getReceiptForTauri(TicketZParam param) {
        try {
            byte[] escPosData = ticketZService.generateEscPosReceiptForTauri(param);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/octet-stream")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"receipt.bin\"")
                .body(escPosData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

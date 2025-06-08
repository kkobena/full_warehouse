package com.kobe.warehouse.service.tiketz.service;

import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import java.awt.print.PrinterException;
import org.springframework.http.ResponseEntity;

public interface TicketZService {
    TicketZ getTicketZ(TicketZParam param);

    void printTicketZ(String hostName, TicketZParam param) throws PrinterException;

    ResponseEntity<byte[]> generatePdf(TicketZParam param);

    // sent to  email
    void sentToEmail(TicketZParam param);
    // sent to sms
}

package com.kobe.warehouse.service.tiketz.service;

import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZParam;
import java.awt.print.PrinterException;

public interface TicketZService {
    TicketZ getTicketZ(TicketZParam param);

    void printTicketZ(String hostName,TicketZParam param) throws PrinterException;
}

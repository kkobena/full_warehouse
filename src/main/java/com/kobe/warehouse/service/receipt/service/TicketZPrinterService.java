package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import java.awt.Graphics2D;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TicketZPrinterService extends AbstractTicketZService{

    protected TicketZPrinterService(
        AppConfigurationService appConfigurationService,
        PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected int drawReglement(Graphics2D graphics2D, int width, int margin, int y,
        int lineHeight) {
        return y;
    }

    @Override
    protected List<HeaderFooterItem> getFooterItems() {
        return List.of();
    }

    @Override
    protected List<byte[]> generateTicket() throws IOException {
        return List.of();
    }

    @Override
    protected List<? extends AbstractItem> getItems() {
        return List.of();
    }

    public void printTicketZ(String hostName, TicketZ ticket, LocalDateTime from, LocalDateTime to)
        throws PrinterException {
        super.print(hostName, ticket, from, to);
    }
}

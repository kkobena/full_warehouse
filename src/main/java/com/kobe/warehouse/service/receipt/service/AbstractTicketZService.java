package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.receipt.dto.TicketZItem;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZData;
import com.kobe.warehouse.service.tiketz.dto.TicketZRecap;
import com.kobe.warehouse.service.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.print.PrintException;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public abstract class AbstractTicketZService extends AbstractJava2DReceiptPrinterService {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractTicketZService.class);
    private String periode;
    private List<TicketZItem> ticketZItems;

    protected AbstractTicketZService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected int getNumberOfCopies() {
        return 1; // Default to 1 copy, can be overridden by subclasses if needed
    }


    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        return List.of(new HeaderFooterItem("RECAPITULATIF DE CAISSE DU " + periode, 1, null));
    }



    private List<TicketZItem> buildTicketZItems(TicketZ ticket) {
        List<TicketZItem> items = new java.util.ArrayList<>();
        List<TicketZData> summaries = ticket.summaries();
        List<TicketZRecap> datas = ticket.datas();
        AtomicInteger count = new AtomicInteger(0);
        datas.forEach(data -> {
            count.incrementAndGet();
            new TicketZItem(data.userName(), null, 1, BOLD_FONT);
            items.add(new TicketZItem("CAISSE DE : " + data.userName(), null, 1, BOLD_FONT));
            data
                .datas()
                .forEach(d -> {
                    items.add(new TicketZItem(d.libelle(), d.montant(), 1, PLAIN_FONT));
                });
            data
                .summary()
                .forEach(sum -> {
                    items.add(new TicketZItem(sum.libelle(), sum.montant(), 2, BOLD_FONT));
                });
        });

        if (!CollectionUtils.isEmpty(summaries) && count.get() > 1) {
            new TicketZItem("TOTAL GENERAL", null, 1, BOLD_FONT);
            summaries.forEach(sum -> {
                items.add(new TicketZItem(sum.libelle(), sum.montant(), 1, PLAIN_FONT));
            });
        }

        return items;
    }

    public void printTicketZ(String hostName, TicketZ ticket, LocalDateTime from, LocalDateTime to) {
        this.ticketZItems = buildTicketZItems(ticket);
        this.periode = "DU " + DateUtil.format(from) + " AU " + DateUtil.format(to);
        try {
            // Use direct ESC/POS printing for better performance and reliability
            printEscPosDirectByHost(hostName, false);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS Ticket Z: {}", e.getMessage(), e);
        }
    }
    @Override
    protected byte[] generateEscPosReceipt(boolean isEdit) throws IOException {
        magasin = appConfigurationService.getMagasin();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // Print common company header
            printEscPosCompanyHeader(out);

            // Separator line
            escPosPrintSeparator(out, 48);

            // Print all ticket Z items
            for (TicketZItem item : ticketZItems) {
                // Set bold based on font
                boolean isBold = item.font().equals(BOLD_FONT);
                escPosSetBold(out, isBold);

                // Format the line
                if (item.column2() != null) {
                    // Two columns - left and right aligned
                    escPosPrintLine(out, String.format("%-37s %10s", item.column1(), item.column2()));
                } else {
                    // Single column - left aligned
                    escPosPrintLine(out, item.column1());
                }

                // Add extra line breaks if specified
                if (item.lineBreakNumber() > 1) {
                    escPosFeedLines(out, item.lineBreakNumber() - 1);
                }
            }

            // Print common footer
            printEscPosFooter(out);

            return out.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to generate ESC/POS Ticket Z: " + e.getMessage(), e);
        } finally {
            out.close();
        }
    }

}

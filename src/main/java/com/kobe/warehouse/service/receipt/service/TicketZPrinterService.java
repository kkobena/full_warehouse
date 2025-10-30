package com.kobe.warehouse.service.receipt.service;

import static org.springframework.util.CollectionUtils.isEmpty;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.receipt.dto.TicketZItem;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.print.PrintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TicketZPrinterService extends AbstractTicketZService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketZPrinterService.class);
    private TicketZ currentTicket;
    private List<TicketZItem> currentTicketZItems;

    protected TicketZPrinterService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected int drawReglement(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        return y;
    }

    @Override
    protected byte[] generateEscPosReceipt(boolean isEdit) throws IOException {
        magasin = appConfigurationService.getMagasin();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // Initialize printer
            escPosInitialize(out);

            // Company header (centered, bold)
            escPosSetBold(out, true);
            escPosSetAlignment(out, EscPosAlignment.CENTER);
            escPosSetTextSize(out, 2, 2); // Double width and height
            escPosPrintLine(out, magasin.getName());
            escPosSetTextSize(out, 1, 1); // Normal size
            escPosFeedLines(out, 1);

            // Company address and contact info
            if (magasin.getAddress() != null && !magasin.getAddress().isEmpty()) {
                escPosPrintLine(out, magasin.getAddress());
            }
            if (magasin.getPhone() != null && !magasin.getPhone().isEmpty()) {
                escPosPrintLine(out, "Tel: " + magasin.getPhone());
            }
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);

            // Welcome message (if any)
            if (magasin.getWelcomeMessage() != null && !magasin.getWelcomeMessage().isEmpty()) {
                escPosPrintLine(out, magasin.getWelcomeMessage());
                escPosFeedLines(out, 1);
            }

            // Left aligned for ticket Z items
            escPosSetAlignment(out, EscPosAlignment.LEFT);

            // Separator line
            escPosPrintSeparator(out, 48);

            // Print all ticket Z items
            for (TicketZItem item : currentTicketZItems) {
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

            // Separator line
            escPosPrintSeparator(out, 48);

            // Date and time
            escPosSetBold(out, false);
            escPosPrintLine(
                out,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " " +
                java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            );
            escPosFeedLines(out, 1);

            // Thank you message
            if (magasin.getNote() != null && !magasin.getNote().isEmpty()) {
                escPosSetAlignment(out, EscPosAlignment.CENTER);
                escPosPrintLine(out, magasin.getNote());
                escPosSetAlignment(out, EscPosAlignment.LEFT);
            }

            // Cut paper
            escPosFeedLines(out, 3);
            escPosCutPaper(out);

            return out.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to generate ESC/POS Ticket Z: " + e.getMessage(), e);
        } finally {
            out.close();
        }
    }

    @Override
    protected List<HeaderFooterItem> getFooterItems() {
        return List.of();
    }

    @Override
    protected List<? extends AbstractItem> getItems() {
        return List.of();
    }

    public void printTicketZ(String hostName, TicketZ ticket, LocalDateTime from, LocalDateTime to) {
        this.currentTicket = ticket;
        this.currentTicketZItems = buildTicketZItems(ticket);

        try {
            // Use direct ESC/POS printing for better performance and reliability
            printEscPosDirectByHost(hostName, false);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS Ticket Z: {}", e.getMessage(), e);
        }
    }

    /**
     * Build the list of TicketZItems from TicketZ data
     * This method is copied from AbstractTicketZService to make it accessible here
     */
    private List<TicketZItem> buildTicketZItems(TicketZ ticket) {
        List<TicketZItem> items = new java.util.ArrayList<>();
        var summaries = ticket.summaries();
        var datas = ticket.datas();
        AtomicInteger count = new AtomicInteger(0);

        datas.forEach(data -> {
            count.incrementAndGet();
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

        if (!isEmpty(summaries) && count.get() > 1) {
            summaries.forEach(sum -> {
                items.add(new TicketZItem(sum.libelle(), sum.montant(), 1, PLAIN_FONT));
            });
        }

        return items;
    }
}

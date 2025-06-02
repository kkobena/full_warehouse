package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.receipt.dto.TicketZItem;
import com.kobe.warehouse.service.tiketz.dto.TicketZ;
import com.kobe.warehouse.service.tiketz.dto.TicketZData;
import com.kobe.warehouse.service.tiketz.dto.TicketZRecap;
import com.kobe.warehouse.service.utils.DateUtil;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public abstract class AbstractTicketZService extends AbstractJava2DReceiptPrinterService {

    //38 *21,2
    //   private int itemSize = 0;
    private String periode;
    private TicketZ ticket;
    private List<TicketZItem> ticketZItems;

    protected AbstractTicketZService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected int getNumberOfCopies() {
        return 1; // Default to 1 copy, can be overridden by subclasses if needed
    }

    protected void print(String hostName, TicketZ ticket, LocalDateTime from, LocalDateTime to) throws PrinterException {
        this.ticket = ticket;
        this.ticketZItems = buildTicketZItems(ticket);
        this.periode = "DU " + DateUtil.format(from) + " AU " + DateUtil.format(to);
        print(hostName);
    }

    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        return List.of(new HeaderFooterItem("RECAPITULATIF DE CAISSE DU " + periode, 1, null));
    }

    // @Override
    public int print__(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex == 0) {
            Graphics2D graphics2D = (Graphics2D) graphics;
            graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int lineHeight = DEFAULT_LINE_HEIGHT;

            int y = lineHeight;
            y = drawCompagnyInfo(graphics2D, DEFAULT_MARGIN, y);
            y = drawHeader(graphics2D, DEFAULT_MARGIN, y, lineHeight);

            FontMetrics fontMetrics;
            List<TicketZData> summaries = ticket.summaries();
            List<TicketZRecap> datas = ticket.datas();
            for (TicketZRecap ticketZRecap : datas) {
                graphics2D.setFont(BOLD_FONT);
                graphics2D.drawString("Caisse de : " + ticketZRecap.userName(), DEFAULT_MARGIN, y);
                graphics2D.setFont(PLAIN_FONT);
                y += lineHeight;
                for (TicketZData data : ticketZRecap.datas()) {
                    graphics2D.drawString(data.libelle(), DEFAULT_MARGIN, y);
                    fontMetrics = graphics2D.getFontMetrics();
                    String montant = data.montant();
                    graphics2D.drawString(montant, DEFAULT_WIDTH - fontMetrics.stringWidth(montant), y);
                    y += lineHeight;
                }
                List<TicketZData> summariesUser = ticketZRecap.summary();
                if (!CollectionUtils.isEmpty(summariesUser)) {
                    graphics2D.setFont(BOLD_FONT);
                    fontMetrics = graphics2D.getFontMetrics();
                    for (TicketZData sum : summariesUser) {
                        graphics2D.drawString(sum.libelle(), DEFAULT_MARGIN, y);
                        String montant = sum.montant();
                        graphics2D.drawString(montant, DEFAULT_WIDTH - fontMetrics.stringWidth(montant), y);
                        y += lineHeight;
                    }
                }
                y += lineHeight;
            }
            graphics2D.setFont(BOLD_FONT);
            for (TicketZData summary : summaries) {
                graphics2D.drawString(summary.libelle(), DEFAULT_MARGIN, y);
                fontMetrics = graphics2D.getFontMetrics();
                String montant = summary.montant();
                graphics2D.drawString(montant, DEFAULT_WIDTH - fontMetrics.stringWidth(montant), y);
                y += lineHeight;
            }
        }
        return PAGE_EXISTS;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

        int lineHeight = DEFAULT_LINE_HEIGHT;
        int maximumLinesPerPage = getMaximumLinesPerPage();
        int itemsSize = ticketZItems.size();
        int totalPages = (int) Math.ceil((double) itemsSize / maximumLinesPerPage);
        if (pageIndex >= totalPages) {
            return NO_SUCH_PAGE;
        }

        int sartItemIndex = pageIndex * maximumLinesPerPage;
        int endItemIndex = Math.min(sartItemIndex + maximumLinesPerPage, itemsSize);
        //  boolean isLastPage = pageIndex == totalPages - 1;
        int y = lineHeight;
        y = drawCompagnyInfo(graphics2D, DEFAULT_MARGIN, y);
        y = drawWelcomeMessage(graphics2D, DEFAULT_MARGIN, y);
        FontMetrics fontMetrics;

        for (int i = sartItemIndex; i < endItemIndex; i++) {
            TicketZItem item = ticketZItems.get(i);
            graphics2D.setFont(item.font());
            fontMetrics = graphics2D.getFontMetrics(item.font());
            graphics2D.drawString(item.column1(), DEFAULT_MARGIN, y);
            if (Objects.nonNull(item.column2())) {
                graphics2D.drawString(item.column2(), DEFAULT_WIDTH - fontMetrics.stringWidth(item.column2()), y);
            }
            y += (lineHeight * item.lineBreakNumber());
        }

        return PAGE_EXISTS;
    }

    private List<TicketZItem> buildTicketZItems(TicketZ ticket) {
        List<TicketZItem> items = new java.util.ArrayList<>();
        List<TicketZData> summaries = ticket.summaries();
        List<TicketZRecap> datas = ticket.datas();
        AtomicInteger count = new AtomicInteger(0);
        datas.forEach(data -> {
            count.incrementAndGet();
            new TicketZItem(data.userName(), null, 1, BOLD_FONT);
            items.add(new TicketZItem("Caisse de : " + data.userName(), null, 1, BOLD_FONT));
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
}

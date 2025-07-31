package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Printer;
import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public abstract class AbstractJava2DReceiptPrinterService implements Printable {

    protected static final String MONTANT_RENDU = "Monnaie";
    protected static final String MONTANT_TTC = "Montant Ttc";
    protected static final String REMISE = "Remise";
    protected static final String TOTAL_TVA = "Total Tva";
    protected static final String TOTAL_A_PAYER = "Total à payer";
    protected static final String RESTE_A_PAYER = "Reste à payer";
    protected static final String TVA = "Taxes";
    protected static final String REGLEMENT = "Règlement";
    protected static final int DEFAULT_LINE_HEIGHT = 12;
    protected static final int DEFAULT_FONT_SIZE = 8;
    protected static final int DEFAULT_MARGIN = 9;
    protected static final Font BOLD_FONT = new Font("Arial, sans-serif", Font.BOLD, DEFAULT_FONT_SIZE);
    protected static final Font PLAIN_FONT = new Font("Arial, sans-serif", Font.PLAIN, DEFAULT_FONT_SIZE);
    protected static final int DEFAULT_WIDTH = ((int) ((8 / 2.54) * 72)) - (DEFAULT_MARGIN * 2); // 8cm soit 80mm
    //38 *21,2
    private final AppConfigurationService appConfigurationService;
    private final PrinterRepository printerRepository;
    protected Magasin magasin;
    protected Printer printer;
    protected PrinterJob printerJob;

    protected AbstractJava2DReceiptPrinterService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        this.appConfigurationService = appConfigurationService;
        this.printerRepository = printerRepository;
    }

    protected void print(String hostName) throws PrinterException {
        PrinterJob job = getPrinterJob(hostName);
        PageFormat pageFormat = job.defaultPage();
        Paper paper = new Paper();
        paper.setImageableArea(DEFAULT_MARGIN, DEFAULT_LINE_HEIGHT, paper.getWidth(), paper.getHeight());
        pageFormat.setPaper(paper);
        pageFormat.setOrientation(PageFormat.PORTRAIT);
        job.setPrintable(this, pageFormat);
        job.setCopies(getNumberOfCopies());
        job.print();
    }

    protected abstract List<HeaderFooterItem> getHeaderItems();

    protected abstract List<HeaderFooterItem> getFooterItems();

    protected abstract int getNumberOfCopies();

    protected int drawWelcomeMessage(Graphics2D graphics2D, int margin, int y) {
        if (StringUtils.hasText(magasin.getWelcomeMessage())) {
            graphics2D.setFont(PLAIN_FONT);
            graphics2D.drawString("*** " + magasin.getWelcomeMessage() + " ***", margin, y);
            y += DEFAULT_LINE_HEIGHT;
            return y;
        }
        return y;
    }

    protected abstract List<? extends AbstractItem> getItems();

    protected abstract int drawReglement(Graphics2D graphics2D, int width, int margin, int y, int lineHeight);

    protected int getMaximumLinesPerPage() {
        return this.appConfigurationService.getPrinterItemCount();
    }

    protected int drawCompagnyInfo(Graphics2D graphics2D, int margin, int y) {
        magasin = appConfigurationService.getMagasin();
        Font font = BOLD_FONT;
        graphics2D.setFont(font);
        int lineHeight = DEFAULT_LINE_HEIGHT;
        int printWidth = DEFAULT_WIDTH;
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int x = margin + (printWidth - fontMetrics.stringWidth(magasin.getName())) / 3;
        y += lineHeight;
        graphics2D.drawString(magasin.getName(), x, y);
        y += lineHeight;
        graphics2D.setFont(PLAIN_FONT);
        fontMetrics = graphics2D.getFontMetrics(font);

        y = drawMagasinInfoLine(graphics2D, magasin.getAddress(), margin, y, lineHeight, printWidth, fontMetrics);
        y = drawMagasinInfoLine(graphics2D, magasin.getEmail(), margin, y, lineHeight, printWidth, fontMetrics);
        y = drawMagasinInfoLine(graphics2D, magasin.getPhone(), margin, y, lineHeight, printWidth, fontMetrics);

        y += lineHeight;
        return y;
    }

    protected int drawFooter(Graphics2D graphics2D, int margin, int y, int lineHeight) {
        graphics2D.setFont(PLAIN_FONT);
        List<HeaderFooterItem> footerItems = getFooterItems();
        for (HeaderFooterItem headerFooterItem : footerItems) {
            graphics2D.drawString(headerFooterItem.value(), margin, y);
            y += (lineHeight * headerFooterItem.lineBreakNumber());
        }
        return y;
    }

    protected int drawHeader(Graphics2D graphics2D, int margin, int y, int lineHeight) {
        for (HeaderFooterItem headerFooterItem : getHeaderItems()) {
            graphics2D.drawString(headerFooterItem.value(), margin, y);
            y += lineHeight;
        }
        return y;
    }

    protected int drawLineSeparator(Graphics2D graphics2D, int margin, int y, int width) {
        graphics2D.setStroke(new BasicStroke(0.5f));
        graphics2D.drawLine(margin, y, width, y);
        y += 10;
        return y;
    }

    protected int drawDate(Graphics2D graphics2D, int x, int y, int lineHeight) {
        graphics2D.setFont(PLAIN_FONT);

        graphics2D.drawString("Le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), x, y);
        graphics2D.drawString("à " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), x + 55, y);
        y += lineHeight;
        return y;
    }

    protected void drawThanksMessage(Graphics2D graphics2D, int margin, int y) {
        if (StringUtils.hasLength(magasin.getNote())) {
            graphics2D.setFont(PLAIN_FONT);
            graphics2D.drawString("*** " + magasin.getNote() + " ***", margin, y);
        }
    }

    protected PrinterJob getPrinterJob(String hostName) throws PrinterException {
        String printerName = StringUtils.hasLength(hostName)
            ? printerRepository.findByPosteName(hostName).map(Printer::getName).orElse(null)
            : null;
        PrintService selectedService = getPrintService(printerName);
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        printerJob.setPrintService(selectedService);
        return printerJob;
    }

    protected void drawAndCenterText(Graphics2D graphics2D, String text, int width, int margin, int y) {
        int printWidth = width - (margin * 2);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int x = margin + (printWidth - fontMetrics.stringWidth(text)) / 3;
        graphics2D.drawString(text, x, y);
    }

    protected void underlineText(Graphics2D graphics2D, String text, int width, int margin, int y) {
        graphics2D.setStroke(new BasicStroke(0.5f));
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int textWidth = fontMetrics.stringWidth(text);
        int x = (width - textWidth) / 3;
        graphics2D.drawLine(x, y, x + textWidth + margin, y);
    }

    protected int getRightMargin() {
        return DEFAULT_WIDTH;
    }

    protected PrintService getPrintService(String printerName) {
        if (StringUtils.hasLength(printerName)) {
            for (PrintService printService : PrintServiceLookup.lookupPrintServices(null, null)) {
                if (printService.getName().equalsIgnoreCase(printerName)) {
                    return printService;
                }
            }
        }
        return PrintServiceLookup.lookupDefaultPrintService();
    }

    private int drawMagasinInfoLine(
        Graphics2D graphics2D,
        String text,
        int margin,
        int y,
        int lineHeight,
        int printWidth,
        FontMetrics fontMetrics
    ) {
        if (Objects.nonNull(text) && !text.isBlank()) {
            int x = margin + (printWidth - fontMetrics.stringWidth(text)) / 3;
            graphics2D.drawString(text, x, y);
            y += lineHeight;
        }
        return y;
    }

    @EventListener(ApplicationReadyEvent.class)
    protected void loadDriver() {
        // This method is intentionally left empty as it's not used.
        // The printerJob is initialized in the constructor or getPrinterJob method.
    }
}

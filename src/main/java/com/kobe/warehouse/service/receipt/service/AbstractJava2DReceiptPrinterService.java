package com.kobe.warehouse.service.receipt.service;


import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Printer;
import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import java.awt.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

@Service
public abstract class AbstractJava2DReceiptPrinterService implements Printable {

    private final AppConfigurationService appConfigurationService;
    private final PrinterRepository printerRepository;

    protected Magasin magasin;
    protected Printer printer;

    protected AbstractJava2DReceiptPrinterService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        this.appConfigurationService = appConfigurationService;
        this.printerRepository = printerRepository;
    }

    //Arial
    protected Font getBodyFont() {
        return new Font("Monospaced", Font.PLAIN, 10);
    }

    protected Font getBodyFontBold() {
        return new Font("Monospaced", Font.BOLD, 10);
    }

    protected abstract List<HeaderFooterItem> getHeaderItems();

    protected abstract List<HeaderFooterItem> getFooterItems();

    protected abstract int getNumberOfCopies();

    protected abstract int drawWelcomeMessage(Graphics2D graphics2D, int margin, int y);


    protected abstract int getLineHeight();

    protected abstract List<? extends AbstractItem> getItems();

    protected abstract int drawReglement(Graphics2D graphics2D, int width, int margin, int y, int lineHeight);


    protected int getMaximumLinesPerPage() {
        return this.appConfigurationService.getPrinterItemCount();
    }


    protected int getMargin() {
        return this.appConfigurationService.getPrinterMargin();
    }


    protected int getWidth() {
        return this.appConfigurationService.getPrinterWidth();
    }

    protected abstract Font getHeaderFont();

    protected abstract Font getFooterFont();


    protected abstract int drawTableHeader(Graphics2D graphics2D, int margin, int y);


    protected int drawCompagnyInfo(Graphics2D graphics2D, int margin, int y) {
        magasin = appConfigurationService.getMagasin();
        Font font = getHeaderFont();
        graphics2D.setFont(font);
        int printWidth = getWidth() - (margin * 2);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        int x = margin + (printWidth - fontMetrics.stringWidth(magasin.getName())) / 3;
        graphics2D.drawString(magasin.getName(), x, y);
        y += getLineHeight();
        graphics2D.setFont(getBodyFont());
        fontMetrics = graphics2D.getFontMetrics(font);
        if (Objects.nonNull(magasin.getAddress()) && !magasin.getAddress().isBlank()) {
            x = margin + (printWidth - fontMetrics.stringWidth(magasin.getAddress())) / 3;
            graphics2D.drawString(magasin.getAddress(), x, y);
            y += getLineHeight();
        }
        if (Objects.nonNull(magasin.getEmail()) && !magasin.getEmail().isBlank()) {
            x = margin + (printWidth - fontMetrics.stringWidth(magasin.getEmail())) / 3;
            graphics2D.drawString(magasin.getEmail(), x, y);
            y += getLineHeight();
        }
        if (Objects.nonNull(magasin.getPhone()) && !magasin.getPhone().isBlank()) {
            x = margin + (printWidth - fontMetrics.stringWidth(magasin.getPhone())) / 3;
            graphics2D.drawString(magasin.getPhone(), x, y);
            y += getLineHeight();
        }
        y += getLineHeight();
        return y;
    }


    protected int drawFooter(Graphics2D graphics2D, int margin, int y, int lineHeight) {
        graphics2D.setFont(getBodyFont());
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
        graphics2D.drawLine(margin, y, (int) (width / 1.4), y);
        y += 10;
        return y;
    }

    protected int calculateHeaderHeight(int y) {
        List<HeaderFooterItem> headerItems = getHeaderItems();

        int lineHeight = getLineHeight();
        for (HeaderFooterItem headerFooterItem : headerItems) {
            y += (lineHeight * headerFooterItem.lineBreakNumber());
        }
        return y;
    }

    protected int calculateFooterHeight(int lineHeight) {
        List<HeaderFooterItem> headerItems = getFooterItems();
        int y = 0;
        for (HeaderFooterItem headerFooterItem : headerItems) {
            y += (lineHeight * headerFooterItem.lineBreakNumber());
        }
        return y;
    }

    protected int drawDate(Graphics2D graphics2D,  int margin, int y, int lineHeight) {
        graphics2D.setFont(getBodyFont());
       int x=margin*3;
        graphics2D.drawString("Le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), x, y);
        graphics2D.drawString("à " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), x + 85, y);
        y += lineHeight;
        return y;
    }

    protected void drawThanksMessage(Graphics2D graphics2D, int margin, int y) {
        if (StringUtils.hasLength(magasin.getNote())) {
            graphics2D.setFont(getBodyFont());
            graphics2D.drawString(magasin.getNote(), margin, y);

        }

    }

    protected PrinterJob getPrinterJob(String hostName) throws PrinterException {

        String printerName = StringUtils.hasLength(hostName) ? printerRepository.findByPosteName(hostName).map(Printer::getName).orElse(null) : null;
        PrintService selectedService = null;
        if (StringUtils.hasLength(printerName)) {
            PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
            for (PrintService printService : printServices) {
                if (printService.getName().equalsIgnoreCase(printerName)) {
                    selectedService = printService;
                    break;
                }
            }
        } else {
            selectedService = PrintServiceLookup.lookupDefaultPrintService();
        }
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
        return 410;
    }
}

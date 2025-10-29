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
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public abstract class AbstractJava2DReceiptPrinterService implements Printable {

    protected static final String MONTANT_RENDU = "MONNAIE RENDUE";
    protected static final String MONTANT_TTC = "MONTANT TTC";
    protected static final String REMISE = "REMISE";
    protected static final String TOTAL_TVA = "TOTAL TVA";
    protected static final String TOTAL_A_PAYER = "TOTAL A PAYER";
    protected static final String RESTE_A_PAYER = "RESTE A PAYER";
    protected static final String TVA = "TAXES";
    protected static final String REGLEMENT = "REGLEMENT(S)";
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

    protected abstract List<byte[]> generateTicket() throws IOException;

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

    // ============================================
    // ESC/POS Helper Methods (for thermal printing)
    // ============================================

    /**
     * Initialize printer (ESC @)
     */
    protected void escPosInitialize(java.io.ByteArrayOutputStream out) throws java.io.IOException {
        out.write(new byte[] { 0x1B, 0x40 }); // ESC @
    }

    /**
     * Set text alignment (ESC a n)
     */
    protected void escPosSetAlignment(java.io.ByteArrayOutputStream out, EscPosAlignment alignment) throws java.io.IOException {
        out.write(new byte[] { 0x1B, 0x61, (byte) alignment.code }); // ESC a n
    }

    /**
     * Set bold mode (ESC E n)
     */
    protected void escPosSetBold(java.io.ByteArrayOutputStream out, boolean enable) throws java.io.IOException {
        out.write(new byte[] { 0x1B, 0x45, (byte) (enable ? 1 : 0) }); // ESC E n
    }

    /**
     * Set character size (GS ! n)
     * @param width 1-8 (normal to 8x width)
     * @param height 1-8 (normal to 8x height)
     */
    protected void escPosSetTextSize(java.io.ByteArrayOutputStream out, int width, int height) throws java.io.IOException {
        int size = ((width - 1) << 4) | (height - 1);
        out.write(new byte[] { 0x1D, 0x21, (byte) size }); // GS ! n
    }

    /**
     * Feed n lines (ESC d n)
     */
    protected void escPosFeedLines(java.io.ByteArrayOutputStream out, int lines) throws java.io.IOException {
        out.write(new byte[] { 0x1B, 0x64, (byte) lines }); // ESC d n
    }

    /**
     * Print line with line feed
     * Uses Windows-1252 encoding for French character support
     */
    protected void escPosPrintLine(java.io.ByteArrayOutputStream out, String text) throws java.io.IOException {
        if (text != null) {
            out.write(text.getBytes("Windows-1252")); // CP1252 encoding for French characters
        }
        out.write(0x0A); // LF (Line Feed)
    }

    /**
     * Print separator line
     * @param length number of dashes (typically 48 for 80mm paper, 32 for 58mm)
     */
    protected void escPosPrintSeparator(java.io.ByteArrayOutputStream out, int length) throws java.io.IOException {
        escPosPrintLine(out, "-".repeat(Math.max(0, length)));
    }

    /**
     * Cut paper (GS V A 0)
     * Partial cut - leaves small connection for easy tearing
     */
    protected void escPosCutPaper(java.io.ByteArrayOutputStream out) throws java.io.IOException {
        out.write(new byte[] { 0x1D, 0x56, 0x41, 0x00 }); // GS V A 0 - Partial cut
    }

    /**
     * Set underline mode (ESC - n)
     * @param mode 0=off, 1=1-dot thick, 2=2-dot thick
     */
    protected void escPosSetUnderline(java.io.ByteArrayOutputStream out, int mode) throws java.io.IOException {
        out.write(new byte[] { 0x1B, 0x2D, (byte) mode }); // ESC - n
    }

    /**
     * Set line spacing (ESC 3 n)
     * @param spacing line spacing in dots (default is usually 30)
     */
    protected void escPosSetLineSpacing(java.io.ByteArrayOutputStream out, int spacing) throws java.io.IOException {
        out.write(new byte[] { 0x1B, 0x33, (byte) spacing }); // ESC 3 n
    }

    /**
     * Truncate string to max length
     * Useful for fitting product names in receipt columns
     */
    protected String truncateString(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    /**
     * Pad string to the right with spaces
     */
    protected String padRight(String str, int length) {
        if (str == null) str = "";
        return String.format("%-" + length + "s", str);
    }

    /**
     * Pad string to the left with spaces
     */
    protected String padLeft(String str, int length) {
        if (str == null) str = "";
        return String.format("%" + length + "s", str);
    }

    /**
     * ESC/POS Alignment enumeration
     */
    protected enum EscPosAlignment {
        LEFT(0),
        CENTER(1),
        RIGHT(2);

        final int code;

        EscPosAlignment(int code) {
            this.code = code;
        }
    }
}

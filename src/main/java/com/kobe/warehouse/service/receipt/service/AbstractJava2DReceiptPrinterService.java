package com.kobe.warehouse.service.receipt.service;

import com.fazecast.jSerialComm.SerialPort;
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
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger LOG = LoggerFactory.getLogger(AbstractJava2DReceiptPrinterService.class);
    //38 *21,2
    protected final AppConfigurationService appConfigurationService;
    protected final PrinterRepository printerRepository;
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

    // ============================================
    // Common ESC/POS Header and Footer Methods
    // ============================================

    /**
     * Print common ESC/POS company header (name, address, phone, welcome message)
     * This centralizes the header logic used across all receipt types
     *
     * @param out the output stream
     * @throws IOException if writing fails
     */
    protected void printEscPosCompanyHeader(java.io.ByteArrayOutputStream out) throws java.io.IOException {
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

        // Reset to left alignment for content
        escPosSetAlignment(out, EscPosAlignment.LEFT);
    }

    /**
     * Print common ESC/POS footer (separator, date/time, thank you message, paper cut)
     * This centralizes the footer logic used across all receipt types
     *
     * @param out the output stream
     * @throws IOException if writing fails
     */
    protected void printEscPosFooter(java.io.ByteArrayOutputStream out) throws java.io.IOException {
        printEscPosFooter(out,
            java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
            " " +
            java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        );
    }

    /**
     * Print common ESC/POS footer with custom timestamp
     * This allows subclasses to provide their own timestamp (e.g., sale timestamp)
     *
     * @param out the output stream
     * @param timestamp the formatted timestamp to print
     * @throws IOException if writing fails
     */
    protected void printEscPosFooter(java.io.ByteArrayOutputStream out, String timestamp) throws java.io.IOException {
        // Separator line
        escPosPrintSeparator(out, 48);

        // Date and time
        escPosSetBold(out, false);
        escPosSetAlignment(out, EscPosAlignment.LEFT);
        escPosPrintLine(out, timestamp);
        escPosFeedLines(out, 1);

        // Thank you message (centered)
        if (magasin.getNote() != null && !magasin.getNote().isEmpty()) {
            escPosSetAlignment(out, EscPosAlignment.CENTER);
            escPosPrintLine(out, magasin.getNote());
            escPosSetAlignment(out, EscPosAlignment.LEFT);
        }

        // Cut paper
        escPosFeedLines(out, 3);
        escPosCutPaper(out);
    }

    // ============================================
    // Abstract method for ESC/POS receipt generation
    // ============================================

    /**
     * Generate ESC/POS receipt data
     * Subclasses must implement this method to generate their specific receipt format
     *
     * @param isEdit whether this is an edit print (affects number of copies)
     * @return byte array containing ESC/POS commands
     * @throws IOException if generation fails
     */
    protected abstract byte[] generateEscPosReceipt(boolean isEdit) throws IOException;

    /**
     * Print ESC/POS receipt directly to a thermal printer using Java Print Service
     * This method sends raw ESC/POS bytes directly to the printer without Graphics2D
     *
     * @param printerName the name of the printer (null for default printer)
     * @param isEdit whether this is an edit print (affects number of copies)
     * @throws IOException if ESC/POS generation fails
     * @throws PrintException if printing fails
     */
    public void printEscPosDirect(String printerName, boolean isEdit) throws IOException, PrintException {
        // Generate ESC/POS byte array
        byte[] escPosData = generateEscPosReceipt(isEdit);

        // Get the print service
        PrintService printService = getPrintService(printerName);
        if (printService == null) {
            throw new PrintException("Printer not found: " + printerName);
        }

        // Send raw bytes to printer
        printRawBytesToPrinter(escPosData, printService);

        LOG.info("ESC/POS receipt printed successfully to: {}", printService.getName());
    }

    // ============================================
    // Direct ESC/POS Printing Methods (No Graphics2D)
    // ============================================

    /**
     * Print ESC/POS receipt to a thermal printer via hostname lookup
     *
     * @param hostName the hostname to look up the printer
     * @param isEdit whether this is an edit print
     * @throws IOException if ESC/POS generation fails
     * @throws PrintException if printing fails
     */
    public void printEscPosDirectByHost(String hostName, boolean isEdit) throws IOException, PrintException {
        String printerName = printer != null ? printer.getName() : null;
        if (hostName != null && !hostName.isEmpty()) {
            printer = printerRepository.findByPosteName(hostName).orElse(null);
            printerName = printer != null ? printer.getName() : null;
        }
        printEscPosDirect(printerName, isEdit);
    }

    /**
     * Print ESC/POS receipt directly to a serial port thermal printer
     * Uses jSerialComm library for serial communication
     *
     * @param portName the serial port name (e.g., "COM1" on Windows, "/dev/ttyUSB0" on Linux)
     * @param baudRate the baud rate (typical values: 9600, 19200, 38400, 115200)
     * @param isEdit whether this is an edit print
     * @throws IOException if ESC/POS generation or serial communication fails
     */
    public void printEscPosToSerialPort(String portName, int baudRate, boolean isEdit) throws IOException {
        // Generate ESC/POS byte array
        byte[] escPosData = generateEscPosReceipt(isEdit);

        // Open serial port
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(baudRate);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (!serialPort.openPort()) {
            throw new IOException("Failed to open serial port: " + portName);
        }

        try {
            // Write ESC/POS data to serial port
            int bytesWritten = serialPort.writeBytes(escPosData, escPosData.length);

            if (bytesWritten != escPosData.length) {
                throw new IOException(
                    String.format("Failed to write all bytes to serial port. Written: %d, Expected: %d", bytesWritten, escPosData.length)
                );
            }

            LOG.info("ESC/POS receipt printed successfully to serial port: {} at {} baud", portName, baudRate);
        } finally {
            serialPort.closePort();
        }
    }

    /**
     * Print ESC/POS receipt to a network-connected thermal printer via TCP/IP socket
     *
     * @param ipAddress the printer's IP address
     * @param port the printer's port (typically 9100 for raw printing)
     * @param isEdit whether this is an edit print
     * @throws IOException if ESC/POS generation or network communication fails
     */
    public void printEscPosToNetworkPrinter(String ipAddress, int port, boolean isEdit) throws IOException {
        // Generate ESC/POS byte array
        byte[] escPosData = generateEscPosReceipt(isEdit);

        // Connect to network printer and send data
        try (Socket socket = new Socket(ipAddress, port); OutputStream out = socket.getOutputStream()) {
            out.write(escPosData);
            out.flush();

            LOG.info("ESC/POS receipt printed successfully to network printer: {}:{}", ipAddress, port);
        } catch (IOException e) {
            LOG.error("Failed to print to network printer {}:{} - {}", ipAddress, port, e.getMessage());
            throw new IOException("Failed to print to network printer at " + ipAddress + ":" + port, e);
        }
    }

    /**
     * Helper method to send raw bytes to a printer using Java Print Service
     *
     * @param data the raw byte data to print
     * @param printService the print service to use
     * @throws PrintException if printing fails
     */
    private void printRawBytesToPrinter(byte[] data, PrintService printService) throws PrintException {
        // Create a DocFlavor for raw bytes
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;

        // Create a Doc with the ESC/POS data
        Doc doc = new SimpleDoc(data, flavor, null);

        // Create a print job
        DocPrintJob printJob = printService.createPrintJob();

        // Create print request attributes (empty for raw printing)
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();

        // Print the document
        printJob.print(doc, attributes);
    }

    /**
     * Print ESC/POS receipt with custom configuration
     * This provides a flexible interface for printing with various options
     *
     * @param config the print configuration
     * @throws IOException if ESC/POS generation fails
     * @throws PrintException if printing fails
     */
    public void printEscPosWithConfig(EscPosPrintConfig config) throws IOException, PrintException {
        switch (config.getPrintMethod()) {
            case JAVA_PRINT_SERVICE -> printEscPosDirect(config.getPrinterName(), config.isEdit());
            case SERIAL_PORT -> printEscPosToSerialPort(config.getSerialPort(), config.getBaudRate(), config.isEdit());
            case NETWORK_SOCKET -> printEscPosToNetworkPrinter(config.getIpAddress(), config.getPort(), config.isEdit());
            default -> throw new IllegalArgumentException("Unsupported print method: " + config.getPrintMethod());
        }
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

    /**
     * Configuration class for ESC/POS printing
     */
    public static class EscPosPrintConfig {

        private PrintMethod printMethod;
        private String printerName;
        private String serialPort;
        private int baudRate = 9600;
        private String ipAddress;
        private int port = 9100;
        private boolean isEdit;

        // Fluent builder methods
        public static EscPosPrintConfig forPrintService(String printerName, boolean isEdit) {
            EscPosPrintConfig config = new EscPosPrintConfig();
            config.setPrintMethod(PrintMethod.JAVA_PRINT_SERVICE);
            config.setPrinterName(printerName);
            config.setEdit(isEdit);
            return config;
        }

        public static EscPosPrintConfig forSerialPort(String portName, int baudRate, boolean isEdit) {
            EscPosPrintConfig config = new EscPosPrintConfig();
            config.setPrintMethod(PrintMethod.SERIAL_PORT);
            config.setSerialPort(portName);
            config.setBaudRate(baudRate);
            config.setEdit(isEdit);
            return config;
        }

        public static EscPosPrintConfig forNetworkPrinter(String ipAddress, int port, boolean isEdit) {
            EscPosPrintConfig config = new EscPosPrintConfig();
            config.setPrintMethod(PrintMethod.NETWORK_SOCKET);
            config.setIpAddress(ipAddress);
            config.setPort(port);
            config.setEdit(isEdit);
            return config;
        }

        public PrintMethod getPrintMethod() {
            return printMethod;
        }

        public void setPrintMethod(PrintMethod printMethod) {
            this.printMethod = printMethod;
        }

        public String getPrinterName() {
            return printerName;
        }

        public void setPrinterName(String printerName) {
            this.printerName = printerName;
        }

        public String getSerialPort() {
            return serialPort;
        }

        public void setSerialPort(String serialPort) {
            this.serialPort = serialPort;
        }

        public int getBaudRate() {
            return baudRate;
        }

        public void setBaudRate(int baudRate) {
            this.baudRate = baudRate;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public boolean isEdit() {
            return isEdit;
        }

        public void setEdit(boolean edit) {
            isEdit = edit;
        }

        public enum PrintMethod {
            JAVA_PRINT_SERVICE,
            SERIAL_PORT,
            NETWORK_SOCKET,
        }
    }
}

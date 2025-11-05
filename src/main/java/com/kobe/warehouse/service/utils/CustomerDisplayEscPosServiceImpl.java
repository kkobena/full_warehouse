package com.kobe.warehouse.service.utils;

import com.fazecast.jSerialComm.SerialPort;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.print.Doc;
import javax.print.DocFlavor;
import javax.print.DocPrintJob;
import javax.print.PrintException;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.SimpleDoc;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * ESC/POS implementation of CustomerDisplayService for VFD/Customer Pole Displays
 * Uses ESC/POS commands for better control and standardization
 * Similar architecture to AbstractJava2DReceiptPrinterService
 */
@Service
@Transactional(readOnly = true)
public class CustomerDisplayEscPosServiceImpl implements CustomerDisplayService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerDisplayEscPosServiceImpl.class);
    // ESC/POS display dimensions (typical 20 chars x 2 lines)
    private static final int DISPLAY_WIDTH = 20;
    private static final int DISPLAY_LINES = 2;
    // Serial port configuration
    private static final int DEFAULT_BAUD_RATE = 9600;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = 1;
    private static final int PARITY = SerialPort.NO_PARITY;
    private final RequestOrigineService requestOrigineService;
    private final AppConfigurationService appConfigurationService;
    private final ConnectionType currentConnectionType = ConnectionType.SERIAL;

  /*  @Value("${customer-display.connection-type:SERIAL}")
    private String connectionType;

    @Value("${customer-display.usb-printer-name:}")
    private String usbPrinterName;*/
    @Value("${port-com:}")
    private String portName;
    private SerialPort serialPort;
    private OutputStream outputStream;

    public CustomerDisplayEscPosServiceImpl(RequestOrigineService requestOrigineService, AppConfigurationService appConfigurationService) {
        this.requestOrigineService = requestOrigineService;
        this.appConfigurationService = appConfigurationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        initializeSerialPort();
      /*  switch (currentConnectionType) {
            case SERIAL -> initializeSerialPort();
            case USB_PRINT_SERVICE -> {
                LOG.info("Customer display configured for USB Print Service: {}", usbPrinterName);
                // No persistent connection needed for print service
            }
            case NETWORK -> LOG.info("Customer display configured for network connection");
        }*/

        // Clear display and show welcome message on startup
        clearDisplay();
        welcomeMessage();
    }

    // ============================================
    // ESC/POS Helper Methods for Customer Display
    // ============================================

    /**
     * Initialize display (ESC @)
     * Resets the display to default state
     */
    private void escPosInitialize(ByteArrayOutputStream out) throws IOException {
        out.write(new byte[]{0x1B, 0x40}); // ESC @
    }

    /**
     * Clear display (CLR command)
     * Different displays may use different commands:
     * - Some use FF (0x0C)
     * - Some use ESC q (0x1B, 0x71)
     * - Some use CLR (0x0C)
     */
    private void escPosClearDisplay(ByteArrayOutputStream out) throws IOException {
        // Try standard form feed (most common)
        out.write(0x0C); // FF (Form Feed) - clears screen

        // Alternative: Some displays use ESC q
        // out.write(new byte[]{0x1B, 0x71}); // ESC q
    }

    /**
     * Move cursor to home position (top-left)
     * ESC H or CR (Carriage Return)
     */
    private void escPosMoveCursorHome(ByteArrayOutputStream out) throws IOException {
        out.write(new byte[]{0x1B, 0x48}); // ESC H
        // Alternative: out.write(0x0D); // CR
    }

    /**
     * Move cursor to specific position
     * ESC l (line) - Move to line (0-based)
     *
     * @param line line number (0 = top line, 1 = bottom line)
     */
    private void escPosMoveCursorToLine(ByteArrayOutputStream out, int line) throws IOException {
        if (line < 0 || line >= DISPLAY_LINES) {
            line = 0;
        }
        // Move cursor to beginning of specified line
        // Different displays use different commands:
        // ESC l (0x1B, 0x6C) for some displays
        out.write(new byte[]{0x1B, 0x6C, (byte) line}); // ESC l n
    }

    /**
     * Set cursor position (row, column)
     * ESC Y row col (for some displays)
     *
     * @param row row (0-1 for 2-line display)
     * @param col column (0-19 for 20-char display)
     */
    private void escPosSetCursorPosition(ByteArrayOutputStream out, int row, int col) throws IOException {
        // ESC Y command (used by some VFD displays)
        // Format: ESC Y row col
        out.write(new byte[]{0x1B, 0x59, (byte) row, (byte) col});

        // Alternative for displays that use different positioning:
        // Some displays use: ESC $ nL nH (horizontal position)
        // out.write(new byte[]{0x1B, 0x24, (byte)(col & 0xFF), (byte)((col >> 8) & 0xFF)});
    }

    /**
     * Set brightness level (if supported)
     * ESC * n (where n = 1-4, 4 = brightest)
     *
     * @param level brightness level (1-4)
     */
    private void escPosSetBrightness(ByteArrayOutputStream out, int level) throws IOException {
        if (level < 1) level = 1;
        if (level > 4) level = 4;
        out.write(new byte[]{0x1B, 0x2A, (byte) level}); // ESC * n
    }

    /**
     * Enable/disable cursor display
     * ESC C n (n=0: hide, n=1: show)
     */
    private void escPosSetCursorVisibility(ByteArrayOutputStream out, boolean visible) throws IOException {
        out.write(new byte[]{0x1B, 0x43, (byte) (visible ? 1 : 0)}); // ESC C n
    }

    /**
     * Enable/disable cursor blink
     * ESC B n (n=0: no blink, n=1: blink)
     */
    private void escPosSetCursorBlink(ByteArrayOutputStream out, boolean blink) throws IOException {
        out.write(new byte[]{0x1B, 0x42, (byte) (blink ? 1 : 0)}); // ESC B n
    }

    /**
     * Scroll display left
     */
    private void escPosScrollLeft(ByteArrayOutputStream out) throws IOException {
        out.write(new byte[]{0x1B, 0x73}); // ESC s
    }

    /**
     * Scroll display right
     */
    private void escPosScrollRight(ByteArrayOutputStream out) throws IOException {
        out.write(new byte[]{0x1B, 0x74}); // ESC t
    }

    /**
     * Set horizontal scroll speed (if supported)
     */
    private void escPosSetScrollSpeed(ByteArrayOutputStream out, int speed) throws IOException {
        // Speed typically 0-7 (0=fastest)
        if (speed < 0) speed = 0;
        if (speed > 7) speed = 7;
        out.write(new byte[]{0x1B, 0x55, (byte) speed}); // ESC U n
    }

    /**
     * Write text to display (Windows-1252 encoding for French characters)
     */
    private void escPosWriteText(ByteArrayOutputStream out, String text) throws IOException {
        if (text != null && !text.isEmpty()) {
            out.write(text.getBytes("Windows-1252")); // CP1252 for French characters
        }
    }

    /**
     * Write text with line feed
     */
    private void escPosWriteLine(ByteArrayOutputStream out, String text) throws IOException {
        escPosWriteText(out, text);
        out.write(0x0A); // LF (Line Feed)
    }

    /**
     * Pad string to display width (20 chars)
     *
     * @param str       the string to pad
     * @param alignment "left", "center", "right", or "begin" (right-align)
     * @return padded string
     */
    private String padToDisplayWidth(String str, String alignment) {
        if (str == null) {
            str = "";
        }

        // Truncate if too long
        if (str.length() > DISPLAY_WIDTH) {
            str = str.substring(0, DISPLAY_WIDTH);
        }

        int padding = DISPLAY_WIDTH - str.length();
        if (padding <= 0) {
            return str;
        }

        String spaces = " ".repeat(padding);

        return switch (alignment.toLowerCase()) {
            case "left", "end" -> str + spaces;
            case "right", "begin" -> spaces + str;
            case "center" -> {
                int leftPad = padding / 2;
                int rightPad = padding - leftPad;
                yield " ".repeat(leftPad) + str + " ".repeat(rightPad);
            }
            default -> str + spaces; // Default to left alignment
        };
    }

    /**
     * Truncate string to fit display width
     */
    private String truncateToDisplayWidth(String str) {
        if (str == null) {
            return "";
        }
        return str.length() > DISPLAY_WIDTH ? str.substring(0, DISPLAY_WIDTH) : str;
    }

    // ============================================
    // Serial Port Management
    // ============================================

    /**
     * Initialize and configure serial port
     */
    private void initializeSerialPort() {
        try {
            if (!isDisplayEnabled()) {
                return;
            }
            if (StringUtils.hasLength(portName)) {
                serialPort = SerialPort.getCommPort(portName);
                serialPort.setBaudRate(DEFAULT_BAUD_RATE);
                serialPort.setNumDataBits(DATA_BITS);
                serialPort.setNumStopBits(STOP_BITS);
                serialPort.setParity(PARITY);
                serialPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

                openSerialPort();

                if (serialPort.isOpen()) {
                    outputStream = serialPort.getOutputStream();
                    LOG.info("Customer display initialized on port: {} at {} baud", portName, DEFAULT_BAUD_RATE);
                } else {
                    LOG.warn("Failed to open customer display port: {}", portName);
                }
            } else {
                LOG.info("Customer display port not configured");
            }
        } catch (Exception e) {
            LOG.error("Error initializing customer display serial port", e);
        }
    }

    /**
     * Open serial port if not already open
     */
    private void openSerialPort() {
        if (Objects.nonNull(serialPort) && !serialPort.isOpen()) {
            boolean opened = serialPort.openPort();
            if (!opened) {
                LOG.error("Failed to open serial port: {}", portName);
            }
        }
    }

    /**
     * Close serial port
     */
    private void closeSerialPort() {
        try {
            if (Objects.nonNull(serialPort) && serialPort.isOpen()) {
                if (Objects.nonNull(outputStream)) {
                    outputStream.close();
                }
                serialPort.closePort();
                LOG.info("Customer display serial port closed");
            }
        } catch (Exception e) {
            LOG.error("Error closing serial port", e);
        }
    }

    /**
     * Send ESC/POS commands to the display (routes to appropriate connection method)
     */
    private void sendEscPosCommands(ByteArrayOutputStream commands) {
        if (!handleRequest()) {
            LOG.debug("Customer display is disabled");
            return;
        }

        try {
            byte[] data = commands.toByteArray();
            sendToSerialPort(data);
          /*  switch (currentConnectionType) {
                case SERIAL -> sendToSerialPort(data);
                case USB_PRINT_SERVICE -> sendToUsbPrintService(data);
                case NETWORK -> {
                    LOG.warn("Network connection not configured for auto-send, use explicit network methods");
                }
                default -> LOG.error("Unknown connection type: {}", currentConnectionType);
            }*/

            if (LOG.isDebugEnabled()) {
                LOG.debug("Sent {} bytes to customer display via {}", data.length, currentConnectionType);
            }
        } catch (Exception e) {
            LOG.error("Error sending data to customer display", e);
        }
    }

    /**
     * Send data to serial port
     */
    private void sendToSerialPort(byte[] data) throws IOException {
        if (!ensurePortOpen()) {
            throw new IOException("Serial port not available");
        }

        outputStream.write(data);
        outputStream.flush();
    }

   /*
    private void sendToUsbPrintService(byte[] data) throws PrintException {
        PrintService printService = getUsbPrintService(usbPrinterName);
        if (printService == null) {
            throw new PrintException("USB printer/display not found: " + usbPrinterName);
        }

        sendRawBytesToPrintService(data, printService);
    }*/

    /**
     * Get USB print service by name
     */
    private PrintService getUsbPrintService(String printerName) {
        if (StringUtils.hasLength(printerName)) {
            for (PrintService printService : PrintServiceLookup.lookupPrintServices(null, null)) {
                if (printService.getName().equalsIgnoreCase(printerName)) {
                    LOG.debug("Found USB print service: {}", printService.getName());
                    return printService;
                }
            }
        }
        // Return default print service if no name specified
        PrintService defaultService = PrintServiceLookup.lookupDefaultPrintService();
        if (defaultService != null) {
            LOG.debug("Using default print service: {}", defaultService.getName());
        }
        return defaultService;
    }

    /**
     * Send raw bytes to print service (USB or other)
     */
    private void sendRawBytesToPrintService(byte[] data, PrintService printService) throws PrintException {
        DocFlavor flavor = DocFlavor.BYTE_ARRAY.AUTOSENSE;
        Doc doc = new SimpleDoc(data, flavor, null);
        DocPrintJob printJob = printService.createPrintJob();
        PrintRequestAttributeSet attributes = new HashPrintRequestAttributeSet();
        printJob.print(doc, attributes);
    }

    /**
     * Ensure serial port is open and ready
     */
    private boolean ensurePortOpen() {
        if (!StringUtils.hasLength(portName)) {
            LOG.debug("Customer display port not configured");
            return false;
        }

        if (Objects.isNull(serialPort)) {
            initializeSerialPort();
        }

        if (Objects.isNull(serialPort) || !serialPort.isOpen()) {
            openSerialPort();
        }

        if (Objects.isNull(outputStream)) {
            try {
                outputStream = serialPort.getOutputStream();
            } catch (Exception e) {
                LOG.error("Failed to get output stream", e);
                return false;
            }
        }

        return Objects.nonNull(serialPort) && serialPort.isOpen() && Objects.nonNull(outputStream);
    }

    /**
     * Check if customer display is enabled in configuration
     */
    private boolean isDisplayEnabled() {
        return this.appConfigurationService.isCustomerDisplayActif();
    }

    // ============================================
    // CustomerDisplayService Interface Implementation
    // ============================================

    /**
     * Clear the entire display
     */
    public void clearDisplay() {
        if (!handleRequest()) {
            return;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            escPosClearDisplay(out);
            escPosMoveCursorHome(out);
            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error clearing display", e);
        }
    }

    @Override
    public void sendDataToAfficheurPos(String data) {
        sendDataToAfficheurPos(data, "left");
    }

    @Override
    public void sendDataToAfficheurPos(String data, String position) {
        if (!handleRequest()) {
            return;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Move cursor to line 2 (bottom line)
            escPosSetCursorPosition(out, 1, 0);

            // Pad data according to position
            String paddedData = padToDisplayWidth(data, position);

            // Write padded text
            escPosWriteText(out, paddedData);

            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error sending data to customer display", e);
        }
    }

    @Override
    public void displaySalesData(String produitName, int qty, int price) {
        if (!handleRequest()) {
            return;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Line 1: Product name (truncated to 20 chars, uppercase)
            escPosSetCursorPosition(out, 0, 0);
            String productLine = truncateToDisplayWidth(produitName.toUpperCase());
            escPosWriteText(out, padToDisplayWidth(productLine, "left"));

            // Line 2: Quantity * Price = Total (right-aligned)
            escPosSetCursorPosition(out, 1, 0);
            String priceLine = qty + "*" + NumberUtil.formatToString(price) + "=" + NumberUtil.formatToString(qty * price);
            escPosWriteText(out, padToDisplayWidth(priceLine, "begin"));

            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error displaying sales data", e);
        }
    }

    @Override
    public void welcomeMessage() {
        if (!handleRequest()) {
            return;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Clear display first
            escPosClearDisplay(out);

            // Line 1: Store name (centered)
            escPosSetCursorPosition(out, 0, 0);
            String magasinName = appConfigurationService.getMagasin().getName();
            escPosWriteText(out, padToDisplayWidth(truncateToDisplayWidth(magasinName), "center"));

            // Line 2: Welcome message (centered)
            escPosSetCursorPosition(out, 1, 0);
            escPosWriteText(out, padToDisplayWidth("BIENVENUE A VOUS", "center"));

            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error displaying welcome message", e);
        }
    }

    @Override
    public void connectedUserMessage(String message) {
        if (!handleRequest()) {
            return;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Line 2: Cash register info
            escPosSetCursorPosition(out, 1, 0);
            String caisseMessage = "Caisse: " + message.toUpperCase();
            escPosWriteText(out, padToDisplayWidth(truncateToDisplayWidth(caisseMessage), "left"));

            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error displaying connected user message", e);
        }
    }

    @Override
    public void displaySaleTotal(int total) {
        if (!handleRequest()) {
            return;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Line 1: "NET A PAYER:"
            escPosSetCursorPosition(out, 0, 0);
            escPosWriteText(out, padToDisplayWidth("NET A PAYER:", "left"));

            // Line 2: Total amount (right-aligned)
            escPosSetCursorPosition(out, 1, 0);
            String totalStr = NumberUtil.formatToString(total);
            escPosWriteText(out, padToDisplayWidth(totalStr, "begin"));

            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error displaying sale total", e);
        }
    }

    @Override
    public void displayMonnaie(int total) {
        try {
            if (!handleRequest()) {
                return;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Line 1: "MONNAIE:"
            escPosSetCursorPosition(out, 0, 0);
            escPosWriteText(out, padToDisplayWidth("MONNAIE:", "left"));

            // Line 2: Change amount (right-aligned)
            escPosSetCursorPosition(out, 1, 0);
            String monnaieLine = NumberUtil.formatToString(total);
            escPosWriteText(out, padToDisplayWidth(monnaieLine, "begin"));

            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error displaying change amount", e);
        }
    }

    // ============================================
    // Additional Utility Methods
    // ============================================

    /**
     * Display two lines of text with custom alignment
     *
     * @param line1      first line text
     * @param line2      second line text
     * @param alignment1 alignment for line 1 ("left", "center", "right")
     * @param alignment2 alignment for line 2
     */
    public void displayTwoLines(String line1, String line2, String alignment1, String alignment2) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            // Clear display
            escPosClearDisplay(out);

            // Line 1
            escPosSetCursorPosition(out, 0, 0);
            escPosWriteText(out, padToDisplayWidth(truncateToDisplayWidth(line1), alignment1));

            // Line 2
            escPosSetCursorPosition(out, 1, 0);
            escPosWriteText(out, padToDisplayWidth(truncateToDisplayWidth(line2), alignment2));

            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error displaying two lines", e);
        }
    }

    /**
     * Set display brightness (if supported by hardware)
     *
     * @param level brightness level (1-4, 4 = brightest)
     */
    public void setBrightness(int level) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            escPosSetBrightness(out, level);
            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error setting brightness", e);
        }
    }

    /**
     * Reset display to default state
     */
    public void resetDisplay() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            escPosInitialize(out);
            escPosClearDisplay(out);
            escPosSetCursorVisibility(out, false); // Hide cursor
            sendEscPosCommands(out);
        } catch (IOException e) {
            LOG.error("Error resetting display", e);
        }
    }

    /**
     * Display scrolling message (if supported)
     *
     * @param message message to scroll
     */
    public void displayScrollingMessage(String message) {
        // This would require multiple updates to simulate scrolling
        // Implementation depends on display capabilities
        LOG.info("Scrolling message: {}", message);

        // Simple implementation: just display truncated message
        displayTwoLines(message, "", "left", "left");
    }

    // ============================================
    // USB and Multi-Connection Methods
    // ============================================

    /**
     * Send data to USB display using Print Service with custom printer name
     *
     * @param data        ESC/POS data to send
     * @param printerName USB printer/display name
     * @throws PrintException if printing fails
     */
    public void sendToUsbDisplay(byte[] data, String printerName) throws PrintException {
        PrintService printService = getUsbPrintService(printerName);
        if (printService == null) {
            throw new PrintException("USB display not found: " + printerName);
        }
        sendRawBytesToPrintService(data, printService);
        LOG.info("Data sent to USB display: {}", printerName);
    }

    /**
     * Send data to network-connected customer display via TCP/IP socket
     *
     * @param data      ESC/POS data to send
     * @param ipAddress display IP address
     * @param port      display port (typically 9100)
     * @throws IOException if network communication fails
     */
    public void sendToNetworkDisplay(byte[] data, String ipAddress, int port) throws IOException {
        try (Socket socket = new Socket(ipAddress, port);
             OutputStream out = socket.getOutputStream()) {
            out.write(data);
            out.flush();
            LOG.info("Data sent to network display: {}:{}", ipAddress, port);
        } catch (IOException e) {
            LOG.error("Failed to send to network display {}:{}", ipAddress, port, e);
            throw e;
        }
    }

    /**
     * Send data using custom configuration
     *
     * @param data   ESC/POS data to send
     * @param config connection configuration
     * @throws Exception if sending fails
     */
    public void sendWithConfig(byte[] data, DisplayConnectionConfig config) throws Exception {
        switch (config.getConnectionType()) {
            case SERIAL -> {
                if (config.getSerialPort() != null) {
                    sendToSerialPortDirect(data, config.getSerialPort(), config.getBaudRate());
                } else {
                    sendToSerialPort(data);
                }
            }
            case USB_PRINT_SERVICE -> sendToUsbDisplay(data, config.getUsbPrinterName());
            case NETWORK -> sendToNetworkDisplay(data, config.getIpAddress(), config.getPort());
            default -> throw new IllegalArgumentException("Unsupported connection type: " + config.getConnectionType());
        }
    }

    /**
     * Send data directly to a serial port (alternative to default port)
     *
     * @param data     ESC/POS data
     * @param portName serial port name
     * @param baudRate baud rate
     * @throws IOException if serial communication fails
     */
    private void sendToSerialPortDirect(byte[] data, String portName, int baudRate) throws IOException {
        SerialPort tempPort = SerialPort.getCommPort(portName);
        tempPort.setBaudRate(baudRate);
        tempPort.setNumDataBits(DATA_BITS);
        tempPort.setNumStopBits(STOP_BITS);
        tempPort.setParity(PARITY);
        tempPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING, 0, 0);

        if (!tempPort.openPort()) {
            throw new IOException("Failed to open serial port: " + portName);
        }

        try {
            int bytesWritten = tempPort.writeBytes(data, data.length);
            if (bytesWritten != data.length) {
                throw new IOException(
                    String.format("Failed to write all bytes. Written: %d, Expected: %d", bytesWritten, data.length)
                );
            }
            LOG.info("Data sent to serial port: {} at {} baud", portName, baudRate);
        } finally {
            tempPort.closePort();
        }
    }

    public List<String> listAvailableSerialPorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        List<String> portNames = Arrays.stream(ports)
            .map(SerialPort::getSystemPortName)
            .toList();

        LOG.info("Found {} serial ports: {}", portNames.size(), portNames);
        return portNames;
    }

    /**
     * Test connection to the customer display
     *
     * @return true if connection successful
     */
    public boolean testConnection() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            escPosInitialize(out);
            sendEscPosCommands(out);
            LOG.info("Connection test successful for {}", currentConnectionType);
            return true;
        } catch (Exception e) {
            LOG.error("Connection test failed for {}", currentConnectionType, e);
            return false;
        }
    }


    private boolean handleRequest() {
        return isDisplayEnabled() && requestOrigineService.isLocalAndNotTauriRequest();
    }


    /**
     * Connection type enumeration
     */
    public enum ConnectionType {
        /**
         * Serial port connection (COM port, USB-to-Serial)
         */
        SERIAL,

        /**
         * USB connection via Java Print Service
         * For displays that register as USB printers
         */
        USB_PRINT_SERVICE,

        /**
         * Network connection via TCP/IP socket
         */
        NETWORK
    }

    public static class DisplayConnectionConfig {
        private ConnectionType connectionType;
        private String serialPort;
        private int baudRate = DEFAULT_BAUD_RATE;
        private String usbPrinterName;
        private String ipAddress;
        private int port = 9100;

        // Fluent builder methods

        public static DisplayConnectionConfig forSerialPort(String portName, int baudRate) {
            DisplayConnectionConfig config = new DisplayConnectionConfig();
            config.setConnectionType(ConnectionType.SERIAL);
            config.setSerialPort(portName);
            config.setBaudRate(baudRate);
            return config;
        }

        public static DisplayConnectionConfig forSerialPort(String portName) {
            return forSerialPort(portName, DEFAULT_BAUD_RATE);
        }

        public static DisplayConnectionConfig forUsbPrintService(String printerName) {
            DisplayConnectionConfig config = new DisplayConnectionConfig();
            config.setConnectionType(ConnectionType.USB_PRINT_SERVICE);
            config.setUsbPrinterName(printerName);
            return config;
        }

        public static DisplayConnectionConfig forNetworkDisplay(String ipAddress, int port) {
            DisplayConnectionConfig config = new DisplayConnectionConfig();
            config.setConnectionType(ConnectionType.NETWORK);
            config.setIpAddress(ipAddress);
            config.setPort(port);
            return config;
        }

        // Getters and setters

        public ConnectionType getConnectionType() {
            return connectionType;
        }

        public void setConnectionType(ConnectionType connectionType) {
            this.connectionType = connectionType;
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

        public String getUsbPrinterName() {
            return usbPrinterName;
        }

        public void setUsbPrinterName(String usbPrinterName) {
            this.usbPrinterName = usbPrinterName;
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
    }
}

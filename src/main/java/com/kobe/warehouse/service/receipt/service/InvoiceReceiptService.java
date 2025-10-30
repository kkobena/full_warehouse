package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentReceiptDTO;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.print.PrintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class InvoiceReceiptService extends ReglementAbstractReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceReceiptService.class);
    private InvoicePaymentReceiptDTO invoicePaymentReceipt;

    public InvoiceReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        Font font = PLAIN_FONT;
        headerItems.add(new HeaderFooterItem("ORGANISME: " + invoicePaymentReceipt.getOrganisme(), 1, font));
        headerItems.add(new HeaderFooterItem("FACTURE#: " + invoicePaymentReceipt.getCodeFacture(), 1, font));
        headerItems.add(new HeaderFooterItem("OPERATEUR: " + invoicePaymentReceipt.getUser(), 1, font));

        return headerItems;
    }

    @Override
    protected int drawSummary(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        int rightMargin = getRightMargin();

        graphics2D.setFont(PLAIN_FONT);
        graphics2D.drawString(NOMBRE_DOSSIER, margin, y);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        String nbre = invoicePaymentReceipt.getInvoicePaymentItemsCount();
        graphics2D.drawString(nbre, rightMargin - fontMetrics.stringWidth(nbre), y);
        y += lineHeight;

        graphics2D.setFont(BOLD_FONT);
        graphics2D.drawString(MONTANT_ATTENDU, margin, y);
        fontMetrics = graphics2D.getFontMetrics(BOLD_FONT);
        String montantAttendu = invoicePaymentReceipt.getMontantAttendu();
        graphics2D.drawString(montantAttendu, rightMargin - fontMetrics.stringWidth(montantAttendu), y);
        y += lineHeight;

        /*  graphics2D.setFont(bodyFont);
        graphics2D.drawString(MONTANT_PAYE, margin, y);
        graphics2D.setFont(bodyFontBold);
         fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
        String montantPaye = invoicePaymentReceipt.getPaidAmount();
        graphics2D.drawString(montantPaye, rightMargin - fontMetrics.stringWidth(montantPaye), y);
        y += lineHeight;

        graphics2D.setFont(bodyFont);
        graphics2D.drawString(RESTE_A_PAYER, margin, y);
        graphics2D.setFont(bodyFontBold);
        fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
        String rest = invoicePaymentReceipt.getMontantRestant();
        graphics2D.drawString(rest, rightMargin - fontMetrics.stringWidth(rest), y);
        y += lineHeight;*/

        return y;
    }

    @Override
    protected List<HeaderFooterItem> getFooterItems() {
        return List.of();
    }

    @Override
    protected int drawReglement(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        int rightMargin = getRightMargin();
        graphics2D.setFont(BOLD_FONT);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        /* String reglement = REGLEMENT;
        underlineText(graphics2D, reglement, width, margin, y);
        y += 10;
        drawAndCenterText(graphics2D, reglement, width, margin, y);
        y += 5;
        underlineText(graphics2D, reglement, width, margin, y);
        y += 10;*/

        String libelle = invoicePaymentReceipt.getPaymentMode();
        String paidAmount = invoicePaymentReceipt.getPaidAmount();
        graphics2D.drawString(libelle, margin, y);
        graphics2D.drawString(paidAmount, rightMargin - fontMetrics.stringWidth(paidAmount), y);
        y += lineHeight;

        if (StringUtils.hasLength(invoicePaymentReceipt.getMontantRestant())) {
            graphics2D.drawString(RESTE_A_PAYER, margin, y);
            String rest = invoicePaymentReceipt.getMontantRestant();
            graphics2D.drawString(rest, rightMargin - fontMetrics.stringWidth(rest), y);
            y += lineHeight;
        }

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

            // Header items (left aligned)
            escPosSetAlignment(out, EscPosAlignment.LEFT);
            escPosPrintLine(out, "ORGANISME: " + invoicePaymentReceipt.getOrganisme());
            escPosPrintLine(out, "FACTURE#: " + invoicePaymentReceipt.getCodeFacture());
            escPosPrintLine(out, "OPERATEUR: " + invoicePaymentReceipt.getUser());
            escPosFeedLines(out, 1);

            // Separator line
            escPosPrintSeparator(out, 48);

            // Summary section
            escPosSetBold(out, false);
            escPosPrintLine(out, String.format("%-37s %10s", NOMBRE_DOSSIER, invoicePaymentReceipt.getInvoicePaymentItemsCount()));

            escPosSetBold(out, true);
            escPosPrintLine(out, String.format("%-37s %10s", MONTANT_ATTENDU, invoicePaymentReceipt.getMontantAttendu()));
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);

            // Payment section
            escPosSetBold(out, true);
            escPosPrintLine(
                out,
                String.format("%-37s %10s", invoicePaymentReceipt.getPaymentMode(), invoicePaymentReceipt.getPaidAmount())
            );
            escPosSetBold(out, false);

            // Remaining amount (if any)
            if (StringUtils.hasLength(invoicePaymentReceipt.getMontantRestant())) {
                escPosSetBold(out, true);
                escPosPrintLine(out, String.format("%-37s %10s", RESTE_A_PAYER, invoicePaymentReceipt.getMontantRestant()));
                escPosSetBold(out, false);
            }
            escPosFeedLines(out, 1);

            // Separator line
            escPosPrintSeparator(out, 48);

            // Date and time
            escPosPrintLine(
                out,
                java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " " +
                java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
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
            throw new IOException("Failed to generate ESC/POS receipt: " + e.getMessage(), e);
        } finally {
            out.close();
        }
    }

    @Override
    protected int drawCashInfo(Graphics2D graphics2D, int margin, int y, int lineHeight) {
        return y;
    }

    public void printReceipt(String hostName, InvoicePaymentReceiptDTO invoicePaymentReceipt) {
        this.invoicePaymentReceipt = invoicePaymentReceipt;
        try {
            // Use direct ESC/POS printing for better performance and reliability
            printEscPosDirectByHost(hostName, false);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS receipt: {}", e.getMessage(), e);
        }
    }
}

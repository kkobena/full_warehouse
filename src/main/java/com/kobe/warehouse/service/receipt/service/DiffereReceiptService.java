package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereReceiptDTO;
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

@Service
public class DiffereReceiptService extends ReglementAbstractReceiptService {

    protected static final String SOLDE = "SOLDE";
    private static final Logger LOG = LoggerFactory.getLogger(DiffereReceiptService.class);
    private ReglementDiffereReceiptDTO differeReceipt;

    public DiffereReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();

        headerItems.add(new HeaderFooterItem("CLIENT: " + differeReceipt.customerfullName(), 1, PLAIN_FONT));
        headerItems.add(new HeaderFooterItem("OPERATEUR: " + differeReceipt.userfullName(), 1, PLAIN_FONT));

        return headerItems;
    }

    @Override
    protected int drawSummary(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        int rightMargin = getRightMargin();

        graphics2D.setFont(BOLD_FONT);
        graphics2D.drawString(MONTANT_ATTENDU, margin, y);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        String montantAttendu = differeReceipt.formattedExpectedAmount();
        graphics2D.drawString(montantAttendu, rightMargin - fontMetrics.stringWidth(montantAttendu), y);
        y += lineHeight;
        return y;
    }

    @Override
    protected List<HeaderFooterItem> getFooterItems() {
        return List.of();
    }

    @Override
    protected int drawReglement(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        int rightMargin = getRightMargin();
        graphics2D.setFont(PLAIN_FONT);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        /* String reglement = REGLEMENT;
        underlineText(graphics2D, reglement, width, margin, y);
        y += 10;
        drawAndCenterText(graphics2D, reglement, width, margin, y);
        y += 5;
        underlineText(graphics2D, reglement, width, margin, y);
        y += 10;*/

        String libelle = differeReceipt.libelleMode();
        String paidAmount = differeReceipt.formattedPaidAmount();
        graphics2D.setFont(BOLD_FONT);
        graphics2D.drawString(libelle, margin, y);
        graphics2D.drawString(paidAmount, rightMargin - fontMetrics.stringWidth(paidAmount), y);
        y += lineHeight;
        graphics2D.drawString(SOLDE, margin, y);
        String rest = differeReceipt.formattedSolde();
        graphics2D.drawString(rest, rightMargin - fontMetrics.stringWidth(rest), y);
        y += lineHeight;

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
            escPosPrintLine(out, "CLIENT: " + differeReceipt.customerfullName());
            escPosPrintLine(out, "OPERATEUR: " + differeReceipt.userfullName());
            escPosFeedLines(out, 1);

            // Separator line
            escPosPrintSeparator(out, 48);

            // Summary section
            escPosSetBold(out, true);
            escPosPrintLine(out, String.format("%-37s %10s", MONTANT_ATTENDU, differeReceipt.formattedExpectedAmount()));
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);

            // Payment section
            escPosSetBold(out, true);
            escPosPrintLine(out, String.format("%-37s %10s", differeReceipt.libelleMode(), differeReceipt.formattedPaidAmount()));

            // Solde (balance)
            escPosPrintLine(out, String.format("%-37s %10s", SOLDE, differeReceipt.formattedSolde()));
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);

            // Cash change (if any)
            if (!differeReceipt.monnaie().isEmpty()) {
                escPosSetBold(out, false);
                escPosPrintLine(out, String.format("%-37s %10s", MONTANT_RENDU, differeReceipt.monnaie()));
                escPosFeedLines(out, 1);
            }

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
        if (!differeReceipt.monnaie().isEmpty()) {
            int rightMargin = getRightMargin();
            graphics2D.setFont(PLAIN_FONT);
            graphics2D.drawString(MONTANT_RENDU, margin, y);
            FontMetrics fontMetrics = graphics2D.getFontMetrics();
            String montantRendu = differeReceipt.monnaie();
            graphics2D.drawString(montantRendu, rightMargin - fontMetrics.stringWidth(montantRendu), y);
            y += lineHeight;
        }
        return y;
    }

    public void printReceipt(String hostName, ReglementDiffereReceiptDTO reglementDiffereReceipt) {
        this.differeReceipt = reglementDiffereReceipt;
        try {
            // Use direct ESC/POS printing for better performance and reliability
            printEscPosDirectByHost(hostName, false);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS receipt: {}", e.getMessage(), e);
        }
    }
}

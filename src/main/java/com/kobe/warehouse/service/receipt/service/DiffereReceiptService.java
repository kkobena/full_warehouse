package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereReceiptDTO;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DiffereReceiptService extends ReglementAbstractReceiptService {

    protected static final String SOLDE = "Solde";
    private static final Logger LOG = LoggerFactory.getLogger(DiffereReceiptService.class);
    private ReglementDiffereReceiptDTO differeReceipt;

    public DiffereReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();

        headerItems.add(new HeaderFooterItem("Client: " + differeReceipt.customerfullName(), 1, PLAIN_FONT));
        headerItems.add(new HeaderFooterItem("Opérateur: " + differeReceipt.userfullName(), 1, PLAIN_FONT));

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
            print(hostName);
        } catch (PrinterException e) {
            LOG.error("Error while printing receipt: {}", e.getMessage());
        }
    }
}

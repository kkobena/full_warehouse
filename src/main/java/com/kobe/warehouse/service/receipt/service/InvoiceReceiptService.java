package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentReceiptDTO;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;
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
        headerItems.add(new HeaderFooterItem("Organisme: " + invoicePaymentReceipt.getOrganisme(), 1, font));
        headerItems.add(new HeaderFooterItem("Facture#: " + invoicePaymentReceipt.getCodeFacture(), 1, font));
        headerItems.add(new HeaderFooterItem("Opérateur: " + invoicePaymentReceipt.getUser(), 1, font));

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
    protected int drawCashInfo(Graphics2D graphics2D, int margin, int y, int lineHeight) {
        return y;
    }

    public void printReceipt(String hostName, InvoicePaymentReceiptDTO invoicePaymentReceipt) {
        this.invoicePaymentReceipt = invoicePaymentReceipt;
        try {
            print(hostName);
        } catch (PrinterException e) {
            LOG.error("Error while printing receipt: {}", e.getMessage());
        }
    }
}

package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereReceiptDTO;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiffereReceiptService extends ReglementAbstractReceiptService {
    protected static final String SOLDE = "Solde";
    private static final Logger LOG = LoggerFactory.getLogger(DiffereReceiptService.class);
    private final ReglementDiffereService reglementDiffereService;
    private ReglementDiffereReceiptDTO differeReceipt;

    public DiffereReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository, ReglementDiffereService reglementDiffereService) {
        super(appConfigurationService, printerRepository);
        this.reglementDiffereService = reglementDiffereService;
    }

    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        Font font = getBodyFont();
        headerItems.add(new HeaderFooterItem("Client: " + differeReceipt.customerfullName(), 1, font));
        headerItems.add(new HeaderFooterItem("Opérateur: " + differeReceipt.userfullName(), 1, font));

        return headerItems;
    }

    @Override
    protected int drawSummary(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {

        int rightMargin = getRightMargin();
        Font bodyFont = getBodyFont();
        Font bodyFontBold = getBodyFontBold();


        graphics2D.setFont(bodyFont);
        graphics2D.drawString(MONTANT_ATTENDU, margin, y);
        FontMetrics fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
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

        Font bodyFont = getBodyFont();
        Font bodyFontBold = getBodyFontBold();
        int rightMargin = getRightMargin();
        graphics2D.setFont(bodyFont);
        FontMetrics fontMetrics;
        String reglement = REGLEMENT;
        underlineText(graphics2D, reglement, width, margin, y);
        y += 10;
        drawAndCenterText(graphics2D, reglement, width, margin, y);
        y += 5;
        underlineText(graphics2D, reglement, width, margin, y);
        y += 10;


        String libelle = differeReceipt.libelleMode();
        String paidAmount = differeReceipt.formattedPaidAmount();
        graphics2D.setFont(bodyFont);
        graphics2D.drawString(libelle, margin, y);
        graphics2D.setFont(bodyFontBold);
        fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
        graphics2D.drawString(paidAmount, rightMargin - fontMetrics.stringWidth(paidAmount), y);

        y += lineHeight;
        graphics2D.setFont(bodyFont);
        graphics2D.drawString(SOLDE, margin, y);
        graphics2D.setFont(bodyFontBold);
        fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
        String rest = differeReceipt.formattedSolde();
        graphics2D.drawString(rest, rightMargin - fontMetrics.stringWidth(rest), y);
        y += lineHeight;

        return y;
    }

    @Override
    protected int drawCashInfo(Graphics2D graphics2D, int margin, int y, int lineHeight) {
        if (!differeReceipt.monnaie().isEmpty()) {
            int rightMargin = getRightMargin();
            Font bodyFont = getBodyFont();
            Font bodyFontBold = getBodyFontBold();
            graphics2D.setFont(bodyFont);
            graphics2D.drawString(MONTANT_RENDU, margin, y);
            graphics2D.setFont(bodyFontBold);
            FontMetrics fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
            String montantRendu = differeReceipt.monnaie();
            graphics2D.drawString(montantRendu, rightMargin - fontMetrics.stringWidth(montantRendu), y);
            y += lineHeight;
        }
        return y;
    }

    public void printReceipt(String hostName, Long idReglement) {
        differeReceipt = reglementDiffereService.getReglementDiffereReceipt(idReglement);
        try {
            print(hostName);
        } catch (PrinterException e) {
            LOG.error("Error while printing receipt: {}", e.getMessage());
        }

    }




}

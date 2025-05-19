package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.PaymentModeDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.TvaEmbeded;
import com.kobe.warehouse.service.receipt.dto.AssuranceReceiptItem;
import com.kobe.warehouse.service.receipt.dto.CashSaleReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.receipt.dto.SaleReceiptItem;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public abstract class AbstractSaleReceiptService extends AbstractJava2DReceiptPrinterService {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractSaleReceiptService.class);
    protected int avoirCount;

    protected AbstractSaleReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    protected abstract SaleDTO getSale();

    protected abstract int drawAssuanceInfo(Graphics2D graphics2D, int width, int margin, int y, int lineHeight);

    protected int getProductNameWidth() {
        return 23;
    }

    @Override
    protected abstract List<? extends SaleReceiptItem> getItems();

    protected abstract int drawSummary(Graphics2D graphics2D, int width, int y, int lineHeight);

    protected void print(String hostName) throws PrinterException {
        PrinterJob job = getPrinterJob(hostName);
        PageFormat pageFormat = job.defaultPage();
        int pageHeight = (int) pageFormat.getImageableHeight();
        int pageWidth = DEFAULT_WIDTH;
        int lineHeight = DEFAULT_LINE_HEIGHT;
        Paper paper = new Paper();
        paper.setSize(pageWidth, pageHeight);
        paper.setImageableArea(DEFAULT_MARGIN, lineHeight, pageWidth, pageHeight - (2 * lineHeight));
        pageFormat.setPaper(paper);
        job.setPrintable(this, pageFormat);
        try {
            job.setCopies(getNumberOfCopies());
            job.print();
        } catch (PrinterException e) {
            LOG.error("Error printing receipt: {}", e.getMessage());
        }
    }

    protected List<HeaderFooterItem> getOperateurInfos() {
        SaleDTO sale = getSale();
        Font font = PLAIN_FONT;
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        if (sale.getCassierId().compareTo(sale.getSellerId()) != 0) {
            headerItems.add(new HeaderFooterItem("Ticket: " + sale.getNumberTransaction(), 1, font));
            headerItems.add(new HeaderFooterItem("Caissier: " + sale.getCassier().getAbbrName(), 1, font));
            headerItems.add(new HeaderFooterItem("Vendeur: " + sale.getSeller().getAbbrName(), 1, font));
        } else {
            headerItems.add(new HeaderFooterItem("Ticket: " + sale.getNumberTransaction(), 1, font));
            headerItems.add(new HeaderFooterItem("Caissier: " + sale.getCassier().getAbbrName(), 1, font));
        }

        return headerItems;
    }

    protected SaleReceiptItem fromSaleLine(SaleLineDTO saleLineDTO) {
        SaleReceiptItem item;
        if (getSale() instanceof CashSaleDTO) {
            item = new CashSaleReceiptItem();
        } else {
            item = new AssuranceReceiptItem();
        }

        int productNameWidth = getProductNameWidth();
        var produitName = saleLineDTO.getProduitLibelle();
        item.setProduitName(produitName.length() > productNameWidth ? produitName.substring(0, productNameWidth) : produitName);
        item.setQuantity(NumberUtil.formatToString(saleLineDTO.getQuantityRequested()));
        item.setUnitPrice(NumberUtil.formatToString(saleLineDTO.getRegularUnitPrice()));
        item.setTotalPrice(NumberUtil.formatToString(saleLineDTO.getSalesAmount()));

        return item;
    }

    protected int drawTableHeader(Graphics2D graphics2D, int margin, int y) {
        Font font = BOLD_FONT;
        FontMetrics fontMetrics = graphics2D.getFontMetrics(font);
        graphics2D.setFont(font);
        //add quantity before product
        String pu = "Prix";
        String total = "Total";
        graphics2D.drawString("Qté", 0, y); //sur 3 chiffres 30pixels //40
        graphics2D.drawString("Produit", 20, y); //90
        graphics2D.drawString(pu, getPuRightMargin() - fontMetrics.stringWidth(pu), y); //390 PU sur 6 chiffres 60pixels
        graphics2D.drawString(total, getRightMargin() - fontMetrics.stringWidth(total), y);
        y += 10;
        return y;
    }

    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        int width = DEFAULT_WIDTH; // 80mm in pixels, à parametrer
        int margin = DEFAULT_MARGIN; // margin in pixels

        int lineHeight = DEFAULT_LINE_HEIGHT;
        int maximumLinesPerPage = getMaximumLinesPerPage();
        int itemsSize = getItems().size();
        int totalPages = (int) Math.ceil((double) itemsSize / maximumLinesPerPage);
        if (pageIndex >= totalPages) {
            return NO_SUCH_PAGE;
        }

        int sartItemIndex = pageIndex * maximumLinesPerPage;
        int endItemIndex = Math.min(sartItemIndex + maximumLinesPerPage, itemsSize);
        boolean isLastPage = pageIndex == totalPages - 1;
        int y = lineHeight;
        y = drawCompagnyInfo(graphics2D, 0, y);
        y = drawWelcomeMessage(graphics2D, 0, y);
        y = drawHeader(graphics2D, 0, y, lineHeight);
        y = drawAssuanceInfo(graphics2D, width, margin, y, lineHeight);
        y = drawTableHeader(graphics2D, margin, y);
        y = drawLineSeparator(graphics2D, margin, y, width);
        Font font = PLAIN_FONT;
        graphics2D.setFont(font);
        FontMetrics fontMetrics = graphics2D.getFontMetrics(font);

        for (int i = sartItemIndex; i < endItemIndex; i++) {
            SaleReceiptItem item = getItems().get(i);
            String quantity = item.getQuantity();
            String produitName = item.getProduitName();
            String unitPrice = item.getUnitPrice();
            String totalPrice = item.getTotalPrice();
            graphics2D.drawString(quantity, 0, y);
            graphics2D.drawString(produitName, 20, y);
            graphics2D.drawString(unitPrice, getPuRightMargin() - fontMetrics.stringWidth(unitPrice), y);
            graphics2D.drawString(totalPrice, getRightMargin() - fontMetrics.stringWidth(totalPrice), y);
            //check if is last item
            if (i == endItemIndex - 1) {
                y += 10;
            } else {
                y += lineHeight;
            }
        }
        //derniere page
        if (isLastPage) {
            y = drawLineSeparator(graphics2D, margin, y, width);
            y = drawSummary(graphics2D, width, y, lineHeight);
            y = drawReglement(graphics2D, width, margin, y, lineHeight);
            y = drawCashInfo(graphics2D, y, lineHeight);
            y = drawResteToPay(graphics2D, y, lineHeight);
            //  y = drawTaxeDetail(graphics2D, width, margin, y, lineHeight);
            y = drawFooter(graphics2D, margin, y, lineHeight);
            y = drawLineSeparator(graphics2D, margin, y, width);
            y = drawDate(graphics2D, 0, y, lineHeight);
            drawThanksMessage(graphics2D, 0, y);
        }
        return PAGE_EXISTS;
    }

    protected int drawTaxeDetail(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        List<TvaEmbeded> tvaEmbededs = getSale().getTvaEmbededs();
        if (CollectionUtils.isEmpty(tvaEmbededs)) {
            return y;
        }
        Font bodyFont = PLAIN_FONT;
        int rightMargin = getRightMargin();
        graphics2D.setFont(bodyFont);
        FontMetrics fontMetrics;
        String tva = TVA;
        underlineText(graphics2D, tva, width, margin, y);
        y += 10;
        drawAndCenterText(graphics2D, tva, width, margin, y);
        y += 5;
        underlineText(graphics2D, tva, width, margin, y);
        y += 10;

        for (TvaEmbeded tvaEmbeded : tvaEmbededs) {
            graphics2D.setFont(bodyFont);
            graphics2D.drawString(tvaEmbeded.getTva() + "%", margin, y);
            fontMetrics = graphics2D.getFontMetrics(bodyFont);
            String amount = NumberUtil.formatToString(tvaEmbeded.getAmount());
            graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
            y += lineHeight;
        }
        return y;
    }

    @Override
    protected int drawReglement(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        List<PaymentDTO> payments = getSale().getPayments();
        if (CollectionUtils.isEmpty(payments)) {
            return y;
        }
        int rightMargin = getRightMargin();
        graphics2D.setFont(PLAIN_FONT);
        FontMetrics fontMetrics = graphics2D.getFontMetrics(PLAIN_FONT);
        /*  String reglement = REGLEMENT;
        underlineText(graphics2D, reglement, width, margin, y);
        y += 10;
        drawAndCenterText(graphics2D, reglement, width, margin, y);
        y += 5;
        underlineText(graphics2D, reglement, width, margin, y);
        y += 10;*/
        for (PaymentDTO payment : payments) {
            PaymentModeDTO paymentMode = payment.getPaymentMode();
            String libelle = paymentMode.getLibelle();
            String amount = paymentMode.getCode().equals(ModePaimentCode.CASH.name())
                ? NumberUtil.formatToString(payment.getMontantVerse())
                : NumberUtil.formatToString(payment.getPaidAmount());

            graphics2D.drawString(libelle, 0, y);

            graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
            y += lineHeight;
        }
        return y;
    }

    protected int drawCashInfo(Graphics2D graphics2D, int y, int lineHeight) {
        if (getSale().getMontantRendu() != null && getSale().getMontantRendu() > 0) {
            int rightMargin = getRightMargin();
            graphics2D.drawString(MONTANT_RENDU, 0, y);
            FontMetrics fontMetrics = graphics2D.getFontMetrics();
            String amount = NumberUtil.formatToString(getSale().getMontantRendu());
            graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
            y += lineHeight;
        }
        return y;
    }

    protected int drawResteToPay(Graphics2D graphics2D, int y, int lineHeight) {
        if (getSale().getRestToPay() != null && getSale().getRestToPay() > 0) {
            int rightMargin = getRightMargin();

            graphics2D.setFont(BOLD_FONT);
            graphics2D.drawString(RESTE_A_PAYER, 0, y);
            FontMetrics fontMetrics = graphics2D.getFontMetrics();
            String amount = NumberUtil.formatToString(getSale().getRestToPay());
            graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
            y += lineHeight;
        }
        return y;
    }

    private int getPuRightMargin() {
        return 150 + (DEFAULT_MARGIN * 2);
    }
}

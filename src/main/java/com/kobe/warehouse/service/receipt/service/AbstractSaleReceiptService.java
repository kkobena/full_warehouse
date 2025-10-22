package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.domain.enumeration.ModePaimentCode;
import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.PaymentDTO;
import com.kobe.warehouse.service.dto.PaymentModeDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.TvaEmbeded;
import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.receipt.dto.CashSaleReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.receipt.dto.SaleReceiptItem;
import com.kobe.warehouse.service.utils.NumberUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.imageio.ImageIO;

@Service
public abstract class AbstractSaleReceiptService extends AbstractJava2DReceiptPrinterService {

    protected int avoirCount;

    protected AbstractSaleReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    protected abstract SaleDTO getSale();

    protected abstract int drawAssuanceInfo(Graphics2D graphics2D, int width, int margin, int y, int lineHeight);

    protected int getProductNameWidth() {
        return 22;
    }

    @Override
    protected abstract List<? extends SaleReceiptItem> getItems();

    protected abstract int drawSummary(Graphics2D graphics2D, int width, int y, int lineHeight);

    protected List<HeaderFooterItem> getOperateurInfos() {
        SaleDTO sale = getSale();
        Font font = PLAIN_FONT;
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        headerItems.add(new HeaderFooterItem("Ticket: " + sale.getNumberTransaction(), 1, font));
        headerItems.add(new HeaderFooterItem("Caissier(re): " + sale.getCassier().getAbbrName(), 1, font));
        if (sale.getCassierId().compareTo(sale.getSellerId()) != 0) {
            headerItems.add(new HeaderFooterItem("Vendeur(se): " + sale.getSeller().getAbbrName(), 1, font));
        }
        return headerItems;
    }

    protected SaleReceiptItem fromSaleLine(SaleLineDTO saleLineDTO) {
        CashSaleReceiptItem item = new CashSaleReceiptItem();
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

        String pu = "Prix";
        String total = "Montant";
        graphics2D.drawString("Qté", margin, y); //sur 3 chiffres 30pixels //40
        graphics2D.drawString("Produit", 20 + margin, y); //90
        graphics2D.drawString(pu, getPuRightMargin() - fontMetrics.stringWidth(pu), y); //390 PU sur 6 chiffres 60pixels
        graphics2D.drawString(total, getRightMargin() - fontMetrics.stringWidth(total), y);
        y += 10;
        return y;
    }
    /**
     * Generate receipt as list of byte arrays for Tauri printing
     * This method creates PNG images for each page of the receipt
     * that can be sent to Tauri clients running on different machines
     *
     * @return List of byte arrays, each representing a page as PNG image
     * @throws IOException if image generation fails
     */
    @Override
    public List<byte[]> generateTicket() throws IOException {
        List<? extends SaleReceiptItem> items = this.getItems();
        int linesPerPage = getMaximumLinesPerPage();
        List<byte[]> pages = new ArrayList<>();

        int totalPages = (int) Math.ceil(items.size() / (double) linesPerPage);

        // Scale factor for high-resolution printing (300 DPI vs 72 DPI = ~4x)
        final int SCALE_FACTOR = 4;

        for (int pageNum = 0; pageNum < totalPages; pageNum++) {
            int startItemIndex = pageNum * linesPerPage;
            int endItemIndex = Math.min(startItemIndex + linesPerPage, items.size());
            boolean isLastPage = pageNum == totalPages - 1;

            // Calculate page height dynamically based on content
            int estimatedHeight = estimatePageHeight(endItemIndex - startItemIndex, isLastPage);

            // Create buffered image with high resolution for better print quality
            BufferedImage image = new BufferedImage(
                (DEFAULT_WIDTH + (DEFAULT_MARGIN * 2)) * SCALE_FACTOR,
                estimatedHeight * SCALE_FACTOR,
                BufferedImage.TYPE_INT_RGB  // Better quality than TYPE_BYTE_GRAY
            );
            Graphics2D g2d = image.createGraphics();

            // Scale the graphics context for high-resolution rendering
            g2d.scale(SCALE_FACTOR, SCALE_FACTOR);

            // Set rendering hints for better quality
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

            // White background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, image.getWidth(), image.getHeight());
            g2d.setColor(Color.BLACK);

            int y = DEFAULT_LINE_HEIGHT;

            // Draw receipt header (only on first page or all pages based on preference)
            if (pageNum == 0) {
                y = drawReceiptHeader(g2d, y, DEFAULT_LINE_HEIGHT);
            } else {
                // For subsequent pages, draw minimal header
                y = drawTableHeader(g2d, DEFAULT_MARGIN, y);
                y = drawLineSeparator(g2d, DEFAULT_MARGIN, y, DEFAULT_WIDTH);
            }

            // Draw items for this page
            y = drawReceiptItems(g2d, y, startItemIndex, endItemIndex, DEFAULT_LINE_HEIGHT);

            // Draw summary only on last page
            if (isLastPage) {
                y = drawReceiptSummary(g2d, y, DEFAULT_LINE_HEIGHT);
            }

            g2d.dispose();

            // Convert image to PNG byte array
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ImageIO.write(image, "png", baos);
                pages.add(baos.toByteArray());
            }
        }

        return pages;
    }

    /**
     * Estimate the height needed for a receipt page
     *
     * @param itemCount number of items on this page
     * @param isLastPage whether this is the last page (includes summary)
     * @return estimated height in pixels
     */
    private int estimatePageHeight(int itemCount, boolean isLastPage) {
        int height = DEFAULT_MARGIN * 2; // Top and bottom margins

        // Header height (company info, welcome message, table header)
        height += DEFAULT_LINE_HEIGHT * 15; // Approximate header lines

        // Items height
        height += itemCount * DEFAULT_LINE_HEIGHT;

        // Summary height (only on last page)
        if (isLastPage) {
            // Summary section: total, payment, taxes, footer
            height += DEFAULT_LINE_HEIGHT * 20; // Approximate summary lines

            // Add extra space for payment modes (variable)
            SaleDTO sale = getSale();
            if (sale != null && sale.getPayments() != null) {
                height += sale.getPayments().size() * DEFAULT_LINE_HEIGHT;
            }

            // Add extra space for TVA details (variable)
            if (sale != null && sale.getTvaEmbededs() != null) {
                height += sale.getTvaEmbededs().size() * DEFAULT_LINE_HEIGHT;
            }
        }

        return height;
    }
    @Override
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

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

        y = drawReceiptHeader(graphics2D, y, lineHeight);
        y = drawReceiptItems(graphics2D, y, sartItemIndex, endItemIndex, lineHeight);

        if (isLastPage) {
            y = drawReceiptSummary(graphics2D, y, lineHeight);
        }
        return PAGE_EXISTS;
    }

    protected void drawItem(Graphics2D graphics2D, int y, FontMetrics fontMetrics, SaleReceiptItem item) {
        graphics2D.drawString(item.getQuantity(), DEFAULT_MARGIN, y);
        graphics2D.drawString(item.getProduitName(), 20 + DEFAULT_MARGIN, y);
        graphics2D.drawString(item.getUnitPrice(), getPuRightMargin() - fontMetrics.stringWidth(item.getUnitPrice()), y);
        graphics2D.drawString(item.getTotalPrice(), getRightMargin() - fontMetrics.stringWidth(item.getTotalPrice()), y);
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
        y = drawSectionHeader(graphics2D, TVA, width, margin, y);

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
        y = drawSectionHeader(graphics2D, REGLEMENT, width, margin, y);

        for (PaymentDTO payment : payments) {
            PaymentModeDTO paymentMode = payment.getPaymentMode();
            String libelle = paymentMode.getLibelle();
            String amount = paymentMode.getCode().equals(ModePaimentCode.CASH.name())
                ? NumberUtil.formatToString(payment.getMontantVerse())
                : NumberUtil.formatToString(payment.getPaidAmount());
            graphics2D.drawString(libelle, DEFAULT_MARGIN, y);
            graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
            y += lineHeight;
        }
        return y;
    }

    protected int drawCashInfo(Graphics2D graphics2D, int y, int lineHeight) {
        if (getSale().getMontantRendu() != null && getSale().getMontantRendu() > 0) {
            int rightMargin = getRightMargin();
            graphics2D.drawString(MONTANT_RENDU, DEFAULT_MARGIN, y);
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
            graphics2D.drawString(RESTE_A_PAYER, DEFAULT_MARGIN, y);
            FontMetrics fontMetrics = graphics2D.getFontMetrics();
            String amount = NumberUtil.formatToString(getSale().getRestToPay());
            graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
            y += lineHeight;
        }
        return y;
    }

    protected int getPuRightMargin() {
        return 160 + DEFAULT_MARGIN;
    }

    private int drawReceiptHeader(Graphics2D graphics2D, int y, int lineHeight) {
        int width = DEFAULT_WIDTH;
        int margin = DEFAULT_MARGIN;
        y = drawCompagnyInfo(graphics2D, DEFAULT_MARGIN, y);
        y = drawWelcomeMessage(graphics2D, DEFAULT_MARGIN, y);
        y = drawHeader(graphics2D, DEFAULT_MARGIN, y, lineHeight);
        y = drawAssuanceInfo(graphics2D, width, margin, y, lineHeight);
        y = drawTableHeader(graphics2D, margin, y);
        y = drawLineSeparator(graphics2D, margin, y, width);
        return y;
    }

    private int drawReceiptItems(Graphics2D graphics2D, int y, int sartItemIndex, int endItemIndex, int lineHeight) {
        Font font = PLAIN_FONT;
        graphics2D.setFont(font);
        FontMetrics fontMetrics = graphics2D.getFontMetrics(font);
        for (int i = sartItemIndex; i < endItemIndex; i++) {
            SaleReceiptItem item = getItems().get(i);
            drawItem(graphics2D, y, fontMetrics, item);
            if (i == endItemIndex - 1) {
                y += 10;
            } else {
                y += lineHeight;
            }
        }
        return y;
    }

    private int drawReceiptSummary(Graphics2D graphics2D, int y, int lineHeight) {
        int width = DEFAULT_WIDTH;
        int margin = DEFAULT_MARGIN;
        y = drawLineSeparator(graphics2D, margin, y, width);
        y = drawSummary(graphics2D, width, y, lineHeight);
        y = drawReglement(graphics2D, width, margin, y, lineHeight);
        y = drawCashInfo(graphics2D, y, lineHeight);
        y = drawResteToPay(graphics2D, y, lineHeight);
        y = drawTaxeDetail(graphics2D, width, margin, y, lineHeight);
        y = drawFooter(graphics2D, margin, y, lineHeight);
        y = drawLineSeparator(graphics2D, margin, y, width);
        y = drawDate(graphics2D, DEFAULT_MARGIN, y, lineHeight);
        drawThanksMessage(graphics2D, DEFAULT_MARGIN, y);
        return y;
    }

    private int drawSectionHeader(Graphics2D graphics2D, String title, int width, int margin, int y) {
        underlineText(graphics2D, title, width, margin, y);
        y += 10;
        drawAndCenterText(graphics2D, title, width, margin, y);
        y += 5;
        underlineText(graphics2D, title, width, margin, y);
        y += 10;
        return y;
    }


}

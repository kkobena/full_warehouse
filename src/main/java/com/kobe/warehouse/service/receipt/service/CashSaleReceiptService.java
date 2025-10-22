package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.receipt.dto.CashSaleReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.receipt.dto.SaleReceiptItem;
import com.kobe.warehouse.service.utils.NumberUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;

@Service
public class CashSaleReceiptService extends AbstractSaleReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(CashSaleReceiptService.class);
    private CashSaleDTO cashSale;
    private boolean isEdit;

    public CashSaleReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected int drawAssuanceInfo(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        return y;
    }

    @Override
    protected SaleDTO getSale() {
        return cashSale;
    }

    @Override
    public List<CashSaleReceiptItem> getItems() {
        return cashSale.getSalesLines().stream().map(saleLineDTO -> (CashSaleReceiptItem) fromSaleLine(saleLineDTO)).toList();
    }

    public void printReceipt(String hostName, CashSaleDTO sale, boolean isEdit) {
        this.isEdit = isEdit;
        this.cashSale = sale;
        try {
            print(hostName);
        } catch (PrinterException e) {
            LOG.error("Error while printing receipt: {}", e.getMessage());
        }
    }

    @Override
    public List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        if (cashSale.getCustomer() != null) {
            UninsuredCustomerDTO customer = (UninsuredCustomerDTO) cashSale.getCustomer();
            headerItems.add(new HeaderFooterItem("Client: " + customer.getFullName(), 1, PLAIN_FONT));
            if (StringUtils.hasLength(customer.getPhone())) {
                headerItems.add(new HeaderFooterItem("Tél: " + customer.getPhone(), 1, PLAIN_FONT));
            }
        }
        headerItems.addAll(getOperateurInfos());

        return headerItems;
    }

    @Override
    protected int getNumberOfCopies() {
        return 1;
    }

    @Override
    public List<HeaderFooterItem> getFooterItems() {
        return List.of();
        /* List<HeaderFooterItem> headerItems = new ArrayList<>();
        Font font = getBodyFont();
        headerItems.add(new HeaderFooterItem("Montants exprimés en FCFA", 1, font));
        return headerItems;*/
    }

    @Override
    protected int drawSummary(Graphics2D graphics2D, int width, int y, int lineHeight) {
        SaleDTO sale = getSale();
        int rightMargin = getRightMargin();
        int margin = DEFAULT_MARGIN;

        graphics2D.setFont(PLAIN_FONT);
        graphics2D.drawString(MONTANT_TTC, margin, y);
        String amount = NumberUtil.formatToString(sale.getSalesAmount());
        if (avoirCount > 0) {
            graphics2D.setFont(BOLD_FONT);
            drawAndCenterText(graphics2D, "Avoir( " + NumberUtil.formatToString(avoirCount) + " )", width, margin, y);
            graphics2D.setFont(PLAIN_FONT);
        }
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
        y += lineHeight;
        if (sale.getDiscountAmount() != null && sale.getDiscountAmount() > 0) {
            graphics2D.drawString(REMISE, margin, y);
            String discount = NumberUtil.formatToString(sale.getDiscountAmount() * (-1));
            graphics2D.drawString(discount, rightMargin - fontMetrics.stringWidth(discount), y);
            y += lineHeight;
        }
        if (sale.getTaxAmount() != null && sale.getTaxAmount() > 0) {
            graphics2D.drawString(TOTAL_TVA, margin, y);
            String tax = NumberUtil.formatToString(sale.getTaxAmount());
            graphics2D.drawString(tax, rightMargin - fontMetrics.stringWidth(tax), y);
            y += lineHeight;
        }
        if (sale.getAmountToBePaid() != null) {
            graphics2D.setFont(BOLD_FONT);
            graphics2D.drawString(TOTAL_A_PAYER, margin, y);
            fontMetrics = graphics2D.getFontMetrics();
            String payroll = NumberUtil.formatToString(sale.getAmountToBePaid());
            graphics2D.drawString(payroll, rightMargin - fontMetrics.stringWidth(payroll), y);
            y += lineHeight;
        }

        return y;
    }

    /**
     * Generate receipt as byte arrays for Tauri clients
     * <p>
     * This method is specifically designed for Tauri clients that need to print
     * receipts on a different machine from the backend server
     *
     * @param sale the cash sale to generate receipt for
     * @return list of byte arrays representing receipt pages as PNG images
     * @throws IOException if image generation fails
     */
    public List<byte[]> generateTicketForTauri(CashSaleDTO sale) throws IOException {
        this.cashSale = sale;
        this.isEdit = false;
        return generateTicket();
    }

}

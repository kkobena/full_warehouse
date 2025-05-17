package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.receipt.dto.CashSaleReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.service.utils.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.awt.*;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;

@Service
public class CashSaleReceiptService extends AbstractSaleReceiptService {
    private static final Logger LOG = LoggerFactory.getLogger(CashSaleReceiptService.class);
    private final SaleDataService saleDataService;
    private CashSaleDTO cashSale;
    private boolean isEdit;

    public CashSaleReceiptService(AppConfigurationService appConfigurationService, SaleDataService saleDataService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
        this.saleDataService = saleDataService;
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

    public void printReceipt(String hostName, Long saleId, boolean isEdit) {
        this.isEdit = isEdit;
        cashSale = (CashSaleDTO) this.saleDataService.getOneSaleDTO(saleId);
        try {
            print(hostName);
        } catch (PrinterException e) {
            LOG.error("Error while printing receipt: {}", e.getMessage());
        }

    }

    @Override
    public List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        Font font = getBodyFont();
        if (cashSale.getCustomer() != null) {
            UninsuredCustomerDTO customer = (UninsuredCustomerDTO) cashSale.getCustomer();
            if (StringUtils.hasLength(customer.getPhone())) {
                headerItems.add(new HeaderFooterItem("Client: " + customer.getFullName() + " | Tel: " + customer.getPhone(), 1, font));
            } else {
                headerItems.add(new HeaderFooterItem("Client: " + customer.getFullName(), 1, font));
            }
        }
        headerItems.addAll(getOperateurInfos());

        return headerItems;
    }

    @Override
    protected int getNumberOfCopies() {
        if (isEdit) {
            return 1;
        }
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
    protected int drawSummary(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        SaleDTO sale = getSale();
        int rightMargin = getRightMargin();
        Font bodyFont = getBodyFont();
        Font bodyFontBold = getBodyFontBold();
        graphics2D.setFont(bodyFont);
        graphics2D.drawString(MONTANT_TTC, margin, y);
        graphics2D.setFont(bodyFontBold);
        FontMetrics fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
        String amount = NumberUtil.formatToString(sale.getSalesAmount());
        if (avoirCount > 0) {
            drawAndCenterText(graphics2D, "Avoir( " + NumberUtil.formatToString(avoirCount) + " )", width, margin, y);
        }
        graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
        y += lineHeight;
        if (sale.getDiscountAmount() != null && sale.getDiscountAmount() > 0) {
            graphics2D.setFont(bodyFont);
            graphics2D.drawString(REMISE, margin, y);
            graphics2D.setFont(bodyFontBold);
            fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
            String discount = NumberUtil.formatToString(sale.getDiscountAmount() * (-1));
            graphics2D.drawString(discount, rightMargin - fontMetrics.stringWidth(discount), y);
            y += lineHeight;

        }
        if (sale.getTaxAmount() != null && sale.getTaxAmount() > 0) {
            graphics2D.setFont(bodyFont);
            graphics2D.drawString(TOTAL_TVA, margin, y);
            graphics2D.setFont(bodyFontBold);
            fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
            String tax = NumberUtil.formatToString(sale.getTaxAmount());
            graphics2D.drawString(tax, rightMargin - fontMetrics.stringWidth(tax), y);
            y += lineHeight;
        }
        if (sale.getAmountToBePaid() != null) {
            graphics2D.setFont(bodyFont);
            graphics2D.drawString(TOTAL_A_PAYER, margin, y);
            graphics2D.setFont(bodyFontBold);
            fontMetrics = graphics2D.getFontMetrics(bodyFontBold);
            String payroll = NumberUtil.formatToString(sale.getAmountToBePaid());
            graphics2D.drawString(payroll, rightMargin - fontMetrics.stringWidth(payroll), y);
            y += 5;
        }

        return y;
    }

}

package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.receipt.dto.CashSaleReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.print.PrintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CashSaleReceiptService extends AbstractSaleReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(CashSaleReceiptService.class);
    private CashSaleDTO cashSale;
    private int avoirCount;

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
    protected int getAvoirCount() {
        return avoirCount;
    }

    @Override
    public List<CashSaleReceiptItem> getItems() {
        List<CashSaleReceiptItem> items = new ArrayList<>();
        for (SaleLineDTO line : cashSale.getSalesLines()) {
            avoirCount += (line.getQuantityRequested() - line.getQuantitySold());
            items.add((CashSaleReceiptItem) fromSaleLine(line));
        }

        return items;
    }

    public void printReceipt(String hostName, CashSaleDTO sale, boolean isEdit) {
        this.cashSale = sale;
        try {
            printEscPosDirectByHost(hostName, isEdit);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS receipt: {}", e.getMessage(), e);
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
     * Generate ESC/POS receipt for direct thermal printer printing
     * <p>
     * This method generates raw ESC/POS commands that can be sent directly to a thermal POS printer.
     * Much more efficient than PNG generation - produces smaller payloads and faster printing.
     *
     * @param sale the cash sale to generate receipt for
     * @return byte array containing ESC/POS commands
     * @throws IOException if generation fails
     */
    public byte[] generateEscPosReceiptForTauri(CashSaleDTO sale, boolean isEdit) throws IOException {
        this.cashSale = sale;
        return generateEscPosReceipt(isEdit);
    }
}

package com.kobe.warehouse.service.receipt.service;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleLineDTO;
import com.kobe.warehouse.service.receipt.dto.AssuranceReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.receipt.dto.SaleReceiptItem;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.print.PrintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class AssuranceSaleReceiptService extends AbstractSaleReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(AssuranceSaleReceiptService.class);
    private ThirdPartySaleDTO thirdPartySale;
    private boolean isEdit;
    private int avoirCount;

    public AssuranceSaleReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected SaleDTO getSale() {
        return thirdPartySale;
    }

    @Override
    protected int getAvoirCount() {
        return avoirCount;
    }

    @Override
    public List<AssuranceReceiptItem> getItems() {
        List<AssuranceReceiptItem> items = new ArrayList<>();
        for (SaleLineDTO line : thirdPartySale.getSalesLines()) {
            avoirCount += (line.getQuantityRequested() - line.getQuantitySold());
            items.add(fromSaleLine(line));
        }

        return items;
    }

    @Override
    protected AssuranceReceiptItem fromSaleLine(SaleLineDTO saleLineDTO) {
        AssuranceReceiptItem item = new AssuranceReceiptItem();
        int productNameWidth = getProductNameWidth();
        var produitName = saleLineDTO.getProduitLibelle();
        item.setTotalPrice(NumberUtil.formatToString(saleLineDTO.getSalesAmount()));

        item.setProduitName(produitName.length() > productNameWidth ? produitName.substring(0, productNameWidth) : produitName);
        item.setQuantity(NumberUtil.formatToString(saleLineDTO.getQuantityRequested()));
        item.setUnitPrice(NumberUtil.formatToString(saleLineDTO.getRegularUnitPrice()));

        return item;
    }

    public void printReceipt(String hostName, ThirdPartySaleDTO thirdPartySale, boolean isEdit) {
        this.isEdit = isEdit;
        this.thirdPartySale = thirdPartySale;

        try {
            // Use direct ESC/POS printing for better performance and reliability
            printEscPosDirectByHost(hostName, isEdit);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS receipt: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        Font font = PLAIN_FONT;
        AssuredCustomerDTO customer = (AssuredCustomerDTO) thirdPartySale.getCustomer();
        headerItems.add(
            new HeaderFooterItem(
                "ASSURE: " +
                customer.getFullName() +
                " | MATRICULE: " +
                thirdPartySale
                    .getTiersPayants()
                    .stream()
                    .filter(e -> e.getPriorite() == PrioriteTiersPayant.R0)
                    .findFirst()
                    .map(ClientTiersPayantDTO::getNum)
                    .orElse(""),
                1,
                font
            )
        );
        if (!customer.getId().equals(thirdPartySale.getAyantDroitId())) {
            headerItems.add(
                new HeaderFooterItem(
                    "BENEFICIAIRE: " +
                    thirdPartySale.getAyantDroitFirstName().concat(" ") +
                    thirdPartySale.getAyantDroitLastName() +
                    " | MATRICULE: " +
                    thirdPartySale.getAyantDroitNum(),
                    1,
                    font
                )
            );
        }
        headerItems.addAll(getOperateurInfos());
        return headerItems;
    }

    @Override
    protected int getNumberOfCopies() {
        if (isEdit) {
            return 1;
        }
        return thirdPartySale.getThirdPartySaleLines().size() + 1;
    }

    @Override
    public List<HeaderFooterItem> getFooterItems() {
        return List.of();
    }

    @Override
    protected int drawAssuanceInfo(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        int rightMargin = getRightMargin();

        FontMetrics fontMetrics;
        for (ThirdPartySaleLineDTO thirdPartySaleLine : thirdPartySale.getThirdPartySaleLines()) {
            graphics2D.setFont(PLAIN_FONT);
            graphics2D.drawString(
                thirdPartySaleLine.getPriorite().getCode().concat(": " + getTiersPayantName(thirdPartySaleLine.getName())),
                margin,
                y
            );

            if (!thirdPartySale.isHasPriceOption()) {
                drawAndCenterText(graphics2D, thirdPartySaleLine.getTaux() + "%", width, getPuRightMargin() + 100, y);
            } else {
                drawAndCenterText(
                    graphics2D,
                    NumberUtil.formatToString(thirdPartySaleLine.getMontant()),
                    width,
                    getPuRightMargin() + 100,
                    y
                );
            }

            graphics2D.setFont(BOLD_FONT);
            fontMetrics = graphics2D.getFontMetrics(BOLD_FONT);
            String amount = NumberUtil.formatToString(thirdPartySaleLine.getMontant());
            graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
            y += lineHeight;
        }

        return y;
    }

    @Override
    protected int drawTableHeader(Graphics2D graphics2D, int margin, int y) {
        Font font = BOLD_FONT;
        FontMetrics fontMetrics = graphics2D.getFontMetrics(font);
        graphics2D.setFont(font);
        //add quantity before product
        /*   if (thirdPartySale.isHasPriceOption()) {
            return drawOprixOptionTableHeader(graphics2D, margin, y, fontMetrics);
        }*/

        return drawDefaultTableHeader(graphics2D, margin, y, fontMetrics);
    }

    @Override
    protected int getProductNameWidth() {
        /*if (thirdPartySale.isHasPriceOption()) {
            return 20;
        }*/
        return 22;
    }

    private int drawOprixOptionTableHeader(Graphics2D graphics2D, int margin, int y, FontMetrics fontMetrics) {
        //add quantity before product
        String pu = "Prix";
        String taux = "%Rb";
        String total = "Montant";
        graphics2D.drawString("Qté", margin, y); //sur 3 chiffres 30pixels //40
        graphics2D.drawString("Produit", 18 + margin, y); //90
        graphics2D.drawString(pu, getTauxRightMargin() - fontMetrics.stringWidth(pu), y); //390 PU sur 6 chiffres 60pixels
        graphics2D.drawString(taux, getPercentageRightMargin() - fontMetrics.stringWidth(taux), y); //390 PU sur 6 chiffres 60pixels
        graphics2D.drawString(total, getRightMargin() - fontMetrics.stringWidth(total), y);
        y += 10;
        return y;
    }

    private int getPercentageRightMargin() {
        return 165 + DEFAULT_MARGIN;
    }

    @Override
    protected void drawItem(Graphics2D graphics2D, int y, FontMetrics fontMetrics, SaleReceiptItem item) {
        /*  if (thirdPartySale.isHasPriceOption()) {
            drawOptionPrixItem(graphics2D, y, fontMetrics, item);
        } else {
            drawDefaultItem(graphics2D, y, fontMetrics, item);
        }*/

        drawDefaultItem(graphics2D, y, fontMetrics, item);
    }

    private void drawOptionPrixItem(Graphics2D graphics2D, int y, FontMetrics fontMetrics, SaleReceiptItem item) {
        AssuranceReceiptItem assuranceItem = (AssuranceReceiptItem) item;
        String quantity = assuranceItem.getQuantity();
        String produitName = assuranceItem.getProduitName();
        String unitPrice = assuranceItem.getUnitPrice();
        String totalPrice = assuranceItem.getTotalPrice();
        String taux = assuranceItem.getTaux();
        graphics2D.drawString(quantity, DEFAULT_MARGIN, y);
        graphics2D.drawString(produitName, 18 + DEFAULT_MARGIN, y);
        graphics2D.drawString(unitPrice, (getTauxRightMargin() + 2) - fontMetrics.stringWidth(unitPrice), y);
        graphics2D.drawString(taux, getPercentageRightMargin() - fontMetrics.stringWidth(taux), y);
        graphics2D.drawString(totalPrice, getRightMargin() - fontMetrics.stringWidth(totalPrice), y);
    }

    private void drawDefaultItem(Graphics2D graphics2D, int y, FontMetrics fontMetrics, SaleReceiptItem item) {
        super.drawItem(graphics2D, y, fontMetrics, item);
    }

    private int getTauxRightMargin() {
        return 153;
    }

    private int drawDefaultTableHeader(Graphics2D graphics2D, int margin, int y, FontMetrics fontMetrics) {
        //add quantity before product
        String pu = "Prix";
        String total = "Montant";
        graphics2D.drawString("Qté", margin, y); //sur 3 chiffres 30pixels //40
        graphics2D.drawString("Produit", 20 + margin, y); //90
        graphics2D.drawString(pu, getPuRightMargin() - fontMetrics.stringWidth(pu), y); //390 PU sur 6 chiffres 60pixels
        graphics2D.drawString(total, getRightMargin() - fontMetrics.stringWidth(total), y);
        y += 10;
        return y;
    }

    @Override
    protected int drawSummary(Graphics2D graphics2D, int width, int y, int lineHeight) {
        int rightMargin = getRightMargin();

        graphics2D.setFont(PLAIN_FONT);
        graphics2D.drawString(MONTANT_TTC, DEFAULT_MARGIN, y);
        //
        String amount = NumberUtil.formatToString(thirdPartySale.getSalesAmount());
        if (avoirCount > 0) {
            graphics2D.setFont(BOLD_FONT);
            drawAndCenterText(graphics2D, "Avoir( " + NumberUtil.formatToString(avoirCount) + " )", width, 0, y);
            graphics2D.setFont(PLAIN_FONT);
        }
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
        y += lineHeight;
        if (thirdPartySale.getDiscountAmount() != null && thirdPartySale.getDiscountAmount() > 0) {
            graphics2D.drawString(REMISE, DEFAULT_MARGIN, y);
            String discount = NumberUtil.formatToString(thirdPartySale.getDiscountAmount() * (-1));
            graphics2D.drawString(discount, rightMargin - fontMetrics.stringWidth(discount), y);
            y += lineHeight;
        }
        if (thirdPartySale.getTaxAmount() != null && thirdPartySale.getTaxAmount() > 0) {
            graphics2D.drawString(TOTAL_TVA, DEFAULT_MARGIN, y);
            String tax = NumberUtil.formatToString(thirdPartySale.getTaxAmount());
            graphics2D.drawString(tax, rightMargin - fontMetrics.stringWidth(tax), y);
            y += lineHeight;
        }
        if (nonNull(thirdPartySale.getPartTiersPayant())) {
            graphics2D.drawString("Part tiers payant", DEFAULT_MARGIN, y);
            String partTiersPayant = NumberUtil.formatToString(thirdPartySale.getPartTiersPayant());
            graphics2D.drawString(partTiersPayant, rightMargin - fontMetrics.stringWidth(partTiersPayant), y);
            y += lineHeight;
        }
        if (thirdPartySale.getAmountToBePaid() != null && thirdPartySale.getAmountToBePaid() > 0) {
            graphics2D.setFont(BOLD_FONT);
            graphics2D.drawString("Reste à charge", DEFAULT_MARGIN, y);
            fontMetrics = graphics2D.getFontMetrics(BOLD_FONT);
            String payroll = NumberUtil.formatToString(thirdPartySale.getAmountToBePaid());
            graphics2D.drawString(payroll, rightMargin - fontMetrics.stringWidth(payroll), y);
            y += lineHeight;
        }
        return y;
    }

    private String getTiersPayantName(String name) {
        if (name.length() < 28) {
            return name;
        }
        return name.substring(0, 28);
    }

    /**
     * Generate ESC/POS receipt for direct thermal printer printing
     * <p>
     * This method generates raw ESC/POS commands that can be sent directly to a thermal POS printer.
     * Much more efficient than PNG generation - produces smaller payloads and faster printing.
     *
     * @param sale the third-party sale to generate receipt for
     * @return byte array containing ESC/POS commands
     * @throws IOException if generation fails
     */
    public byte[] generateEscPosReceiptForTauri(ThirdPartySaleDTO sale, boolean isEdit) throws IOException {
        this.thirdPartySale = sale;
        return generateEscPosReceipt(isEdit);
    }

    /**
     * Override to provide insurance information for ESC/POS receipts
     */
    @Override
    protected String getAssuranceInfoText() {
        if (thirdPartySale == null || CollectionUtils.isEmpty(thirdPartySale.getThirdPartySaleLines())) {
            return null;
        }

        StringBuilder info = new StringBuilder();
        for (ThirdPartySaleLineDTO thirdPartySaleLine : thirdPartySale.getThirdPartySaleLines()) {
            String priorityCode = thirdPartySaleLine.getPriorite().getCode();
            String tiersPayantName = getTiersPayantName(thirdPartySaleLine.getName());
            String amount = NumberUtil.formatToString(thirdPartySaleLine.getMontant());

            if (!thirdPartySale.isHasPriceOption()) {
                // Display percentage
                info.append(String.format("%-26s %7s%% %10s", priorityCode + ": " + tiersPayantName, thirdPartySaleLine.getTaux(), amount));
            } else {
                // Display amount
                info.append(
                    String.format(
                        "%-26s %10s %10s",
                        priorityCode + ": " + tiersPayantName,
                        NumberUtil.formatToString(thirdPartySaleLine.getMontant()),
                        amount
                    )
                );
            }
            info.append("\n");
        }

        return info.toString().trim();
    }
}

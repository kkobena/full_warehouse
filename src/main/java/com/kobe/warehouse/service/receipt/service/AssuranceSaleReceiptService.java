package com.kobe.warehouse.service.receipt.service;

import static java.util.Objects.nonNull;

import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.AssuredCustomerDTO;
import com.kobe.warehouse.service.dto.ClientTiersPayantDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleDTO;
import com.kobe.warehouse.service.dto.ThirdPartySaleLineDTO;
import com.kobe.warehouse.service.receipt.dto.AssuranceReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.sale.SaleDataService;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.print.PrinterException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AssuranceSaleReceiptService extends AbstractSaleReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(AssuranceSaleReceiptService.class);
    private final SaleDataService saleDataService;
    private ThirdPartySaleDTO thirdPartySale;
    private boolean isEdit;

    public AssuranceSaleReceiptService(
        AppConfigurationService appConfigurationService,
        SaleDataService saleDataService,
        PrinterRepository printerRepository
    ) {
        super(appConfigurationService, printerRepository);
        this.saleDataService = saleDataService;
    }

    @Override
    protected SaleDTO getSale() {
        return thirdPartySale;
    }

    @Override
    public List<AssuranceReceiptItem> getItems() {
        return thirdPartySale.getSalesLines().stream().map(saleLineDTO -> (AssuranceReceiptItem) fromSaleLine(saleLineDTO)).toList();
    }

    public void printReceipt(String hostName, Long saleId, boolean isEdit) {
        this.isEdit = isEdit;
        thirdPartySale = (ThirdPartySaleDTO) this.saleDataService.getOneSaleDTO(saleId);
        try {
            print(hostName);
        } catch (PrinterException e) {
            LOG.error("Error while printing receipt: {}", e.getMessage());
        }
    }

    @Override
    public List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        Font font = PLAIN_FONT;
        AssuredCustomerDTO customer = (AssuredCustomerDTO) thirdPartySale.getCustomer();
        headerItems.add(
            new HeaderFooterItem(
                "Assuré: " +
                customer.getFullName() +
                " | Matricule: " +
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
                    "Bénéficiaire: " +
                    thirdPartySale.getAyantDroitFirstName().concat(" ") +
                    thirdPartySale.getAyantDroitLastName() +
                    " | Matricule: " +
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
        /* List<HeaderFooterItem> headerItems = new ArrayList<>();
        Font font = getBodyFont();
        headerItems.add(new HeaderFooterItem("Montants exprimés en FCFA", 1, font));
        return headerItems;*/
    }

    @Override
    protected int drawAssuanceInfo(Graphics2D graphics2D, int width, int margin, int y, int lineHeight) {
        int rightMargin = getRightMargin();

        FontMetrics fontMetrics;
        for (ThirdPartySaleLineDTO thirdPartySaleLine : thirdPartySale.getThirdPartySaleLines()) {
            graphics2D.setFont(PLAIN_FONT);
            graphics2D.drawString(
                thirdPartySaleLine.getPriorite().getCode().concat(": " + thirdPartySaleLine.getTiersPayantFullName()),
                margin,
                y
            );
            drawAndCenterText(graphics2D, thirdPartySaleLine.getTaux() + "%", width, margin, y);
            graphics2D.setFont(BOLD_FONT);
            fontMetrics = graphics2D.getFontMetrics(BOLD_FONT);
            String amount = NumberUtil.formatToString(thirdPartySaleLine.getMontant());
            graphics2D.drawString(amount, rightMargin - fontMetrics.stringWidth(amount), y);
            y += lineHeight;
        }

        return y;
    }

    @Override
    protected int drawSummary(Graphics2D graphics2D, int width, int y, int lineHeight) {
        int rightMargin = getRightMargin();
        graphics2D.setFont(PLAIN_FONT);
        graphics2D.drawString(MONTANT_TTC, 0, y);
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
            graphics2D.drawString(REMISE, 0, y);
            String discount = NumberUtil.formatToString(thirdPartySale.getDiscountAmount() * (-1));
            graphics2D.drawString(discount, rightMargin - fontMetrics.stringWidth(discount), y);
            y += lineHeight;
        }
        if (thirdPartySale.getTaxAmount() != null && thirdPartySale.getTaxAmount() > 0) {
            graphics2D.drawString(TOTAL_TVA, 0, y);
            String tax = NumberUtil.formatToString(thirdPartySale.getTaxAmount());
            graphics2D.drawString(tax, rightMargin - fontMetrics.stringWidth(tax), y);
            y += lineHeight;
        }
        if (nonNull(thirdPartySale.getPartTiersPayant())) {
            graphics2D.drawString("Part tiers payant", 0, y);
            String partTiersPayant = NumberUtil.formatToString(thirdPartySale.getPartTiersPayant());
            graphics2D.drawString(partTiersPayant, rightMargin - fontMetrics.stringWidth(partTiersPayant), y);
            y += lineHeight;
        }
        if (thirdPartySale.getAmountToBePaid() != null && thirdPartySale.getAmountToBePaid() > 0) {
            graphics2D.setFont(BOLD_FONT);
            graphics2D.drawString("Reste à charge", 0, y);
            fontMetrics = graphics2D.getFontMetrics(BOLD_FONT);
            String payroll = NumberUtil.formatToString(thirdPartySale.getAmountToBePaid());
            graphics2D.drawString(payroll, rightMargin - fontMetrics.stringWidth(payroll), y);
            y += lineHeight;
        }
        return y;
    }
}

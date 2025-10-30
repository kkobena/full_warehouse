package com.kobe.warehouse.service.receipt.service;

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
import com.kobe.warehouse.service.utils.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.print.PrintException;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    protected int getProductNameWidth() {
        /*if (thirdPartySale.isHasPriceOption()) {
            return 20;
        }*/
        return 22;
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

package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.DepotExtensionSaleDTO;
import com.kobe.warehouse.service.dto.MagasinDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.receipt.dto.CashSaleReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.print.PrintException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class VenteDepotReceiptService extends AbstractSaleReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(VenteDepotReceiptService.class);
    private DepotExtensionSaleDTO depotExtensionSale;
    private int avoirCount;

    public VenteDepotReceiptService(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    @Override
    protected boolean printHeaderWelcomeMessage() {
        return false;
    }

    @Override
    protected boolean printFooterNote() {
        return false;
    }

    @Override
    protected SaleDTO getSale() {
        return depotExtensionSale;
    }

    @Override
    protected int getAvoirCount() {
        return avoirCount;
    }

    @Override
    public List<CashSaleReceiptItem> getItems() {
        List<CashSaleReceiptItem> items = new ArrayList<>();
        for (SaleLineDTO line : depotExtensionSale.getSalesLines()) {
            avoirCount += (line.getQuantityRequested() - line.getQuantitySold());
            items.add((CashSaleReceiptItem) fromSaleLine(line));
        }

        return items;
    }

    public void printReceipt(String hostName, DepotExtensionSaleDTO sale, boolean isEdit) {
        this.depotExtensionSale = sale;
        try {
            printEscPosDirectByHost(hostName, isEdit);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS receipt: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        MagasinDTO depot = depotExtensionSale.getMagasin();

            headerItems.add(new HeaderFooterItem("DEPOT: " + depot.getName(), 1, PLAIN_FONT));
            if (StringUtils.hasLength(depot.getPhone())) {
                headerItems.add(new HeaderFooterItem("TEL: " + depot.getPhone(), 1, PLAIN_FONT));
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

    }



    public byte[] generateEscPosReceiptForTauri(DepotExtensionSaleDTO sale, boolean isEdit) throws IOException {
        this.depotExtensionSale = sale;
        return generateEscPosReceipt(isEdit);
    }
}

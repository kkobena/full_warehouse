package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.service.dto.CashSaleDTO;
import com.kobe.warehouse.service.dto.SaleDTO;
import com.kobe.warehouse.service.dto.SaleLineDTO;
import com.kobe.warehouse.service.dto.UninsuredCustomerDTO;
import com.kobe.warehouse.service.receipt.dto.CashSaleReceiptItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.settings.AppConfigurationService;
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
    /** Titre imprimé en tête d'un ticket particulier (ex. produits non remboursés) ; nul pour un ticket ordinaire. */
    private String titre;

    public CashSaleReceiptService(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    @Override
    protected SaleDTO getSale() {
        return cashSale;
    }

    /**
     * Le nombre d'unités non servies, calculé À CHAQUE APPEL depuis la vente courante.
     */
    @Override
    protected int getAvoirCount() {
        return cashSale.getSalesLines().stream()
            .mapToInt(line -> line.getQuantityRequested() - line.getQuantitySold()).sum();
    }

    @Override
    public List<CashSaleReceiptItem> getItems() {
        List<CashSaleReceiptItem> items = new ArrayList<>();
        for (SaleLineDTO line : cashSale.getSalesLines()) {
            items.add((CashSaleReceiptItem) fromSaleLine(line));
        }

        return items;
    }

    public void printReceipt(String hostName, CashSaleDTO sale, boolean isEdit) {
        printReceipt(hostName, sale, isEdit, null);
    }

    public void printReceipt(String hostName, CashSaleDTO sale, boolean isEdit, String titre) {
        this.cashSale = sale;
        this.titre = titre;
        try {
            printEscPosDirectByHost(hostName, isEdit);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS receipt: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        if (titre != null) {
            headerItems.add(new HeaderFooterItem(titre, 1, PLAIN_FONT));
        }
        if (cashSale.getCustomer() != null) {
            UninsuredCustomerDTO customer = (UninsuredCustomerDTO) cashSale.getCustomer();
            headerItems.add(
                new HeaderFooterItem("Client: " + customer.getFullName(), 1, PLAIN_FONT));
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

    public byte[] generateEscPosReceiptForTauri(CashSaleDTO sale, boolean isEdit)
        throws IOException {
        return generateEscPosReceiptForTauri(sale, isEdit, null);
    }

    public byte[] generateEscPosReceiptForTauri(CashSaleDTO sale, boolean isEdit, String titre)
        throws IOException {
        this.cashSale = sale;
        this.titre = titre;
        return generateEscPosReceipt(isEdit);
    }
}

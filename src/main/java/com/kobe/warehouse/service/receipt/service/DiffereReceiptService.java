package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.reglement.differe.dto.ReglementDiffereReceiptDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.print.PrintException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiffereReceiptService extends ReglementAbstractReceiptService {

    protected static final String SOLDE = "SOLDE";
    private static final Logger LOG = LoggerFactory.getLogger(DiffereReceiptService.class);
    private ReglementDiffereReceiptDTO differeReceipt;

    public DiffereReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();

        headerItems.add(new HeaderFooterItem("CLIENT: " + differeReceipt.customerfullName(), 1, PLAIN_FONT));
        headerItems.add(new HeaderFooterItem("OPERATEUR: " + differeReceipt.userfullName(), 1, PLAIN_FONT));

        return headerItems;
    }


    @Override
    protected List<HeaderFooterItem> getFooterItems() {
        return List.of();
    }


    @Override
    protected byte[] generateEscPosReceipt(boolean isEdit) throws IOException {
        magasin = appConfigurationService.getMagasin();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // Print common company header
            printEscPosCompanyHeader(out);

            // Header items (differe specific)
            escPosPrintLine(out, "CLIENT: " + differeReceipt.customerfullName());
            escPosPrintLine(out, "OPERATEUR: " + differeReceipt.userfullName());
            escPosFeedLines(out, 1);

            // Separator line
            escPosPrintSeparator(out, 48);

            // Summary section
            escPosSetBold(out, true);
            escPosPrintLine(out, String.format("%-37s %10s", MONTANT_ATTENDU, differeReceipt.formattedExpectedAmount()));
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);

            // Payment section
            escPosSetBold(out, true);
            escPosPrintLine(out, String.format("%-37s %10s", differeReceipt.libelleMode(), differeReceipt.formattedPaidAmount()));

            // Solde (balance)
            escPosPrintLine(out, String.format("%-37s %10s", SOLDE, differeReceipt.formattedSolde()));
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);

            // Cash change (if any)
            if (!differeReceipt.monnaie().isEmpty()) {
                escPosSetBold(out, false);
                escPosPrintLine(out, String.format("%-37s %10s", MONTANT_RENDU, differeReceipt.monnaie()));
                escPosFeedLines(out, 1);
            }

            // Print common footer
            printEscPosFooter(out);

            return out.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to generate ESC/POS receipt: " + e.getMessage(), e);
        } finally {
            out.close();
        }
    }


    public void printReceipt(String hostName, ReglementDiffereReceiptDTO reglementDiffereReceipt) {
        this.differeReceipt = reglementDiffereReceipt;
        try {
            // Use direct ESC/POS printing for better performance and reliability
            printEscPosDirectByHost(hostName, false);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS receipt: {}", e.getMessage(), e);
        }
    }
}

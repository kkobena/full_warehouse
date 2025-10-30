package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.repository.PrinterRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.reglement.dto.InvoicePaymentReceiptDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.print.PrintException;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvoiceReceiptService extends ReglementAbstractReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(InvoiceReceiptService.class);
    private InvoicePaymentReceiptDTO invoicePaymentReceipt;

    public InvoiceReceiptService(AppConfigurationService appConfigurationService, PrinterRepository printerRepository) {
        super(appConfigurationService, printerRepository);
    }

    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();
        Font font = PLAIN_FONT;
        headerItems.add(new HeaderFooterItem("ORGANISME: " + invoicePaymentReceipt.getOrganisme(), 1, font));
        headerItems.add(new HeaderFooterItem("FACTURE#: " + invoicePaymentReceipt.getCodeFacture(), 1, font));
        headerItems.add(new HeaderFooterItem("OPERATEUR: " + invoicePaymentReceipt.getUser(), 1, font));

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

            // Header items (invoice specific)
            escPosPrintLine(out, "ORGANISME: " + invoicePaymentReceipt.getOrganisme());
            escPosPrintLine(out, "FACTURE#: " + invoicePaymentReceipt.getCodeFacture());
            escPosPrintLine(out, "OPERATEUR: " + invoicePaymentReceipt.getUser());
            escPosFeedLines(out, 1);

            // Separator line
            escPosPrintSeparator(out, 48);

            // Summary section
            escPosSetBold(out, false);
            escPosPrintLine(out, String.format("%-37s %10s", NOMBRE_DOSSIER, invoicePaymentReceipt.getInvoicePaymentItemsCount()));

            escPosSetBold(out, true);
            escPosPrintLine(out, String.format("%-37s %10s", MONTANT_ATTENDU, invoicePaymentReceipt.getMontantAttendu()));
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);

            // Payment section
            escPosSetBold(out, true);
            escPosPrintLine(
                out,
                String.format("%-37s %10s", invoicePaymentReceipt.getPaymentMode(), invoicePaymentReceipt.getPaidAmount())
            );
            escPosSetBold(out, false);

            // Remaining amount (if any)
            if (StringUtils.hasLength(invoicePaymentReceipt.getMontantRestant())) {
                escPosSetBold(out, true);
                escPosPrintLine(out, String.format("%-37s %10s", RESTE_A_PAYER, invoicePaymentReceipt.getMontantRestant()));
                escPosSetBold(out, false);
            }
            escPosFeedLines(out, 1);

            // Print common footer
            printEscPosFooter(out);

            return out.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to generate ESC/POS receipt: " + e.getMessage(), e);
        } finally {
            out.close();
        }
    }


    public void printReceipt(String hostName, InvoicePaymentReceiptDTO invoicePaymentReceipt) {
        this.invoicePaymentReceipt = invoicePaymentReceipt;
        try {
            // Use direct ESC/POS printing for better performance and reliability
            printEscPosDirectByHost(hostName, false);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS receipt: {}", e.getMessage(), e);
        }
    }
}

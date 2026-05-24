package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.service.receipt.dto.AbstractItem;
import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.reglement.dto.DefaultPaymentRecord;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.print.PrintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MouvementCaisseReceiptService extends AbstractJava2DReceiptPrinterService {

    private static final Logger LOG = LoggerFactory.getLogger(MouvementCaisseReceiptService.class);
    private DefaultPaymentRecord paymentRecord;

    protected MouvementCaisseReceiptService(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    @Override
    protected boolean printFooterNote() {
        return false;
    }

    @Override
    protected boolean printHeaderWelcomeMessage() {
        return false;
    }

    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        List<HeaderFooterItem> headerItems = new ArrayList<>();

        headerItems.add(new HeaderFooterItem("OPERATEUR: " + paymentRecord.operateur(), 1, PLAIN_FONT));
        headerItems.add(new HeaderFooterItem("TYPE: " + paymentRecord.typeFinancialTransaction().getValue(), 1, PLAIN_FONT));
        return headerItems;
    }

    @Override
    protected List<HeaderFooterItem> getFooterItems() {
        return List.of();
    }

    @Override
    protected int getNumberOfCopies() {
        return 1;
    }

    @Override
    protected List<? extends AbstractItem> getItems() {
        return List.of();
    }

    @Override
    protected byte[] generateEscPosReceipt(boolean isEdit) throws IOException {
        magasin = appConfigurationService.getMagasin();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            // Print common company header
            printEscPosCompanyHeader(out);

            // Header items
            escPosPrintLine(out, "OPERATEUR: " + paymentRecord.operateur());
            escPosPrintLine(out, "TYPE: " + paymentRecord.typeFinancialTransaction().getValue());
            escPosFeedLines(out, 1);

            // Separator line
            escPosPrintSeparator(out, 48);

            // Payment amount section
            escPosSetBold(out, true);
            escPosPrintLine(out, String.format("%-28s %19s", "MODE", paymentRecord.modeReglement()));
            escPosPrintLine(out, String.format("%-28s %19s", "MONTANT", NumberUtil.formatToString(paymentRecord.amount())));
            escPosSetBold(out, false);
            escPosFeedLines(out, 1);

            // Transaction details
            escPosPrintLine(
                out,
                String.format("%-28s %19s", "DATE", paymentRecord.mvtDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
            );
            escPosPrintLine(out, String.format("%-28s %19s", "REFERENCE", paymentRecord.reference()));
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

    public void printReceipt(DefaultPaymentRecord paymentRecord) {
        this.paymentRecord = paymentRecord;
        try {
            printEscPosDirectByHost(null, true);
        } catch (IOException | PrintException e) {
            LOG.error("Error while printing ESC/POS receipt: {}", e.getMessage(), e);
        }
    }

    public byte[] generateEscPosReceiptForTauri(DefaultPaymentRecord paymentRecord) throws IOException {
        this.paymentRecord = paymentRecord;
        return generateEscPosReceipt(true);
    }
}

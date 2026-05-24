package com.kobe.warehouse.service.receipt.service;

import com.kobe.warehouse.service.receipt.dto.HeaderFooterItem;
import com.kobe.warehouse.service.sale.dto.RetourClientDTO;
import com.kobe.warehouse.service.sale.dto.RetourClientLineDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.utils.NumberUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.print.PrintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RetourClientReceiptService extends ReglementAbstractReceiptService {

    private static final Logger LOG = LoggerFactory.getLogger(RetourClientReceiptService.class);
    private RetourClientDTO retourDTO;

    public RetourClientReceiptService(AppConfigurationService appConfigurationService) {
        super(appConfigurationService);
    }

    @Override
    protected List<HeaderFooterItem> getHeaderItems() {
        return List.of();
    }

    @Override
    protected List<HeaderFooterItem> getFooterItems() {
        return List.of();
    }

    @Override
    protected byte[] generateEscPosReceipt(boolean isEdit) throws IOException {
        magasin = appConfigurationService.getMagasin();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            printEscPosCompanyHeader(out);

            escPosSetAlignment(out, EscPosAlignment.CENTER);
            escPosSetBold(out, true);
            escPosPrintLine(out, "BON DE RETOUR CLIENT");
            escPosSetBold(out, false);
            escPosSetAlignment(out, EscPosAlignment.LEFT);
            escPosFeedLines(out, 1);

            escPosPrintLine(out, "REF: " + retourDTO.reference());
            escPosPrintLine(out, "DATE: " + retourDTO.createdAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            if (retourDTO.customerName() != null) {
                escPosPrintLine(out, "CLIENT: " + retourDTO.customerName());
            }
            if (retourDTO.originalSaleRef() != null) {
                escPosPrintLine(out, "VENTE N°: " + retourDTO.originalSaleRef());
            }
            if (retourDTO.createdByName() != null) {
                escPosPrintLine(out, "OPERATEUR: " + retourDTO.createdByName());
            }
            escPosFeedLines(out, 1);

            escPosPrintSeparator(out, 48);
            escPosSetBold(out, true);
            escPosPrintLine(out, String.format("%-25s %5s %17s", "PRODUIT", "QTE", "MONTANT"));
            escPosSetBold(out, false);
            escPosPrintSeparator(out, 48);

            for (RetourClientLineDTO line : retourDTO.lines()) {
                escPosPrintLine(out, String.format("%-25s %5d %17s",
                    truncateString(line.produitLibelle(), 24),
                    line.quantite(),
                    NumberUtil.formatToString(line.montant())
                ));
            }

            escPosPrintSeparator(out, 48);

            if (retourDTO.montantTpTotal() > 0) {
                escPosPrintLine(out, String.format("%-32s %16s", "PART MUTUELLE", NumberUtil.formatToString(retourDTO.montantTpTotal())));
            }
            escPosSetBold(out, true);
            escPosPrintLine(out, String.format("%-32s %16s", "A REMBOURSER", NumberUtil.formatToString(retourDTO.montantTotal())));
            escPosSetBold(out, false);

            if (retourDTO.modeReglement() != null) {
                escPosFeedLines(out, 1);
                escPosPrintLine(out, "MODE: " + retourDTO.modeReglement().getLibelle());
            }
            if (retourDTO.motif() != null) {
                escPosPrintLine(out, "MOTIF: " + retourDTO.motif().getLibelle());
            }

            printEscPosFooter(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IOException("Erreur génération ticket retour: " + e.getMessage(), e);
        }
    }

    public void printReceipt(String hostName, RetourClientDTO retourDTO) {
        this.retourDTO = retourDTO;
        try {
            printEscPosDirectByHost(hostName, true);
        } catch (IOException | PrintException e) {
            LOG.error("Erreur impression bon de retour client: {}", e.getMessage(), e);
        }
    }

    public byte[] generateEscPosReceiptForTauri(RetourClientDTO retourDTO) throws IOException {
        this.retourDTO = retourDTO;
        return generateEscPosReceipt(true);
    }

}

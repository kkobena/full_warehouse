package com.kobe.warehouse.service.csv;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.OrderItem;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExportationCsvService {

    private final Logger log = LoggerFactory.getLogger(ExportationCsvService.class);

    /** La commande au format CIP / quantité, produite en mémoire. */
    public byte[] exportCommandeToCsv(Commande commande) {
        StringWriter writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL)) {
            commande
                .getOrderLines()
                .forEach(orderLine -> {
                    FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
                    Produit produit = fournisseurProduit.getProduit();
                    try {
                        printer.printRecord(
                            StringUtils.isNotEmpty(produit.getCodeEanLaboratoire())
                                ? produit.getCodeEanLaboratoire()
                                : fournisseurProduit.getCodeCip(),
                            orderLine.getQuantityRequested()
                        );
                    } catch (IOException e) {
                        log.error("Error writing data to the csv printer", e);
                    }
                });

            printer.flush();
        } catch (final IOException e) {
            throw new RuntimeException("Csv writing error: " + e.getMessage());
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Les lignes non prises en compte d'un import de commande, au format du grossiste, produites en mémoire. */
    public byte[] exporterRuptures(List<OrderItem> items, CommandeModel commandeModel) {
        StringWriter writer = new StringWriter();
        try (final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL.builder().setDelimiter(';').get())) {
            switch (commandeModel) {
                case LABOREX -> printLaborexFormatCsv(printer, items);
                case DPCI -> printDPCIFormatCsv(printer, items);
                case TEDIS -> printTEDISFormatCsv(printer, items);
                case COPHARMED -> printCOPHARMEDFormatCsv(printer, items);
                case CIP_QTE_PA -> throw new RuntimeException("Ce format n'est pas encore pris en compte");
            }
            printer.flush();
        } catch (final IOException e) {
            throw new RuntimeException("Csv writing error: " + e.getMessage());
        }
        return writer.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void printLaborexFormatCsv(CSVPrinter printer, List<OrderItem> items) throws IOException {
        printer.printRecord(
            "N° Facture",
            "N° ligne",
            "CIP/EAN13",
            "Libellé du produit",
            "Qté commandée",
            "Qté livrée",
            "Prix de cession",
            "Prix public",
            "N° commande",
            "Tva"
        );
        for (OrderItem item : items) {
            printer.printRecord(
                item.getFacture(),
                item.getLigne(),
                item.getProduitCip(),
                item.getProduitLibelle(),
                item.getQuantityRequested(),
                item.getQuantityReceived(),
                item.getMontant(),
                item.getPrixUn(),
                item.getReferenceBonLivraison(),
                item.getTva()
            );
        }
    }

    private void printDPCIFormatCsv(CSVPrinter printer, List<OrderItem> items) throws IOException {
        for (OrderItem item : items) {
            printer.printRecord(
                item.getLigne(),
                item.getProduitLibelle(),
                item.getProduitCip(),
                item.getPrixAchat(),
                item.getPrixUn(),
                item.getTva(),
                item.getQuantityReceived(),
                item.getQuantityRequested(),
                item.getReferenceBonLivraison()
            );
        }
    }

    private void printTEDISFormatCsv(CSVPrinter printer, List<OrderItem> items) throws IOException {
        for (OrderItem item : items) {
            printer.printRecord(
                item.getProduitCip(),
                item.getQuantityRequested(),
                item.getProduitCip(),
                item.getQuantityReceived(),
                item.getMontant().intValue()
            );
        }
    }

    private void printCOPHARMEDFormatCsv(CSVPrinter printer, List<OrderItem> items) throws IOException {
        printer.printRecord(
            "Date",
            "Numero Facture",
            "Numero Ligne",
            "Code Interne",
            "Code CIP",
            "Code CIP Alternatif",
            "Description",
            "Laboratoire",
            "Quantité demandée",
            "Quantitee livree",
            "Unite Gratuite",
            "Prix de Cession Hors Taxe",
            "Taux Taxe",
            "Prix public",
            "Prix TTC"
        );
        for (OrderItem item : items) {
            printer.printRecord(
                item.getDateBonLivraison(),
                item.getFacture(),
                item.getLigne(),
                "",
                item.getProduitCip(),
                "",
                item.getProduitLibelle(),
                "",
                +item.getQuantityRequested(),
                item.getQuantityReceived(),
                item.getUg(),
                item.getPrixAchat(),
                "",
                item.getPrixUn(),
                ""
            );
        }
    }

    enum CommandeHeaders {
        CODE("CIP/EAN"),
        QUANTITY("Quantite");

        private final String value;

        CommandeHeaders(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}

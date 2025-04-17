package com.kobe.warehouse.service.csv;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.dto.CommandeModel;
import com.kobe.warehouse.service.dto.OrderItem;
import com.kobe.warehouse.service.errors.FileStorageException;
import com.kobe.warehouse.service.errors.GenericError;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

@Service
public class ExportationCsvService {

    private final Logger log = LoggerFactory.getLogger(ExportationCsvService.class);
    private final Path fileStorageLocation;

    public ExportationCsvService(FileStorageProperties fileStorageProperties) {
        this.fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public String exportCommandeToCsv(Commande commande) {
        log.info("Writing data to the csv printer");
        String filename =
            this.fileStorageLocation.resolve(
                    "commande_" +
                    commande.getOrderReference() +
                    "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) +
                    ".csv"
                )
                .toFile()
                .getAbsolutePath();
        try (final FileWriter writer = new FileWriter(filename); final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL)) {
            commande
                .getOrderLines()
                .forEach(orderLine -> {
                    FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
                    Produit produit = fournisseurProduit.getProduit();
                    try {
                        printer.printRecord(
                            StringUtils.isNotEmpty(produit.getCodeEan()) ? produit.getCodeEan() : fournisseurProduit.getCodeCip(),
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
        return filename;
    }

    public void createRuptureFile(String reference, List<OrderItem> items, CommandeModel commandeModel) {
        log.info("createRuptureFile data to the csv printer");
        String filename =
            this.fileStorageLocation.resolve(
                    "rupture_" + reference + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) + ".csv"
                )
                .toFile()
                .getAbsolutePath();
        try (
            final FileWriter writer = new FileWriter(filename);
            final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL.builder().setDelimiter(';').build())
        ) {
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

    public Resource getRutureFileByOrderReference(String reference) {
        Path path =
            this.fileStorageLocation.resolve(
                    "rupture_" + reference + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd")) + ".csv"
                );
        try {
            return new UrlResource(path.toUri());
        } catch (MalformedURLException e) {
            throw new GenericError("Le fichier n'existe pas ", "duplicateProvider");
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

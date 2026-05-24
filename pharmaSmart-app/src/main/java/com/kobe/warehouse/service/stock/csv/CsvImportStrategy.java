package com.kobe.warehouse.service.stock.csv;

import com.kobe.warehouse.service.dto.OrderItem;
import com.kobe.warehouse.service.utils.DateUtil;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.apache.commons.csv.CSVRecord;

/**
 * Stratégie d'import CSV par format fournisseur.
 * Chaque constante encapsule la logique d'extraction des colonnes et la construction
 * de l'item d'échec propre à un format donné.
 */
public interface CsvImportStrategy {

    /**
     * Extrait les données d'une ligne CSV.
     *
     * @param record  la ligne CSV
     * @param rowIndex index 0-based de la ligne dans le fichier
     * @return Optional vide si la ligne doit être ignorée (ex. en-tête)
     */
    Optional<ParsedCsvRecord> extract(CSVRecord record, int rowIndex);

    /**
     * Construit l'item d'échec quand le produit n'est pas trouvé dans la base.
     */
    OrderItem onFailure(CSVRecord record, ParsedCsvRecord parsed);

    /**
     * Indique si le fichier comporte une ligne d'en-tête à ignorer.
     * Utilisé pour calculer le total d'articles affiché à l'utilisateur.
     */
    default boolean hasHeader() {
        return false;
    }

    // -----------------------------------------------------------------------
    // Format LABOREX : séparateur ';', 1 ligne d'en-tête, colonnes indexées 0-based
    // -----------------------------------------------------------------------
    CsvImportStrategy LABOREX = new CsvImportStrategy() {
        @Override
        public boolean hasHeader() {
            return true;
        }

        @Override
        public Optional<ParsedCsvRecord> extract(CSVRecord r, int idx) {
            if (idx == 0) return Optional.empty();
            String codeProduit = r.get(3);
            int quantityReceived = Integer.parseInt(r.get(7));
            int orderCostAmount = (int) Double.parseDouble(r.get(8));
            int orderUnitPrice = (int) Double.parseDouble(r.get(9));
            int taxAmount = (int) Double.parseDouble(r.get(11));
            int quantityUg = Integer.parseInt(r.get(6));
            return Optional.of(new ParsedCsvRecord(
                codeProduit, quantityReceived, quantityReceived,
                orderCostAmount, orderUnitPrice, quantityUg, taxAmount, null, null
            ));
        }

        @Override
        public OrderItem onFailure(CSVRecord r, ParsedCsvRecord p) {
            return new OrderItem()
                .setEtablissement(r.get(0))
                .setFacture(r.get(1))
                .setLigne(Integer.parseInt(r.get(2)))
                .setProduitCip(p.codeProduit())
                .setProduitLibelle(r.get(4))
                .setQuantityRequested(Integer.parseInt(r.get(5)))
                .setQuantityReceived(p.quantityReceived())
                .setMontant((double) p.orderCostAmount())
                .setPrixAchat(p.orderCostAmount())
                .setPrixUn(p.orderUnitPrice())
                .setReferenceBonLivraison(r.get(10))
                .setUg(p.quantityUg())
                .setTva(Double.parseDouble(r.get(11)));
        }
    };

    // -----------------------------------------------------------------------
    // Format COPHARMED : séparateur ';', 1 ligne d'en-tête
    // -----------------------------------------------------------------------
    CsvImportStrategy COPHARMED = new CsvImportStrategy() {
        @Override
        public boolean hasHeader() {
            return true;
        }

        @Override
        public Optional<ParsedCsvRecord> extract(CSVRecord r, int idx) {
            if (idx == 0) return Optional.empty();
            String codeProduit = r.get(4);
            int quantityReceived = Integer.parseInt(r.get(9));
            int orderCostAmount = (int) Double.parseDouble(r.get(11));
            int orderUnitPrice = (int) Double.parseDouble(r.get(13));
            int quantityUg = Integer.parseInt(r.get(10));
            int quantityRequested = Integer.parseInt(r.get(8));
            return Optional.of(new ParsedCsvRecord(
                codeProduit, quantityRequested, quantityReceived,
                orderCostAmount, orderUnitPrice, quantityUg, 0, null, null
            ));
        }

        @Override
        public OrderItem onFailure(CSVRecord r, ParsedCsvRecord p) {
            return new OrderItem()
                .setFacture(r.get(1))
                .setDateBonLivraison(r.get(0))
                .setUg(p.quantityUg())
                .setLigne(Integer.parseInt(r.get(2)))
                .setProduitCip(p.codeProduit())
                .setProduitLibelle(r.get(6))
                .setQuantityRequested(p.quantityRequested())
                .setQuantityReceived(p.quantityReceived())
                .setPrixUn(p.orderUnitPrice())
                .setPrixAchat(p.orderCostAmount());
        }
    };

    // -----------------------------------------------------------------------
    // Format DPCI : séparateur ';', pas d'en-tête
    // -----------------------------------------------------------------------
    CsvImportStrategy DPCI = new CsvImportStrategy() {
        @Override
        public Optional<ParsedCsvRecord> extract(CSVRecord r, int idx) {
            String codeProduit = r.get(2);
            int quantityReceived = Integer.parseInt(r.get(6));
            int orderCostAmount = (int) Double.parseDouble(r.get(3));
            int orderUnitPrice = (int) Double.parseDouble(r.get(4));
            int quantityRequested = Integer.parseInt(r.get(7));
            int taxAmount = (int) Double.parseDouble(r.get(5));
            return Optional.of(new ParsedCsvRecord(
                codeProduit, quantityRequested, quantityReceived,
                orderCostAmount, orderUnitPrice, 0, taxAmount, null, null
            ));
        }

        @Override
        public OrderItem onFailure(CSVRecord r, ParsedCsvRecord p) {
            return new OrderItem()
                .setReferenceBonLivraison(r.get(8))
                .setTva((double) p.taxAmount())
                .setLigne(Integer.parseInt(r.get(0)))
                .setProduitCip(p.codeProduit())
                .setProduitLibelle(r.get(1))
                .setQuantityRequested(p.quantityRequested())
                .setQuantityReceived(p.quantityReceived())
                .setPrixUn(p.orderUnitPrice())
                .setPrixAchat(p.orderCostAmount());
        }
    };

    // -----------------------------------------------------------------------
    // Format TEDIS : séparateur ';', pas d'en-tête, inclut numéro de lot et date péremption
    // -----------------------------------------------------------------------
    CsvImportStrategy TEDIS = new CsvImportStrategy() {
        @Override
        public Optional<ParsedCsvRecord> extract(CSVRecord r, int idx) {
            String codeProduit = r.get(1);
            int quantityReceived = new BigDecimal(r.get(3)).intValue();
            int orderCostAmount = new BigDecimal(r.get(2)).intValue();
            int orderUnitPrice = new BigDecimal(r.get(5)).intValue();
            String lotNumero = r.get(6);
            LocalDate datePeremption = DateUtil.fromYyyyMmDd(r.get(7));
            return Optional.of(new ParsedCsvRecord(
                codeProduit, quantityReceived, quantityReceived,
                orderCostAmount, orderUnitPrice, 0, 0, lotNumero, datePeremption
            ));
        }

        @Override
        public OrderItem onFailure(CSVRecord r, ParsedCsvRecord p) {
            return new OrderItem()
                .setLigne(Integer.parseInt(r.get(0)))
                .setProduitCip(p.codeProduit())
                .setProduitEan(p.codeProduit())
                .setPrixUn(p.orderUnitPrice())
                .setQuantityReceived(p.quantityReceived())
                .setPrixAchat(p.orderCostAmount())
                .setLotNumber(p.lotNumber())
                .setDatePeremption(r.get(7));
        }
    };

    // -----------------------------------------------------------------------
    // Format CIP_QTE : séparateur ';', 1 ligne d'en-tête possible (détection automatique)
    // -----------------------------------------------------------------------
    CsvImportStrategy CIP_QTE = new CsvImportStrategy() {
        @Override
        public boolean hasHeader() {
            return true;
        }

        @Override
        public Optional<ParsedCsvRecord> extract(CSVRecord r, int idx) {
            if (idx == 0) {
                try {
                    Integer.parseInt(r.get(1));
                } catch (NumberFormatException e) {
                    return Optional.empty(); // ligne d'en-tête non numérique
                }
            }
            String codeProduit = r.get(0);
            int quantityReceived = Integer.parseInt(r.get(1));
            return Optional.of(new ParsedCsvRecord(
                codeProduit, quantityReceived, quantityReceived,
                0, 0, 0, 0, null, null
            ));
        }

        @Override
        public OrderItem onFailure(CSVRecord r, ParsedCsvRecord p) {
            return new OrderItem()
                .setProduitCip(p.codeProduit())
                .setQuantityReceived(p.quantityReceived());
        }
    };

    // -----------------------------------------------------------------------
    // Format CIP_QTE_PA : séparateur ';', 1 ligne d'en-tête possible, inclut prix achat
    // -----------------------------------------------------------------------
    CsvImportStrategy CIP_QTE_PA = new CsvImportStrategy() {
        @Override
        public boolean hasHeader() {
            return true;
        }

        @Override
        public Optional<ParsedCsvRecord> extract(CSVRecord r, int idx) {
            if (idx == 0) {
                try {
                    Integer.parseInt(r.get(1));
                } catch (NumberFormatException e) {
                    return Optional.empty(); // ligne d'en-tête non numérique
                }
            }
            String codeProduit = r.get(0);
            int quantityReceived = Integer.parseInt(r.get(3));
            int prixAchat = Integer.parseInt(r.get(4));
            return Optional.of(new ParsedCsvRecord(
                codeProduit, quantityReceived, quantityReceived,
                prixAchat, 0, 0, 0, null, null
            ));
        }

        @Override
        public OrderItem onFailure(CSVRecord r, ParsedCsvRecord p) {
            return new OrderItem()
                .setQuantityRequested(Integer.parseInt(r.get(1)))
                .setPrixAchat(p.orderCostAmount())
                .setProduitCip(p.codeProduit())
                .setQuantityReceived(p.quantityReceived());
        }
    };
}

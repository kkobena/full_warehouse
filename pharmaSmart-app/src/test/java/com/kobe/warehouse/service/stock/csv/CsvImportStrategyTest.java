package com.kobe.warehouse.service.stock.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kobe.warehouse.service.dto.OrderItem;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("CsvImportStrategy")
class CsvImportStrategyTest {

    /** Construit une ligne CSV reelle : les strategies lisent par index de colonne. */
    private static CSVRecord ligne(String csv) {
        try (CSVParser parser = CSVParser.parse(new StringReader(csv), CSVFormat.DEFAULT.builder().setDelimiter(';').get())) {
            return parser.getRecords().getFirst();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    static List<org.junit.jupiter.params.provider.Arguments> strategiesAvecEnTete() {
        return List.of(
            org.junit.jupiter.params.provider.Arguments.of("LABOREX", CsvImportStrategy.LABOREX),
            org.junit.jupiter.params.provider.Arguments.of("COPHARMED", CsvImportStrategy.COPHARMED),
            org.junit.jupiter.params.provider.Arguments.of("CIP_QTE", CsvImportStrategy.CIP_QTE),
            org.junit.jupiter.params.provider.Arguments.of("CIP_QTE_PA", CsvImportStrategy.CIP_QTE_PA)
        );
    }

    static List<org.junit.jupiter.params.provider.Arguments> strategiesSansEnTete() {
        return List.of(
            org.junit.jupiter.params.provider.Arguments.of("DPCI", CsvImportStrategy.DPCI),
            org.junit.jupiter.params.provider.Arguments.of("TEDIS", CsvImportStrategy.TEDIS)
        );
    }

    @ParameterizedTest(name = "{0} declare une ligne d en-tete")
    @MethodSource("strategiesAvecEnTete")
    void declareUnEnTete(String nom, CsvImportStrategy strategy) {
        assertThat(strategy.hasHeader()).isTrue();
    }

    @ParameterizedTest(name = "{0} ne declare pas d en-tete")
    @MethodSource("strategiesSansEnTete")
    void neDeclarePasDEnTete(String nom, CsvImportStrategy strategy) {
        assertThat(strategy.hasHeader()).isFalse();
    }

    @Nested
    @DisplayName("LABOREX")
    class Laborex {

        /** etab;facture;ligne;cip;libelle;qteDem;ug;qteRecue;pa;pu;refBL;tva */
        private static final String LIGNE = "PH01;F-001;3;1234567;DOLIPRANE;10;2;8;400,0;800,0;BL-99;18,0";

        private static CSVRecord ligneValide() {
            return ligne("PH01;F-001;3;1234567;DOLIPRANE;10;2;8;400.0;800.0;BL-99;18.0");
        }

        @Test
        @DisplayName("ignore la premiere ligne du fichier")
        void ignoreLEnTete() {
            assertThat(CsvImportStrategy.LABOREX.extract(ligneValide(), 0)).isEmpty();
        }

        @Test
        @DisplayName("extrait les colonnes de la ligne")
        void extrait() {
            ParsedCsvRecord parsed = CsvImportStrategy.LABOREX.extract(ligneValide(), 1).orElseThrow();

            assertThat(parsed.codeProduit()).isEqualTo("1234567");
            assertThat(parsed.quantityRequested()).isEqualTo(8);
            assertThat(parsed.quantityReceived()).isEqualTo(8);
            assertThat(parsed.orderCostAmount()).isEqualTo(400);
            assertThat(parsed.orderUnitPrice()).isEqualTo(800);
            assertThat(parsed.quantityUg()).isEqualTo(2);
            assertThat(parsed.taxAmount()).isEqualTo(18);
            assertThat(parsed.lotNumber()).isNull();
            assertThat(parsed.expirationDate()).isNull();
        }

        @Test
        @DisplayName("construit l item d echec avec les colonnes propres au format")
        void itemDEchec() {
            CSVRecord r = ligneValide();
            ParsedCsvRecord parsed = CsvImportStrategy.LABOREX.extract(r, 1).orElseThrow();

            OrderItem item = CsvImportStrategy.LABOREX.onFailure(r, parsed);

            assertThat(item.getEtablissement()).isEqualTo("PH01");
            assertThat(item.getFacture()).isEqualTo("F-001");
            assertThat(item.getLigne()).isEqualTo(3);
            assertThat(item.getProduitCip()).isEqualTo("1234567");
            assertThat(item.getProduitLibelle()).isEqualTo("DOLIPRANE");
            assertThat(item.getQuantityRequested()).isEqualTo(10);
            assertThat(item.getQuantityReceived()).isEqualTo(8);
            assertThat(item.getMontant()).isEqualTo(400d);
            assertThat(item.getPrixAchat()).isEqualTo(400);
            assertThat(item.getPrixUn()).isEqualTo(800d);
            assertThat(item.getReferenceBonLivraison()).isEqualTo("BL-99");
            assertThat(item.getUg()).isEqualTo(2);
            assertThat(item.getTva()).isEqualTo(18d);
        }

        @Test
        @DisplayName("une quantite non numerique remonte en erreur")
        void quantiteInvalide() {
            CSVRecord r = ligne("PH01;F-001;3;1234567;DOLIPRANE;10;2;XX;400.0;800.0;BL-99;18.0");

            assertThatThrownBy(() -> CsvImportStrategy.LABOREX.extract(r, 1)).isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("COPHARMED")
    class Copharmed {

        /** dateBL;facture;ligne;_;cip;_;libelle;_;qteDem;qteRecue;ug;pa;_;pu */
        private static CSVRecord ligneValide() {
            return ligne("2026-04-18;F-002;5;x;1234567;x;DOLIPRANE;x;10;8;2;400.0;x;800.0");
        }

        @Test
        @DisplayName("ignore la premiere ligne du fichier")
        void ignoreLEnTete() {
            assertThat(CsvImportStrategy.COPHARMED.extract(ligneValide(), 0)).isEmpty();
        }

        @Test
        @DisplayName("extrait les colonnes de la ligne")
        void extrait() {
            ParsedCsvRecord parsed = CsvImportStrategy.COPHARMED.extract(ligneValide(), 1).orElseThrow();

            assertThat(parsed.codeProduit()).isEqualTo("1234567");
            assertThat(parsed.quantityRequested()).isEqualTo(10);
            assertThat(parsed.quantityReceived()).isEqualTo(8);
            assertThat(parsed.orderCostAmount()).isEqualTo(400);
            assertThat(parsed.orderUnitPrice()).isEqualTo(800);
            assertThat(parsed.quantityUg()).isEqualTo(2);
            assertThat(parsed.taxAmount()).isZero();
        }

        @Test
        @DisplayName("construit l item d echec avec la date de bon de livraison")
        void itemDEchec() {
            CSVRecord r = ligneValide();
            ParsedCsvRecord parsed = CsvImportStrategy.COPHARMED.extract(r, 1).orElseThrow();

            OrderItem item = CsvImportStrategy.COPHARMED.onFailure(r, parsed);

            assertThat(item.getDateBonLivraison()).isEqualTo("2026-04-18");
            assertThat(item.getFacture()).isEqualTo("F-002");
            assertThat(item.getLigne()).isEqualTo(5);
            assertThat(item.getProduitCip()).isEqualTo("1234567");
            assertThat(item.getProduitLibelle()).isEqualTo("DOLIPRANE");
            assertThat(item.getQuantityRequested()).isEqualTo(10);
            assertThat(item.getQuantityReceived()).isEqualTo(8);
            assertThat(item.getUg()).isEqualTo(2);
            assertThat(item.getPrixUn()).isEqualTo(800d);
            assertThat(item.getPrixAchat()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("DPCI")
    class Dpci {

        /** ligne;libelle;cip;pa;pu;tva;qteRecue;qteDem;refBL */
        private static CSVRecord ligneValide() {
            return ligne("7;DOLIPRANE;1234567;400.0;800.0;18.0;8;10;BL-77");
        }

        @Test
        @DisplayName("lit la premiere ligne du fichier comme une donnee")
        void pasDEnTete() {
            assertThat(CsvImportStrategy.DPCI.extract(ligneValide(), 0)).isPresent();
        }

        @Test
        @DisplayName("extrait les colonnes de la ligne")
        void extrait() {
            ParsedCsvRecord parsed = CsvImportStrategy.DPCI.extract(ligneValide(), 0).orElseThrow();

            assertThat(parsed.codeProduit()).isEqualTo("1234567");
            assertThat(parsed.quantityRequested()).isEqualTo(10);
            assertThat(parsed.quantityReceived()).isEqualTo(8);
            assertThat(parsed.orderCostAmount()).isEqualTo(400);
            assertThat(parsed.orderUnitPrice()).isEqualTo(800);
            assertThat(parsed.taxAmount()).isEqualTo(18);
            assertThat(parsed.quantityUg()).isZero();
        }

        @Test
        @DisplayName("construit l item d echec avec la reference du bon")
        void itemDEchec() {
            CSVRecord r = ligneValide();
            ParsedCsvRecord parsed = CsvImportStrategy.DPCI.extract(r, 0).orElseThrow();

            OrderItem item = CsvImportStrategy.DPCI.onFailure(r, parsed);

            assertThat(item.getReferenceBonLivraison()).isEqualTo("BL-77");
            assertThat(item.getTva()).isEqualTo(18d);
            assertThat(item.getLigne()).isEqualTo(7);
            assertThat(item.getProduitCip()).isEqualTo("1234567");
            assertThat(item.getProduitLibelle()).isEqualTo("DOLIPRANE");
            assertThat(item.getQuantityRequested()).isEqualTo(10);
            assertThat(item.getQuantityReceived()).isEqualTo(8);
            assertThat(item.getPrixUn()).isEqualTo(800d);
            assertThat(item.getPrixAchat()).isEqualTo(400);
        }
    }

    @Nested
    @DisplayName("TEDIS")
    class Tedis {

        /** ligne;cip;pa;qteRecue;_;pu;lot;datePeremption */
        private static CSVRecord ligneValide() {
            return ligne("9;1234567;400;8;x;800;LOT-A;20270630");
        }

        @Test
        @DisplayName("lit la premiere ligne du fichier comme une donnee")
        void pasDEnTete() {
            assertThat(CsvImportStrategy.TEDIS.extract(ligneValide(), 0)).isPresent();
        }

        @Test
        @DisplayName("extrait le lot et la date de peremption")
        void extrait() {
            ParsedCsvRecord parsed = CsvImportStrategy.TEDIS.extract(ligneValide(), 0).orElseThrow();

            assertThat(parsed.codeProduit()).isEqualTo("1234567");
            assertThat(parsed.quantityRequested()).isEqualTo(8);
            assertThat(parsed.quantityReceived()).isEqualTo(8);
            assertThat(parsed.orderCostAmount()).isEqualTo(400);
            assertThat(parsed.orderUnitPrice()).isEqualTo(800);
            assertThat(parsed.lotNumber()).isEqualTo("LOT-A");
            assertThat(parsed.expirationDate()).isEqualTo(LocalDate.of(2027, 6, 30));
        }

        @Test
        @DisplayName("tronque les montants decimaux vers l entier inferieur")
        void montantsDecimaux() {
            ParsedCsvRecord parsed = CsvImportStrategy.TEDIS
                .extract(ligne("9;1234567;400.99;8.7;x;800.5;LOT-A;20270630"), 0)
                .orElseThrow();

            assertThat(parsed.orderCostAmount()).isEqualTo(400);
            assertThat(parsed.quantityReceived()).isEqualTo(8);
            assertThat(parsed.orderUnitPrice()).isEqualTo(800);
        }

        @Test
        @DisplayName("construit l item d echec avec code CIP et EAN identiques")
        void itemDEchec() {
            CSVRecord r = ligneValide();
            ParsedCsvRecord parsed = CsvImportStrategy.TEDIS.extract(r, 0).orElseThrow();

            OrderItem item = CsvImportStrategy.TEDIS.onFailure(r, parsed);

            assertThat(item.getLigne()).isEqualTo(9);
            assertThat(item.getProduitCip()).isEqualTo("1234567");
            assertThat(item.getProduitEan()).isEqualTo("1234567");
            assertThat(item.getPrixUn()).isEqualTo(800d);
            assertThat(item.getQuantityReceived()).isEqualTo(8);
            assertThat(item.getPrixAchat()).isEqualTo(400);
            assertThat(item.getLotNumber()).isEqualTo("LOT-A");
            assertThat(item.getDatePeremption()).isEqualTo("20270630");
        }

        @Test
        @DisplayName("une date de peremption hors format remonte en erreur")
        void dateInvalide() {
            CSVRecord r = ligne("9;1234567;400;8;x;800;LOT-A;30/06/2027");

            assertThatThrownBy(() -> CsvImportStrategy.TEDIS.extract(r, 0))
                .isInstanceOf(java.time.format.DateTimeParseException.class);
        }
    }

    @Nested
    @DisplayName("CIP_QTE")
    class CipQte {

        @Test
        @DisplayName("ignore une premiere ligne dont la quantite n est pas numerique")
        void ignoreLEnTete() {
            assertThat(CsvImportStrategy.CIP_QTE.extract(ligne("CIP;QUANTITE"), 0)).isEmpty();
        }

        @Test
        @DisplayName("traite une premiere ligne numerique comme une donnee")
        void premiereLigneNumerique() {
            ParsedCsvRecord parsed = CsvImportStrategy.CIP_QTE.extract(ligne("1234567;8"), 0).orElseThrow();

            assertThat(parsed.codeProduit()).isEqualTo("1234567");
            assertThat(parsed.quantityReceived()).isEqualTo(8);
        }

        @Test
        @DisplayName("extrait code et quantite, tous les montants a zero")
        void extrait() {
            ParsedCsvRecord parsed = CsvImportStrategy.CIP_QTE.extract(ligne("1234567;8"), 1).orElseThrow();

            assertThat(parsed.quantityRequested()).isEqualTo(8);
            assertThat(parsed.quantityReceived()).isEqualTo(8);
            assertThat(parsed.orderCostAmount()).isZero();
            assertThat(parsed.orderUnitPrice()).isZero();
            assertThat(parsed.quantityUg()).isZero();
            assertThat(parsed.taxAmount()).isZero();
            assertThat(parsed.lotNumber()).isNull();
            assertThat(parsed.expirationDate()).isNull();
        }

        @Test
        @DisplayName("construit un item d echec minimal")
        void itemDEchec() {
            CSVRecord r = ligne("1234567;8");
            ParsedCsvRecord parsed = CsvImportStrategy.CIP_QTE.extract(r, 1).orElseThrow();

            OrderItem item = CsvImportStrategy.CIP_QTE.onFailure(r, parsed);

            assertThat(item.getProduitCip()).isEqualTo("1234567");
            assertThat(item.getQuantityReceived()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("CIP_QTE_PA")
    class CipQtePa {

        /** cip;qteDem;_;qteRecue;prixAchat */
        private static CSVRecord ligneValide() {
            return ligne("1234567;10;x;8;400");
        }

        @Test
        @DisplayName("ignore une premiere ligne dont la deuxieme colonne n est pas numerique")
        void ignoreLEnTete() {
            assertThat(CsvImportStrategy.CIP_QTE_PA.extract(ligne("CIP;QUANTITE;x;RECU;PA"), 0)).isEmpty();
        }

        @Test
        @DisplayName("traite une premiere ligne numerique comme une donnee")
        void premiereLigneNumerique() {
            assertThat(CsvImportStrategy.CIP_QTE_PA.extract(ligneValide(), 0)).isPresent();
        }

        @Test
        @DisplayName("extrait le prix d achat")
        void extrait() {
            ParsedCsvRecord parsed = CsvImportStrategy.CIP_QTE_PA.extract(ligneValide(), 1).orElseThrow();

            assertThat(parsed.codeProduit()).isEqualTo("1234567");
            assertThat(parsed.quantityRequested()).isEqualTo(8);
            assertThat(parsed.quantityReceived()).isEqualTo(8);
            assertThat(parsed.orderCostAmount()).isEqualTo(400);
            assertThat(parsed.orderUnitPrice()).isZero();
        }

        @Test
        @DisplayName("l item d echec reprend la quantite demandee de la ligne")
        void itemDEchec() {
            CSVRecord r = ligneValide();
            ParsedCsvRecord parsed = CsvImportStrategy.CIP_QTE_PA.extract(r, 1).orElseThrow();

            OrderItem item = CsvImportStrategy.CIP_QTE_PA.onFailure(r, parsed);

            assertThat(item.getQuantityRequested()).isEqualTo(10);
            assertThat(item.getPrixAchat()).isEqualTo(400);
            assertThat(item.getProduitCip()).isEqualTo("1234567");
            assertThat(item.getQuantityReceived()).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("ParsedCsvRecord")
    class ParsedCsvRecordTest {

        @Test
        @DisplayName("expose toutes ses composantes")
        void composantes() {
            ParsedCsvRecord record = new ParsedCsvRecord("1234567", 10, 8, 400, 800, 2, 18, "LOT-A", LocalDate.of(2027, 6, 30));

            assertThat(record.codeProduit()).isEqualTo("1234567");
            assertThat(record.quantityRequested()).isEqualTo(10);
            assertThat(record.quantityReceived()).isEqualTo(8);
            assertThat(record.orderCostAmount()).isEqualTo(400);
            assertThat(record.orderUnitPrice()).isEqualTo(800);
            assertThat(record.quantityUg()).isEqualTo(2);
            assertThat(record.taxAmount()).isEqualTo(18);
            assertThat(record.lotNumber()).isEqualTo("LOT-A");
            assertThat(record.expirationDate()).isEqualTo(LocalDate.of(2027, 6, 30));
        }

        @Test
        @DisplayName("egalite et empreinte de valeur")
        void egalite() {
            ParsedCsvRecord a = new ParsedCsvRecord("1234567", 10, 8, 400, 800, 2, 18, "LOT-A", LocalDate.of(2027, 6, 30));
            ParsedCsvRecord b = new ParsedCsvRecord("1234567", 10, 8, 400, 800, 2, 18, "LOT-A", LocalDate.of(2027, 6, 30));
            ParsedCsvRecord c = new ParsedCsvRecord("7654321", 10, 8, 400, 800, 2, 18, "LOT-A", LocalDate.of(2027, 6, 30));

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
            assertThat(a.toString()).contains("1234567");
        }
    }

    @Nested
    @DisplayName("ReponseCommandeColumnMap")
    class ReponseCommandeColumnMapTest {

        @Test
        @DisplayName("expose ses composantes")
        void composantes() {
            ReponseCommandeColumnMap map = new ReponseCommandeColumnMap(0, 3, true);

            assertThat(map.cipCol()).isZero();
            assertThat(map.qteCol()).isEqualTo(3);
            assertThat(map.hasHeader()).isTrue();
        }

        @Test
        @DisplayName("egalite et empreinte de valeur")
        void egalite() {
            ReponseCommandeColumnMap a = new ReponseCommandeColumnMap(0, 3, true);
            ReponseCommandeColumnMap b = new ReponseCommandeColumnMap(0, 3, true);
            ReponseCommandeColumnMap c = new ReponseCommandeColumnMap(1, 3, false);

            assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(c);
            assertThat(a.toString()).contains("cipCol");
        }
    }
}

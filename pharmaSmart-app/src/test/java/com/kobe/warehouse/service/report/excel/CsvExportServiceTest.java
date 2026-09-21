package com.kobe.warehouse.service.report.excel;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tous les exports CSV du logiciel passent par ce service. Le fichier produit n'est pas lu par le
 * logiciel mais par <b>Excel</b>, sur un poste Windows francophone — et c'est ce qui commande ses
 * deux particularités.
 *
 * <p>Le <b>point-virgule</b> comme séparateur : Excel en locale française attend celui-là, et un
 * fichier séparé par des virgules s'y ouvre en une seule colonne illisible. Et le <b>BOM UTF-8</b>,
 * sans lequel Excel lit le fichier en ANSI et affiche « Libellé » en « LibellÃ© » — sur un export de
 * produits pharmaceutiques, tous les accents y passent.
 *
 * <p>L'échappement est délégué à la bibliothèque, mais il vaut d'être éprouvé : un libellé de produit
 * contenant un point-virgule décalerait toutes les colonnes suivantes s'il n'était pas protégé.
 */
@DisplayName("CsvExportService — socle des exports CSV")
class CsvExportServiceTest {

    private CsvExportService service;

    @BeforeEach
    void setUp() {
        service = new CsvExportService();
    }

    // ===== format du fichier =====

    @Nested
    @DisplayName("Format du fichier")
    class FormatDuFichier {

        /** Excel en français attend le point-virgule ; la virgule lui donne une colonne unique. */
        @Test
        @DisplayName("les colonnes sont séparées par des points-virgules")
        void separateurPointVirgule() throws IOException {
            String csv = csv("Alertes", entetes("Code", "Libellé"), List.<String[]>of(ligne("CIP1", "DOLIPRANE")));

            assertThat(csv).contains("Code;Libellé");
            assertThat(csv).contains("CIP1;DOLIPRANE");
        }

        /** Le titre et la date précèdent les en-têtes, comme dans l'export Excel du même rapport. */
        @Test
        @DisplayName("les en-têtes suivent le titre et la date")
        void entetesApresLeTitre() throws IOException {
            String csv = csv("Alertes", entetes("Code", "Libellé", "Stock"), List.of());

            assertThat(lignes(csv)).hasSize(3);
            assertThat(lignes(csv).get(0)).isEqualTo("Alertes");
            assertThat(lignes(csv).get(1)).startsWith("Généré le:");
            assertThat(lignes(csv).get(2)).isEqualTo("Code;Libellé;Stock");
        }

        @Test
        @DisplayName("chaque donnée occupe sa propre ligne")
        void uneLigneParDonnee() throws IOException {
            String csv = csv(
                "Alertes",
                entetes("Code", "Libellé"),
                List.<String[]>of(ligne("CIP1", "DOLIPRANE"), ligne("CIP2", "EFFERALGAN"), ligne("CIP3", "ZOVIRAX"))
            );

            assertThat(lignes(csv)).hasSize(6); // titre, date, en-têtes et trois données
        }

        /** Un libellé contenant le séparateur décalerait toutes les colonnes suivantes. */
        @Test
        @DisplayName("un point-virgule dans une valeur est protégé")
        void separateurDansUneValeur() throws IOException {
            String csv = csv("Alertes", entetes("Code", "Libellé"), List.<String[]>of(ligne("CIP1", "DOLIPRANE 1000 mg; boîte de 8")));

            assertThat(csv).contains("\"DOLIPRANE 1000 mg; boîte de 8\"");
            assertThat(lignes(csv)).hasSize(4); // titre, date, en-têtes et une donnée
        }

        @Test
        @DisplayName("un guillemet dans une valeur est protégé")
        void guillemetDansUneValeur() throws IOException {
            String csv = csv("Alertes", entetes("Code", "Libellé"), List.<String[]>of(ligne("CIP1", "SIROP \"ENFANT\"")));

            assertThat(lignes(csv)).hasSize(4); // titre, date, en-têtes et une donnée
            assertThat(csv).contains("\"\"ENFANT\"\"");
        }

        @Test
        @DisplayName("un export sans donnée conserve son titre et ses en-têtes")
        void exportSansDonnee() throws IOException {
            String csv = csv("Alertes", entetes("Code", "Libellé"), List.of());

            assertThat(csv).contains("Alertes").contains("Code;Libellé");
        }

        /**
         * Le titre est documenté comme « ajouté en première ligne », et l'export Excel du même
         * rapport le place bien en tête de la feuille. Deux exports du même écran doivent se
         * ressembler : reçu par courriel, un CSV sans titre ne dit pas de quel rapport il s'agit.
         */
        @Test
        @DisplayName("le titre du rapport ouvre le fichier")
        void titreEnTete() throws IOException {
            String csv = csv("Alertes Stock - RUPTURE", entetes("Code", "Libellé"), List.<String[]>of(ligne("CIP1", "DOLIPRANE")));

            assertThat(csv).contains("Alertes Stock - RUPTURE");
        }

        @Test
        @DisplayName("le fichier est encodé en UTF-8")
        void encodageUtf8() throws IOException {
            byte[] octets = service.createCsvReport("Alertes", entetes("Libellé"), List.<String[]>of(ligne("Périmé")), r -> r);

            assertThat(new String(octets, StandardCharsets.UTF_8)).contains("Périmé");
        }
    }

    // ===== marque d'ordre des octets =====

    @Nested
    @DisplayName("Marque d'ordre des octets")
    class MarqueDOrdreDesOctets {

        /**
         * Sans cette marque, Excel sur Windows lit le fichier en ANSI : « Libellé » devient
         * « LibellÃ© », et un export de produits pharmaceutiques devient illisible de bout en bout.
         */
        @Test
        @DisplayName("la marque UTF-8 précède le contenu")
        void marquePrecedeLeContenu() throws IOException {
            byte[] sansMarque = service.createCsvReport("Alertes", entetes("Libellé"), List.<String[]>of(ligne("Périmé")), r -> r);

            byte[] avecMarque = service.addUtf8Bom(sansMarque);

            assertThat(avecMarque).startsWith((byte) 0xEF, (byte) 0xBB, (byte) 0xBF);
            assertThat(avecMarque).hasSize(sansMarque.length + 3);
        }

        @Test
        @DisplayName("le contenu reste intact derrière la marque")
        void contenuIntact() throws IOException {
            byte[] sansMarque = service.createCsvReport("Alertes", entetes("Libellé"), List.<String[]>of(ligne("Périmé")), r -> r);

            byte[] avecMarque = service.addUtf8Bom(sansMarque);

            byte[] apresMarque = new byte[sansMarque.length];
            System.arraycopy(avecMarque, 3, apresMarque, 0, sansMarque.length);
            assertThat(apresMarque).isEqualTo(sansMarque);
        }

        @Test
        @DisplayName("un contenu vide reste marquable")
        void contenuVide() {
            assertThat(service.addUtf8Bom(new byte[0])).hasSize(3);
        }
    }

    // ===== export simple =====

    @Nested
    @DisplayName("Export simple")
    class ExportSimple {

        /** Certains rapports fournissent déjà leurs lignes prêtes : elles passent sans transformation. */
        @Test
        @DisplayName("des lignes déjà formées sont écrites telles quelles")
        void lignesDejaFormees() throws IOException {
            byte[] octets = service.createSimpleCsvReport(
                "Alertes",
                entetes("Code", "Libellé"),
                List.<String[]>of(ligne("CIP1", "DOLIPRANE"), ligne("CIP2", "EFFERALGAN"))
            );

            String csv = new String(octets, StandardCharsets.UTF_8);

            assertThat(csv).contains("CIP1;DOLIPRANE").contains("CIP2;EFFERALGAN");
        }
    }

    // ===== fabriques =====

    private String csv(String titre, String[] entetes, List<String[]> donnees) throws IOException {
        return new String(service.createCsvReport(titre, entetes, donnees, r -> r), StandardCharsets.UTF_8);
    }

    private static String[] entetes(String... entetes) {
        return entetes;
    }

    private static String[] ligne(String... valeurs) {
        return valeurs;
    }

    private static List<String> lignes(String csv) {
        return csv.lines().filter(l -> !l.isBlank()).toList();
    }

}

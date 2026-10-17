package com.kobe.warehouse.service.stock.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.service.dto.DataMatrixInfo;
import com.kobe.warehouse.service.stock.DataMatrixParserService.BarcodeType;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("DataMatrixParserServiceImpl")
class DataMatrixParserServiceImplTest {

    /** Séparateur de groupe GS1 (ASCII 29). */
    private static final String GS = String.valueOf((char) 29);

    /** EAN-13 dont la clé de contrôle est juste. */
    private static final String EAN_13_VALIDE = "4006381333931";

    /** EAN-8 dont la clé de contrôle est juste. */
    private static final String EAN_8_VALIDE = "96385074";

    /** EAN-13 a cle juste dont les deux premiers chiffres forment l AI 01 (prefixe GS1 americain). */
    private static final String EAN_13_PREFIXE_AI = "0112345678902";

    /** EAN-8 a cle juste dont les deux premiers chiffres forment l AI 10. */
    private static final String EAN_8_PREFIXE_AI = "10234563";

    /** GTIN-14 dont le chiffre indicateur est zero : l EAN-13 s en deduit. */
    private static final String GTIN_14_INDICATEUR_ZERO = "03400930000000";

    private DataMatrixParserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DataMatrixParserServiceImpl();
    }

    @Nested
    @DisplayName("detectBarcodeType")
    class DetectBarcodeType {

        @ParameterizedTest(name = "[{0}] est inconnu")
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "abcdef", "12345", "123456789", "1234567890123456" })
        void codesInconnus(String code) {
            assertThat(service.detectBarcodeType(code)).isEqualTo(BarcodeType.UNKNOWN);
        }

        @Test
        @DisplayName("sept chiffres : CIP-7")
        void cip7() {
            assertThat(service.detectBarcodeType("1234567")).isEqualTo(BarcodeType.CIP_7);
        }

        @Test
        @DisplayName("huit chiffres a cle juste : EAN-8")
        void ean8() {
            assertThat(service.detectBarcodeType(EAN_8_VALIDE)).isEqualTo(BarcodeType.EAN_8);
        }

        @Test
        @DisplayName("huit chiffres a cle fausse : inconnu")
        void ean8CleFausse() {
            assertThat(service.detectBarcodeType("96385075")).isEqualTo(BarcodeType.UNKNOWN);
        }

        @Test
        @DisplayName("treize chiffres prefixes 340 : CIP-13")
        void cip13() {
            assertThat(service.detectBarcodeType("3400930000000")).isEqualTo(BarcodeType.CIP_13);
        }

        @Test
        @DisplayName("treize chiffres a cle juste : EAN-13")
        void ean13() {
            assertThat(service.detectBarcodeType(EAN_13_VALIDE)).isEqualTo(BarcodeType.EAN_13);
        }

        @Test
        @DisplayName("treize chiffres a cle fausse et sans prefixe : inconnu")
        void ean13CleFausse() {
            assertThat(service.detectBarcodeType("4006381333932")).isEqualTo(BarcodeType.UNKNOWN);
        }

        @Test
        @DisplayName("les espaces autour du code sont ignores")
        void espacesIgnores() {
            assertThat(service.detectBarcodeType("  1234567  ")).isEqualTo(BarcodeType.CIP_7);
        }

        @Test
        @DisplayName("un identifiant d application GS1 prime sur la detection par longueur")
        void aiPrimeSurLaLongueur() {
            assertThat(service.detectBarcodeType("0103400930000000")).isEqualTo(BarcodeType.DATAMATRIX);
        }

        @Test
        @DisplayName("quatorze chiffres : GTIN-14")
        void gtin14() {
            assertThat(service.detectBarcodeType(GTIN_14_INDICATEUR_ZERO)).isEqualTo(BarcodeType.GTIN_14);
        }

        @Test
        @DisplayName("un EAN-13 debutant par un AI connu reste un EAN-13")
        void ean13DebutantParUnAi() {
            assertThat(service.detectBarcodeType(EAN_13_PREFIXE_AI)).isEqualTo(BarcodeType.EAN_13);
        }

        @Test
        @DisplayName("un EAN-8 debutant par un AI connu reste un EAN-8")
        void ean8DebutantParUnAi() {
            assertThat(service.detectBarcodeType(EAN_8_PREFIXE_AI)).isEqualTo(BarcodeType.EAN_8);
        }

        @Test
        @DisplayName("une trame AI 17 plus date, de meme longueur qu un EAN-8, reste un DataMatrix")
        void datePeremptionNEstPasUnEan8() {
            assertThat(service.detectBarcodeType("17270630")).isEqualTo(BarcodeType.DATAMATRIX);
        }

        @Test
        @DisplayName("sept chiffres debutant par un AI connu : GTIN tronque, donc inconnu")
        void gtinTronqueNEstPasUnCip7() {
            assertThat(service.detectBarcodeType("0134009")).isEqualTo(BarcodeType.DATAMATRIX);
        }

        @Test
        @DisplayName("le FNC1 imprimable en tete est retire avant la detection")
        void fnc1Imprimable() {
            assertThat(service.detectBarcodeType("è" + EAN_13_VALIDE)).isEqualTo(BarcodeType.EAN_13);
        }

        @Test
        @DisplayName("un separateur de groupe en tete est retire avant la detection")
        void separateurEnTete() {
            assertThat(service.detectBarcodeType(GS + EAN_13_VALIDE)).isEqualTo(BarcodeType.EAN_13);
        }
    }

    @Nested
    @DisplayName("parse : GTIN-14 nu")
    class ParseGtin14 {

        @Test
        @DisplayName("un GTIN-14 a indicateur zero donne le GTIN et l EAN-13")
        void indicateurZero() {
            DataMatrixInfo info = service.parse(GTIN_14_INDICATEUR_ZERO).orElseThrow();

            assertThat(info.gtin()).isEqualTo(GTIN_14_INDICATEUR_ZERO);
            assertThat(info.ean13()).isEqualTo("3400930000000");
            assertThat(info.getProductCode()).isEqualTo("3400930000000");
        }

        @Test
        @DisplayName("un GTIN-14 a indicateur non nul ne donne que le GTIN")
        void indicateurNonNul() {
            DataMatrixInfo info = service.parse("13400930000000").orElseThrow();

            assertThat(info.gtin()).isEqualTo("13400930000000");
            assertThat(info.ean13()).isNull();
            assertThat(info.getProductCode()).isEqualTo("13400930000000");
        }
    }

    @Nested
    @DisplayName("isValidEan8")
    class IsValidEan8 {

        @Test
        @DisplayName("accepte une cle de controle juste")
        void cleJuste() {
            assertThat(service.isValidEan8(EAN_8_VALIDE)).isTrue();
        }

        @ParameterizedTest(name = "[{0}] est refuse")
        @NullAndEmptySource
        @ValueSource(strings = { "9638507", "963850745", "9638507a", "96385075" })
        void refuse(String code) {
            assertThat(service.isValidEan8(code)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidEan13")
    class IsValidEan13 {

        @Test
        @DisplayName("accepte une cle de controle juste")
        void cleJuste() {
            assertThat(service.isValidEan13(EAN_13_VALIDE)).isTrue();
        }

        @ParameterizedTest(name = "[{0}] est refuse")
        @NullAndEmptySource
        @ValueSource(strings = { "400638133393", "40063813339311", "400638133393a", "4006381333932" })
        void refuse(String code) {
            assertThat(service.isValidEan13(code)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidCip13")
    class IsValidCip13 {

        @Test
        @DisplayName("accepte treize chiffres prefixes 340")
        void accepte() {
            assertThat(service.isValidCip13("3400930000000")).isTrue();
        }

        @ParameterizedTest(name = "[{0}] est refuse")
        @NullAndEmptySource
        @ValueSource(strings = { "340093000000", "34009300000001", "340093000000a", "4006381333931" })
        void refuse(String code) {
            assertThat(service.isValidCip13(code)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidCip7")
    class IsValidCip7 {

        @Test
        @DisplayName("accepte sept chiffres")
        void accepte() {
            assertThat(service.isValidCip7("1234567")).isTrue();
        }

        @ParameterizedTest(name = "[{0}] est refuse")
        @NullAndEmptySource
        @ValueSource(strings = { "123456", "12345678", "123456a" })
        void refuse(String code) {
            assertThat(service.isValidCip7(code)).isFalse();
        }
    }

    @Nested
    @DisplayName("isValidDataMatrix")
    class IsValidDataMatrix {

        @ParameterizedTest(name = "le prefixe AI [{0}] est reconnu")
        @ValueSource(strings = { "01", "10", "11", "17", "21", "710", "711", "712", "713", "714" })
        void prefixesReconnus(String ai) {
            assertThat(service.isValidDataMatrix(ai + "0000000000")).isTrue();
        }

        @ParameterizedTest(name = "[{0}] n est pas un DataMatrix")
        @NullAndEmptySource
        @ValueSource(strings = { "   ", "9638507400000", "3400930000000" })
        void nonReconnus(String code) {
            assertThat(service.isValidDataMatrix(code)).isFalse();
        }

        @Test
        @DisplayName("l identifiant de symbologie ]d2 est retire avant la detection")
        void symbologieD2() {
            assertThat(service.isValidDataMatrix("]d201034009300000005")).isTrue();
        }

        @Test
        @DisplayName("l identifiant de symbologie ]C1 est retire avant la detection")
        void symbologieC1() {
            assertThat(service.isValidDataMatrix("]C101034009300000005")).isTrue();
        }

        @Test
        @DisplayName("un separateur de groupe en tete est retire avant la detection")
        void separateurEnTete() {
            assertThat(service.isValidDataMatrix(GS + "01034009300000005")).isTrue();
        }
    }

    @Nested
    @DisplayName("parse : codes simples")
    class ParseCodesSimples {

        @ParameterizedTest(name = "[{0}] ne donne aucun resultat")
        @NullAndEmptySource
        @ValueSource(strings = { "   " })
        void aucunResultat(String code) {
            assertThat(service.parse(code)).isEmpty();
        }

        @ParameterizedTest(name = "[{0}] remonte en code brut")
        @ValueSource(strings = { "abcdef", "12345", "GROS/A-7788" })
        @DisplayName("un code hors nomenclature est remonte brut, sans etre interprete")
        void codeBrut(String code) {
            DataMatrixInfo info = service.parse(code).orElseThrow();

            assertThat(info.rawCode()).isEqualTo(code);
            assertThat(info.getProductCode()).isEqualTo(code);
            assertThat(info.cip13()).isNull();
            assertThat(info.ean13()).isNull();
            assertThat(info.gtin()).isNull();
            assertThat(info.batchNumber()).isNull();
            assertThat(info.expiryDate()).isNull();
        }

        @Test
        @DisplayName("un code brut reste de type inconnu : rien n en est deduit")
        void codeBrutResteInconnu() {
            assertThat(service.detectBarcodeType("GROS/A-7788")).isEqualTo(BarcodeType.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("parse : mode proposition (texte libre sans AI GS1)")
    class ParseHeuristique {

        /** EAN-13 a cle juste, utilise dans les chaines en texte libre. */
        private static final String EAN_LIBRE = "3400933399931";

        @Test
        @DisplayName("lit code, lot et peremption d une chaine etiquetee")
        void chaineEtiquetee() {
            DataMatrixInfo info = service.parse("LOT:ABC123/EXP:20270131/EAN:" + EAN_LIBRE).orElseThrow();

            assertThat(info.ean13()).isEqualTo(EAN_LIBRE);
            assertThat(info.cip13()).isEqualTo(EAN_LIBRE);
            assertThat(info.batchNumber()).isEqualTo("ABC123");
            assertThat(info.expiryDate()).isEqualTo(LocalDate.of(2027, 1, 31));
            assertThat(info.proposition()).isTrue();
        }

        @Test
        @DisplayName("les valeurs deduites ne sont jamais certaines")
        void jamaisCertaine() {
            DataMatrixInfo info = service.parse("LOT:ABC123/EXP:20270131/EAN:" + EAN_LIBRE).orElseThrow();

            assertThat(info.hasBatchInfo()).isTrue();
            assertThat(info.hasBatchInfoCertaine()).isFalse();
        }

        @Test
        @DisplayName("un lot lu dans un AI GS1 reste certain")
        void gs1ResteCertain() {
            DataMatrixInfo info = service.parse("10LOT2026A" + GS + "17270630").orElseThrow();

            assertThat(info.proposition()).isFalse();
            assertThat(info.hasBatchInfoCertaine()).isTrue();
        }

        @Test
        @DisplayName("lit une chaine separee par des barres verticales, au format AAMMJJ")
        void chaineSepareeParBarres() {
            DataMatrixInfo info = service.parse("LOT ABC123|270131|" + EAN_LIBRE).orElseThrow();

            assertThat(info.ean13()).isEqualTo(EAN_LIBRE);
            assertThat(info.batchNumber()).isEqualTo("ABC123");
            assertThat(info.expiryDate()).isEqualTo(LocalDate.of(2027, 1, 31));
        }

        @Test
        @DisplayName("un GTIN-14 en texte libre donne aussi l EAN-13")
        void gtin14EnTexteLibre() {
            DataMatrixInfo info = service.parse("BATCH=XY9/0" + EAN_LIBRE).orElseThrow();

            assertThat(info.gtin()).isEqualTo("0" + EAN_LIBRE);
            assertThat(info.ean13()).isEqualTo(EAN_LIBRE);
            assertThat(info.batchNumber()).isEqualTo("XY9");
        }

        @Test
        @DisplayName("un lot non etiquete n est pas devine")
        void lotNonEtiqueteIgnore() {
            DataMatrixInfo info = service.parse("ABC123 " + EAN_LIBRE).orElseThrow();

            assertThat(info.ean13()).isEqualTo(EAN_LIBRE);
            assertThat(info.batchNumber()).isNull();
        }

        @Test
        @DisplayName("treize chiffres a cle fausse ne sont pas pris pour un code produit")
        void cleFausseRefusee() {
            DataMatrixInfo info = service.parse("LOT:ABC123/EAN:4006381333932").orElseThrow();

            assertThat(info.ean13()).isNull();
            assertThat(info.batchNumber()).isEqualTo("ABC123");
            // Sans code produit, la chaine entiere est remontee pour pouvoir etre memorisee.
            assertThat(info.rawCode()).isEqualTo("LOT:ABC123/EAN:4006381333932");
        }

        @Test
        @DisplayName("une date passee est ignoree : c est une fabrication ou un faux positif")
        void datePasseeIgnoree() {
            DataMatrixInfo info = service.parse("LOT:ABC123/EXP:20100131/EAN:" + EAN_LIBRE).orElseThrow();

            assertThat(info.expiryDate()).isNull();
            assertThat(info.batchNumber()).isEqualTo("ABC123");
        }

        @Test
        @DisplayName("entre deux dates futures, la plus proche est retenue")
        void dateLaPlusProche() {
            DataMatrixInfo info = service.parse("LOT:A1/20300131/20270630/EAN:" + EAN_LIBRE).orElseThrow();

            assertThat(info.expiryDate()).isEqualTo(LocalDate.of(2027, 6, 30));
        }

        @Test
        @DisplayName("un motif de date impossible ne fait pas echouer la lecture")
        void dateImpossibleIgnoree() {
            DataMatrixInfo info = service.parse("LOT:ABC123/EXP:20270230/EAN:" + EAN_LIBRE).orElseThrow();

            assertThat(info.expiryDate()).isNull();
            assertThat(info.ean13()).isEqualTo(EAN_LIBRE);
        }

        @Test
        @DisplayName("sans aucun motif reconnu, le code reste brut et non propose")
        void aucunMotifReconnu() {
            DataMatrixInfo info = service.parse("GROS/A-7788").orElseThrow();

            assertThat(info.rawCode()).isEqualTo("GROS/A-7788");
            assertThat(info.proposition()).isFalse();
            assertThat(info.batchNumber()).isNull();
        }

        @Test
        @DisplayName("un EAN-8 est range dans le champ EAN-13")
        void ean8() {
            DataMatrixInfo info = service.parse(EAN_8_VALIDE).orElseThrow();

            assertThat(info.ean13()).isEqualTo(EAN_8_VALIDE);
            assertThat(info.cip13()).isNull();
            assertThat(info.gtin()).isNull();
        }

        @Test
        @DisplayName("un EAN-13 hors prefixe francais ne remplit que l EAN")
        void ean13() {
            DataMatrixInfo info = service.parse(EAN_13_VALIDE).orElseThrow();

            assertThat(info.ean13()).isEqualTo(EAN_13_VALIDE);
            assertThat(info.cip13()).isNull();
        }

        @Test
        @DisplayName("un CIP-7 est range dans le champ CIP-13")
        void cip7() {
            DataMatrixInfo info = service.parse("1234567").orElseThrow();

            assertThat(info.cip13()).isEqualTo("1234567");
            assertThat(info.ean13()).isNull();
        }

        @Test
        @DisplayName("un CIP-13 remplit a la fois le CIP et l EAN")
        void cip13() {
            DataMatrixInfo info = service.parse("3400930000000").orElseThrow();

            assertThat(info.cip13()).isEqualTo("3400930000000");
            assertThat(info.ean13()).isEqualTo("3400930000000");
        }

        @Test
        @DisplayName("les espaces autour du code sont ignores")
        void espacesIgnores() {
            assertThat(service.parse("  " + EAN_13_VALIDE + "  ").orElseThrow().ean13()).isEqualTo(EAN_13_VALIDE);
        }

        @Test
        @DisplayName("l identifiant de symbologie n est pas range avec le code")
        void symbologieRetireeAvantRangement() {
            DataMatrixInfo info = service.parse("]C13400930000000").orElseThrow();

            assertThat(info.cip13()).isEqualTo("3400930000000");
            assertThat(info.ean13()).isEqualTo("3400930000000");
        }

        @Test
        @DisplayName("un EAN-13 debutant par un AI connu est lu comme un EAN-13")
        void ean13DebutantParUnAi() {
            DataMatrixInfo info = service.parse(EAN_13_PREFIXE_AI).orElseThrow();

            assertThat(info.ean13()).isEqualTo(EAN_13_PREFIXE_AI);
            assertThat(info.gtin()).isNull();
        }
    }

    @Nested
    @DisplayName("parse : GS1 DataMatrix")
    class ParseDataMatrix {

        @Test
        @DisplayName("lit le GTIN et en deduit l EAN-13 quand le chiffre indicateur est zero")
        void gtinVersEan13() {
            DataMatrixInfo info = service.parse("0103400930000000").orElseThrow();

            assertThat(info.gtin()).isEqualTo("03400930000000");
            assertThat(info.ean13()).isEqualTo("3400930000000");
        }

        @Test
        @DisplayName("un GTIN dont le chiffre indicateur n est pas zero ne donne pas d EAN-13")
        void gtinSansEan13() {
            DataMatrixInfo info = service.parse("0113400930000000").orElseThrow();

            assertThat(info.gtin()).isEqualTo("13400930000000");
            assertThat(info.ean13()).isNull();
        }

        @Test
        @DisplayName("lit un numero de lot en longueur variable termine par un separateur")
        void lotTermineParSeparateur() {
            DataMatrixInfo info = service.parse("10LOT2026A" + GS + "21SERIE1").orElseThrow();

            assertThat(info.batchNumber()).isEqualTo("LOT2026A");
            assertThat(info.serialNumber()).isEqualTo("SERIE1");
        }

        @Test
        @DisplayName("lit un numero de lot en fin de chaine")
        void lotEnFinDeChaine() {
            assertThat(service.parse("10LOT2026A").orElseThrow().batchNumber()).isEqualTo("LOT2026A");
        }

        @Test
        @DisplayName("lit la date de peremption au format AAMMJJ")
        void datePeremption() {
            assertThat(service.parse("17270630").orElseThrow().expiryDate()).isEqualTo(LocalDate.of(2027, 6, 30));
        }

        @Test
        @DisplayName("un jour a 00 designe le dernier jour du mois")
        void jourZeroEstFinDeMois() {
            assertThat(service.parse("17270200").orElseThrow().expiryDate()).isEqualTo(LocalDate.of(2027, 2, 28));
        }

        @Test
        @DisplayName("un jour a 00 sur une annee bissextile designe le 29 fevrier")
        void jourZeroAnneeBissextile() {
            assertThat(service.parse("17280200").orElseThrow().expiryDate()).isEqualTo(LocalDate.of(2028, 2, 29));
        }

        @Test
        @DisplayName("lit la date de fabrication")
        void dateDeFabrication() {
            assertThat(service.parse("11260115").orElseThrow().manufacturingDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        }

        @Test
        @DisplayName("lit le numero de serie")
        void numeroDeSerie() {
            assertThat(service.parse("21ABC123XYZ").orElseThrow().serialNumber()).isEqualTo("ABC123XYZ");
        }

        @Test
        @DisplayName("lit la quantite scannee")
        void quantiteScannee() {
            assertThat(service.parse("1012" + GS + "3725").orElseThrow().scannedQty()).isEqualTo(25);
        }

        @Test
        @DisplayName("une quantite nulle ou negative est ignoree")
        void quantiteNonPositive() {
            // la quantite par defaut du DTO (1) est conservee
            assertThat(service.parse("1012" + GS + "370").orElseThrow().scannedQty()).isEqualTo(1);
        }

        @Test
        @DisplayName("une quantite non numerique est ignoree sans faire echouer la lecture")
        void quantiteNonNumerique() {
            DataMatrixInfo info = service.parse("1012" + GS + "37ABC").orElseThrow();

            assertThat(info.scannedQty()).isEqualTo(1);
            assertThat(info.batchNumber()).isEqualTo("12");
        }

        @ParameterizedTest(name = "l AI {0} alimente le champ CIP-13")
        @CsvSource({ "710, DE12345", "711, 3400930000000", "712, ES12345", "713, BR12345", "714, PT12345" })
        void codesNationaux(String ai, String valeur) {
            assertThat(service.parse(ai + valeur).orElseThrow().cip13()).isEqualTo(valeur);
        }

        @Test
        @DisplayName("lit une trame complete GTIN + peremption + lot")
        void trameComplete() {
            DataMatrixInfo info = service.parse("010340093000000017270630" + "10LOT2026A" + GS + "21SN99").orElseThrow();

            assertThat(info.gtin()).isEqualTo("03400930000000");
            assertThat(info.ean13()).isEqualTo("3400930000000");
            assertThat(info.expiryDate()).isEqualTo(LocalDate.of(2027, 6, 30));
            assertThat(info.batchNumber()).isEqualTo("LOT2026A");
            assertThat(info.serialNumber()).isEqualTo("SN99");
        }

        @Test
        @DisplayName("retire l identifiant de symbologie ]d2")
        void symbologieD2() {
            assertThat(service.parse("]d20103400930000000").orElseThrow().gtin()).isEqualTo("03400930000000");
        }

        @Test
        @DisplayName("retire l identifiant de symbologie ]Q3 des QR Code GS1")
        void symbologieQ3() {
            assertThat(service.parse("]Q30103400930000000").orElseThrow().gtin()).isEqualTo("03400930000000");
        }

        @Test
        @DisplayName("retire le FNC1 imprimable emis par certains scanners")
        void fnc1Imprimable() {
            assertThat(service.parse("è0103400930000000").orElseThrow().gtin()).isEqualTo("03400930000000");
        }

        @Test
        @DisplayName("retire un separateur de groupe en tete, Code 128 GS1 sans identifiant")
        void separateurEnTete() {
            assertThat(service.parse(GS + "0103400930000000").orElseThrow().gtin()).isEqualTo("03400930000000");
        }

        @Test
        @DisplayName("accepte l echappement decimal ^029 du separateur")
        void separateurEchappementDecimal() {
            assertThat(service.parse("10LOT1^02921SN1").orElseThrow().serialNumber()).isEqualTo("SN1");
        }

        @Test
        @DisplayName("accepte la notation URL-encodee %1D du separateur")
        void separateurUrlEncode() {
            assertThat(service.parse("10LOT1%1D21SN1").orElseThrow().serialNumber()).isEqualTo("SN1");
        }

        @Test
        @DisplayName("accepte la notation textuelle <GS> du separateur")
        void separateurTextuelChevrons() {
            DataMatrixInfo info = service.parse("10LOT1<GS>21SN1").orElseThrow();

            assertThat(info.batchNumber()).isEqualTo("LOT1");
            assertThat(info.serialNumber()).isEqualTo("SN1");
        }

        @Test
        @DisplayName("accepte la notation textuelle {GS} du separateur")
        void separateurTextuelAccolades() {
            assertThat(service.parse("10LOT1{GS}21SN1").orElseThrow().serialNumber()).isEqualTo("SN1");
        }

        @Test
        @DisplayName("ignore les separateurs consecutifs")
        void separateursConsecutifs() {
            assertThat(service.parse("10LOT1" + GS + GS + "21SN1").orElseThrow().serialNumber()).isEqualTo("SN1");
        }

        @Test
        @DisplayName("saute un identifiant inconnu jusqu au separateur suivant")
        void identifiantInconnuSaute() {
            DataMatrixInfo info = service.parse("10LOT1" + GS + "99INCONNU" + GS + "21SN1").orElseThrow();

            assertThat(info.batchNumber()).isEqualTo("LOT1");
            assertThat(info.serialNumber()).isEqualTo("SN1");
        }

        @Test
        @DisplayName("un identifiant inconnu sans separateur interrompt la lecture")
        void identifiantInconnuSansSeparateur() {
            assertThat(service.parse("17ABCDEF" + "99INCONNU")).isEmpty();
        }

        @Test
        @DisplayName("un GTIN tronque ne donne aucun resultat")
        void gtinTronque() {
            assertThat(service.parse("0134009")).isEmpty();
        }

        @Test
        @DisplayName("un identifiant de lot sans donnee ne donne aucun resultat")
        void lotSansDonnee() {
            assertThat(service.parse("10" + GS)).isEmpty();
        }

        @Test
        @DisplayName("une date de peremption invalide fait echouer toute la lecture")
        void datePeremptionInvalide() {
            assertThat(service.parse("17271330")).isEmpty();
        }

        @Test
        @DisplayName("une date non numerique est ignoree sans donnee exploitable")
        void dateNonNumerique() {
            assertThat(service.parse("17ABCDEF")).isEmpty();
        }

        @Test
        @DisplayName("une date de fabrication illisible est ignoree sans perdre le lot")
        void dateDeFabricationIllisible() {
            DataMatrixInfo info = service.parse("11ABCDEF" + "10LOT1").orElseThrow();

            assertThat(info.manufacturingDate()).isNull();
            assertThat(info.batchNumber()).isEqualTo("LOT1");
        }

        @Test
        @DisplayName("une date de fabrication invalide apres une donnee valide ne l efface pas")
        void dateDeFabricationInvalideApresDonneeValide() {
            DataMatrixInfo info = service.parse("10LOT1" + GS + "11ABCDEF").orElseThrow();

            assertThat(info.batchNumber()).isEqualTo("LOT1");
            assertThat(info.manufacturingDate()).isNull();
        }

        @Test
        @DisplayName("une date de peremption invalide apres une donnee valide ne l efface pas")
        void datePeremptionInvalideApresDonneeValide() {
            DataMatrixInfo info = service.parse("10LOT1" + GS + "17ABCDEF").orElseThrow();

            assertThat(info.batchNumber()).isEqualTo("LOT1");
            assertThat(info.expiryDate()).isNull();
        }

        @Test
        @DisplayName("un reliquat trop court pour porter un identifiant interrompt la lecture")
        void reliquatTropCourt() {
            DataMatrixInfo info = service.parse("10LOT1" + GS + "9").orElseThrow();

            assertThat(info.batchNumber()).isEqualTo("LOT1");
        }

        @Test
        @DisplayName("une date suivie d un lot conserve le lot malgre la date invalide")
        void dateNonNumeriqueSuivieDunLot() {
            Optional<DataMatrixInfo> info = service.parse("17ABCDEF" + "10LOT1");

            assertThat(info).isPresent();
            assertThat(info.orElseThrow().batchNumber()).isEqualTo("LOT1");
            assertThat(info.orElseThrow().expiryDate()).isNull();
        }
    }

    @Nested
    @DisplayName("parse : AI 240, etiquette interne de l officine")
    class ParseIdentifiantAdditionnel {

        @Test
        @DisplayName("le code du catalogue est remonte en code brut, non interprete")
        void codeDuCatalogueEnCodeBrut() {
            DataMatrixInfo info = service.parse("240A-7788" + GS + "10LOT9").orElseThrow();

            assertThat(info.rawCode()).isEqualTo("A-7788");
            assertThat(info.getProductCode()).isEqualTo("A-7788");
            assertThat(info.cip13()).isNull();
            assertThat(info.ean13()).isNull();
            assertThat(info.gtin()).isNull();
        }

        @Test
        @DisplayName("lot et peremption y sont certains : ils viennent d un identifiant GS1")
        void lotEtPeremptionCertains() {
            DataMatrixInfo info = service.parse("240A-7788" + GS + "10LOT9" + GS + "17261231").orElseThrow();

            assertThat(info.batchNumber()).isEqualTo("LOT9");
            assertThat(info.expiryDate()).isEqualTo(LocalDate.of(2026, 12, 31));
            assertThat(info.proposition()).isFalse();
            assertThat(info.hasBatchInfoCertaine()).isTrue();
        }

        @Test
        @DisplayName("un code purement numerique passe aussi par l AI 240")
        void codeNumerique() {
            DataMatrixInfo info = service.parse("2401234567" + GS + "17270630").orElseThrow();

            assertThat(info.rawCode()).isEqualTo("1234567");
            assertThat(info.expiryDate()).isEqualTo(LocalDate.of(2027, 6, 30));
        }

        @Test
        @DisplayName("un AI 240 vide est ignore sans faire perdre les identifiants suivants")
        void ai240Vide() {
            DataMatrixInfo info = service.parse("240" + GS + "10LOT9").orElseThrow();

            assertThat(info.rawCode()).isNull();
            assertThat(info.batchNumber()).isEqualTo("LOT9");
        }
    }
}

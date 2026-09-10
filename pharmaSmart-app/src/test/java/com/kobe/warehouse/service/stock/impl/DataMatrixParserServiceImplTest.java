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
        @ValueSource(strings = { "   ", "abcdef", "12345" })
        void aucunResultat(String code) {
            assertThat(service.parse(code)).isEmpty();
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
}

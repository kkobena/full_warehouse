package com.kobe.warehouse.service.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.kobe.warehouse.service.dto.DataMatrixInfo;
import com.kobe.warehouse.service.stock.impl.DataMatrixParserServiceImpl;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Gs1DataMatrixEncoder")
class Gs1DataMatrixEncoderTest {

    private static final LocalDate PEREMPTION = LocalDate.of(2026, 12, 31);

    @Nested
    @DisplayName("construireEntree")
    class ConstruireEntree {

        @Test
        @DisplayName("porte le code du catalogue en AI 240, le lot en AI 10 et la peremption en AI 17")
        void trameComplete() {
            String entree = Gs1DataMatrixEncoder.construireEntree("A-7788", "LOT9", PEREMPTION);

            assertThat(entree).isEqualTo("dm:240A-7788{GS}10LOT9{GS}17261231");
        }

        @Test
        @DisplayName("sans lot, ne porte que le code et la peremption")
        void sansLot() {
            assertThat(Gs1DataMatrixEncoder.construireEntree("A-7788", null, PEREMPTION)).isEqualTo("dm:240A-7788{GS}17261231");
        }

        @Test
        @DisplayName("sans peremption, ne porte que le code et le lot")
        void sansPeremption() {
            assertThat(Gs1DataMatrixEncoder.construireEntree("A-7788", "LOT9", null)).isEqualTo("dm:240A-7788{GS}10LOT9");
        }

        @Test
        @DisplayName("sans lot ni peremption, aucune trame : le Code 39 du code suffit")
        void sansRienAPorter() {
            assertThat(Gs1DataMatrixEncoder.construireEntree("A-7788", null, null)).isNull();
            assertThat(Gs1DataMatrixEncoder.construireEntree("A-7788", "  ", null)).isNull();
        }

        @Test
        @DisplayName("sans code du catalogue, aucune trame")
        void sansCode() {
            assertThat(Gs1DataMatrixEncoder.construireEntree(null, "LOT9", PEREMPTION)).isNull();
            assertThat(Gs1DataMatrixEncoder.construireEntree("", "LOT9", PEREMPTION)).isNull();
        }

        @Test
        @DisplayName("les espaces autour des valeurs sont retires")
        void valeursRognees() {
            assertThat(Gs1DataMatrixEncoder.construireEntree(" A-7788 ", " LOT9 ", PEREMPTION))
                .isEqualTo("dm:240A-7788{GS}10LOT9{GS}17261231");
        }
    }

    @Nested
    @DisplayName("encoder")
    class Encoder {

        @Test
        @DisplayName("produit un symbole carre")
        void symboleCarre() {
            BufferedImage image = Gs1DataMatrixEncoder.encoder(Gs1DataMatrixEncoder.construireEntree("A-7788", "LOT9", PEREMPTION));

            assertThat(image.getWidth()).isPositive();
            assertThat(image.getWidth()).isEqualTo(image.getHeight());
        }

        @Test
        @DisplayName("l etiquette imprimee est relue par l application : code, lot et peremption")
        void relectureParLApplication() {
            String entree = Gs1DataMatrixEncoder.construireEntree("A-7788", "LOT9", PEREMPTION);

            DataMatrixInfo info = new DataMatrixParserServiceImpl().parse(decoder(Gs1DataMatrixEncoder.encoder(entree))).orElseThrow();

            assertThat(info.getProductCode()).isEqualTo("A-7788");
            assertThat(info.batchNumber()).isEqualTo("LOT9");
            assertThat(info.expiryDate()).isEqualTo(PEREMPTION);
            // Valeurs lues dans un AI GS1, donc sures : la reception peut creer le lot.
            assertThat(info.hasBatchInfoCertaine()).isTrue();
        }

        /**
         * Valeurs du jeu de démonstration (scripts/demo-data) : code_cip est un varchar(10) — ici
         * le CIP-7 de METFORMINE VIATRIS 1000 mg — et le numéro de lot suit le motif LAAMMnnnn.
         * C'est ce couple que l'officine imprimera, et qu'elle rescannera.
         */
        @Test
        @DisplayName("l etiquette du jeu de demonstration se relit : CIP-7 et numero de lot")
        void relectureDuJeuDeDemonstration() {
            String entree = Gs1DataMatrixEncoder.construireEntree("3794609", "L26050002", LocalDate.of(2027, 5, 30));

            DataMatrixInfo info = new DataMatrixParserServiceImpl().parse(decoder(Gs1DataMatrixEncoder.encoder(entree))).orElseThrow();

            assertThat(info.getProductCode()).isEqualTo("3794609");
            assertThat(info.batchNumber()).isEqualTo("L26050002");
            assertThat(info.expiryDate()).isEqualTo(LocalDate.of(2027, 5, 30));
            assertThat(info.hasBatchInfoCertaine()).isTrue();
        }

        @Test
        @DisplayName("un code purement numerique se relit aussi")
        void relectureDunCodeNumerique() {
            String entree = Gs1DataMatrixEncoder.construireEntree("1234567", "L1", PEREMPTION);

            DataMatrixInfo info = new DataMatrixParserServiceImpl().parse(decoder(Gs1DataMatrixEncoder.encoder(entree))).orElseThrow();

            assertThat(info.getProductCode()).isEqualTo("1234567");
            assertThat(info.batchNumber()).isEqualTo("L1");
        }

        /**
         * Un seul FNC1 de tête. Deux — ce que donnait un 0x1D explicite dans le contenu, en plus
         * de l'en-tête que pose {@code GS1_FORMAT} — passent chez nous, parce que
         * {@code normalizeCode} les retire en boucle, mais un scanner tiers rendrait le second
         * comme une donnée et un validateur GS1 refuserait le symbole.
         */
        @Test
        @DisplayName("le symbole ne porte qu un seul FNC1 de tete")
        void unSeulFnc1DeTete() {
            String trame = decoder(
                Gs1DataMatrixEncoder.encoder(Gs1DataMatrixEncoder.construireEntree("A-7788", "LOT9", PEREMPTION))
            );

            assertThat(trame).startsWith("240A-7788");
            assertThat(trame.chars().takeWhile(c -> c == 0x1D).count()).isEqualTo(1);
        }

        /** Relit le symbole comme le ferait un scanner, séparateurs de groupe compris. */
        private String decoder(BufferedImage image) {
            try {
                LuminanceSource source = new BufferedImageLuminanceSource(image);
                Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
                hints.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
                Result result = new DataMatrixReader().decode(new BinaryBitmap(new HybridBinarizer(source)), hints);
                return result.getText();
            } catch (Exception e) {
                throw new AssertionError("symbole illisible", e);
            }
        }
    }
}

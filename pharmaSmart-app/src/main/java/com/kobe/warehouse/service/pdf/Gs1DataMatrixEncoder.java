package com.kobe.warehouse.service.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.datamatrix.DataMatrixWriter;
import com.google.zxing.datamatrix.encoder.SymbolShapeHint;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Trame et symbole du GS1 DataMatrix des étiquettes internes : quand la boîte porte un DataMatrix
 * étranger, l'officine réimprime le code de son catalogue avec le lot et la péremption.
 */
public final class Gs1DataMatrixEncoder {

    /** Préfixe d'une entrée de {@code barcodesData} à rendre en DataMatrix plutôt qu'en Code 39. */
    public static final String PREFIXE_DATA_MATRIX = "dm:";

    /** Séparateur de groupe en notation textuelle : un 0x1D nu serait illégal dans le gabarit XML. */
    private static final String GS_TEXTUEL = "{GS}";
    private static final String GS = "\u001D";

    /** Identification additionnelle du fabricant : le seul AI qui accepte un code non numérique. */
    private static final String AI_IDENTIFIANT_ADDITIONNEL = "240";
    private static final String AI_LOT = "10";
    private static final String AI_PEREMPTION = "17";

    private static final DateTimeFormatter PEREMPTION_GS1 = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int TAILLE_SYMBOLE = 120;
    private static final String JEU_DE_CARACTERES = "ISO-8859-1";

    private Gs1DataMatrixEncoder() {}

    /**
     * Entrée d'étiquette : préfixe de branchement, puis AI 240 (code du catalogue), AI 10 (lot)
     * et AI 17 (péremption).
     *
     * <p>Aucun FNC1 n'est écrit en tête : {@code GS1_FORMAT} fait déjà émettre à ZXing l'en-tête
     * FNC1 du symbole, et un 0x1D de plus dans le contenu en produirait un second — relu comme
     * une donnée par un scanner tiers, et refusé par un validateur GS1.
     *
     * <p>AI 240 et non AI 01 : un GTIN-14 n'accepte que quatorze chiffres alors que
     * {@code code_cip} est un code interne de dix caractères, qu'un bourrage de zéros rendrait
     * introuvable au scan.
     *
     * @return {@code null} s'il n'y a ni lot ni péremption à porter — le Code 39 du code suffit
     */
    public static String construireEntree(String code, String numLot, LocalDate peremption) {
        if (!StringUtils.hasText(code) || (!StringUtils.hasText(numLot) && peremption == null)) {
            return null;
        }
        StringBuilder contenu = new StringBuilder(PREFIXE_DATA_MATRIX)
            .append(AI_IDENTIFIANT_ADDITIONNEL)
            .append(code.trim());
        if (StringUtils.hasText(numLot)) {
            contenu.append(GS_TEXTUEL).append(AI_LOT).append(numLot.trim());
        }
        if (peremption != null) {
            contenu.append(GS_TEXTUEL).append(AI_PEREMPTION).append(peremption.format(PEREMPTION_GS1));
        }
        return contenu.toString();
    }

    /** Symbole d'une entrée produite par {@link #construireEntree}, préfixe compris. */
    public static BufferedImage encoder(String entree) {
        String contenu = entree.substring(PREFIXE_DATA_MATRIX.length()).replace(GS_TEXTUEL, GS);
        BitMatrix matrice = new DataMatrixWriter()
            .encode(contenu, BarcodeFormat.DATA_MATRIX, TAILLE_SYMBOLE, TAILLE_SYMBOLE, construireHints());
        return MatrixToImageWriter.toBufferedImage(matrice);
    }

    /**
     * {@code DATA_MATRIX_COMPACT} commande l'encodeur minimal, seul à lire {@code GS1_FORMAT} et
     * {@code CHARACTER_SET} : sans lui ZXing ignore les deux et n'émet aucun FNC1. Avec lui,
     * l'en-tête FNC1 du symbole est posé par l'encodeur — le contenu n'a pas à le porter.
     */
    private static Map<EncodeHintType, Object> construireHints() {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.DATA_MATRIX_SHAPE, SymbolShapeHint.FORCE_SQUARE);
        hints.put(EncodeHintType.DATA_MATRIX_COMPACT, true);
        hints.put(EncodeHintType.GS1_FORMAT, true);
        hints.put(EncodeHintType.CHARACTER_SET, JEU_DE_CARACTERES);
        return hints;
    }
}

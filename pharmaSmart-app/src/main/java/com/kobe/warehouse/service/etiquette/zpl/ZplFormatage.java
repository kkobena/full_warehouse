package com.kobe.warehouse.service.etiquette.zpl;

import com.kobe.warehouse.service.pdf.Gs1DataMatrixEncoder;

/**
 * Conversions et échappements du langage ZPL.
 *
 * <p>Hors du paquetage {@code impl}, qui ne porte que des implémentations de service : ce sont des
 * calculs purs, sans état ni dépendance.
 */
public final class ZplFormatage {

    /** Séparateur de groupe tel que la trame GS1 l'écrit en clair. */
    private static final String GS_TEXTUEL = "{GS}";

    /**
     * Caractère d'échappement déclaré au symbole DataMatrix ({@code ^BX}), qui fait du
     * {@code _1} qui suit un FNC1 — le seul moyen d'écrire un séparateur GS1 en ZPL.
     */
    public static final String ECHAPPEMENT = "_";

    /** FNC1 : en tête, il déclare un GS1 DataMatrix ; entre deux AI, il les sépare. */
    public static final String FNC1 = ECHAPPEMENT + "1";

    /** AI 240, le seul AI qui accepte le code non numérique du catalogue (cf. l'encodeur GS1). */
    private static final String AI_IDENTIFIANT_ADDITIONNEL = "240";

    /**
     * Caractères que ZPL lit comme des commandes : {@code ^} et {@code ~} ouvrent une instruction,
     * {@code _} est notre échappement. Les laisser passer dans un libellé couperait l'étiquette en
     * deux.
     */
    private static final String CARACTERES_DE_COMMANDE = "^~_";

    private static final char REMPLACANT = '-';

    private ZplFormatage() {}

    /** Convertit une cote en points de la tête d'impression, la seule unité que ZPL comprenne. */
    public static int convertirEnDots(double millimetres, int dpi) {
        return (int) Math.round(millimetres * dpi / 25.4);
    }

    /** Neutralise les caractères de commande d'un texte destiné à un champ {@code ^FD}. */
    public static String assainir(String texte) {
        if (texte == null || texte.isEmpty()) {
            return "";
        }
        StringBuilder propre = new StringBuilder(texte.length());
        for (char caractere : texte.toCharArray()) {
            propre.append(CARACTERES_DE_COMMANDE.indexOf(caractere) >= 0 ? REMPLACANT : caractere);
        }
        return propre.toString();
    }

    /**
     * Donnée d'un GS1 DataMatrix en ZPL : FNC1 en tête, puis les AI séparés par un FNC1.
     *
     * <p>Là où ZXing pose lui-même l'en-tête FNC1 du symbole, le firmware de l'imprimante attend
     * de le lire dans la donnée. La trame est donc la même, écrite autrement.
     *
     * @param entree trame produite par {@link Gs1DataMatrixEncoder}, préfixe compris
     */
    public static String convertirTrameEnDonnee(String entree) {
        // Assaini AVANT d'insérer les FNC1 : l'échappement porte justement le caractère que
        // l'assainissement remplace.
        String contenu = assainir(entree.substring(Gs1DataMatrixEncoder.PREFIXE_DATA_MATRIX.length()));
        return FNC1 + contenu.replace(GS_TEXTUEL, FNC1);
    }

    /** Donnée d'un DataMatrix ne portant que le code du catalogue, sans lot ni péremption. */
    public static String construireDonneeDuCode(String code) {
        return FNC1 + AI_IDENTIFIANT_ADDITIONNEL + assainir(code);
    }
}

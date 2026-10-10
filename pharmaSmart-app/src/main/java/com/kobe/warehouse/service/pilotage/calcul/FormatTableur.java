package com.kobe.warehouse.service.pilotage.calcul;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Nombre pour un tableur français : deux décimales au plus, virgule décimale, sans séparateur de milliers ; vide si absent. */
public final class FormatTableur {

    private FormatTableur() {}

    public static String formater(Double valeur) {
        if (valeur == null || valeur.isNaN() || valeur.isInfinite()) {
            return "";
        }
        return BigDecimal.valueOf(valeur).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',');
    }
}

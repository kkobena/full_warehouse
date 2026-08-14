package com.kobe.warehouse.license;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalisation des libellés comparés entre la licence et l'officine (nom du magasin, n° contribuable).
 *
 * <p>Sans elle, « Pharmacie de la Paix » et « PHARMACIE DE LA PAIX » désigneraient deux officines
 * différentes et la licence serait rejetée à tort — le faux positif le plus probable du dispositif
 * (cf. PLAN-GESTION-LICENCE §3.4 et §9).
 */
public final class LicenseTextNormalizer {

    private LicenseTextNormalizer() {}

    /**
     * @return la valeur en majuscules, sans accents, espaces réduits et rognés ; {@code null} si
     *     l'entrée est nulle ou vide une fois rognée.
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String stripped = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String collapsed = stripped.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        return collapsed.isEmpty() ? null : collapsed;
    }

    /** {@code true} si les deux valeurs sont non vides et équivalentes après normalisation. */
    public static boolean matches(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        return a != null && a.equals(b);
    }

    /**
     * Comparaison de renfort : ne tranche que si la valeur est renseignée <em>des deux côtés</em>.
     * Utilisée pour le n° contribuable, nullable dans {@code Magasin} — jamais un pivot.
     *
     * @return {@code false} uniquement si les deux valeurs sont renseignées et divergentes.
     */
    public static boolean matchesIfBothPresent(String left, String right) {
        String a = normalize(left);
        String b = normalize(right);
        return a == null || b == null || a.equals(b);
    }
}

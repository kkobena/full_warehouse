package com.kobe.warehouse.domain.enumeration;

/**
 * Critère de sélection des produits pour un inventaire tournant.
 * <ul>
 *   <li>{@link #RAYON} — rotation par rayon du storage</li>
 *   <li>{@link #FAMILLE} — rotation par famille de produits</li>
 *   <li>{@link #CLASSIFICATION_ABC} — rotation par classe Pareto A → B → C</li>
 * </ul>
 */
public enum CritereTournant {
    RAYON,
    FAMILLE,
    CLASSIFICATION_ABC,
}

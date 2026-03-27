package com.kobe.warehouse.domain.enumeration;

/**
 * Type de suggestion.
 */
public enum TypeSuggession {
    /**
     * @deprecated Plus créé depuis v12. Conservé pour compatibilité des données existantes.
     *             Le batch {@code creerSuggestionBatch()} utilise désormais {@link #SEMOIS}.
     */
    @Deprecated(since = "2026-03", forRemoval = false)
    AUTO,
    MANUELLE,
    /**
     * Suggestions créées par le batch {@code SemoisBatchJobService.creerSuggestionBatch()}.
     * Remplace le workflow {@code suggestionAuto} (décision architecture v12).
     */
    SEMOIS,
}

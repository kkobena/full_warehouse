package com.kobe.warehouse.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Identité d'une ligne de {@link SemoisSuggestionView} : un produit <b>dans un magasin</b>.
 *
 * <p>Le seul produit ne suffit plus depuis V2.1.55. S'en tenir à lui ferait confondre par
 * Hibernate la ligne de l'officine et celle de la parapharmacie : la seconde chargée rendrait
 * silencieusement la première, déjà dans le contexte de persistance.
 */
public class SemoisSuggestionViewId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer produitId;
    private Integer magasinId;

    public SemoisSuggestionViewId() {}

    public SemoisSuggestionViewId(Integer produitId, Integer magasinId) {
        this.produitId = produitId;
        this.magasinId = magasinId;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public Integer getMagasinId() {
        return magasinId;
    }

    @Override
    public boolean equals(Object autre) {
        if (this == autre) {
            return true;
        }
        if (!(autre instanceof SemoisSuggestionViewId id)) {
            return false;
        }
        return Objects.equals(produitId, id.produitId) && Objects.equals(magasinId, id.magasinId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(produitId, magasinId);
    }

    @Override
    public String toString() {
        return "produit " + produitId + " / magasin " + magasinId;
    }
}

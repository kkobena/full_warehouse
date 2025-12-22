package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.Instant;

/**
 * Entité en lecture seule mappée sur la vue matérialisée mv_semois_suggestion.
 * Cette vue contient tous les calculs SEMOIS précalculés et est rafraîchie quotidiennement.
 *
 * IMPORTANT: Cette entité est IMMUTABLE (lecture seule).
 * Pour modifier les données, passer par SemoisConfiguration.
 */
@Entity
@Table(name = "mv_semois_suggestion")
@Immutable // Hibernate: empêche INSERT/UPDATE/DELETE
public class SemoisSuggestionView implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "produit_id")
    private Integer produitId;

    @Column(name = "libelle", nullable = false)
    private String libelle;

    @Column(name = "code_cip")
    private String codeCip;

    @Enumerated(EnumType.STRING)
    @Column(name = "classe_criticite", length = 3)
    private ClasseCriticite classeCriticite;

    @Column(name = "coefficient_securite")
    private Double coefficientSecurite;

    @Column(name = "delai_livraison_jours")
    private Integer delaiLivraisonJours;

    @Column(name = "vmm")
    private Integer vmm;

    @Column(name = "marge_securite")
    private Integer margeSecurite;

    @Column(name = "stock_objectif")
    private Integer stockObjectif;

    @Column(name = "stock_actuel")
    private Integer stockActuel;

    @Column(name = "quantite_a_commander")
    private Integer quantiteACommander;

    @Column(name = "date_dernier_calcul")
    private Instant dateDernierCalcul;

    @Column(name = "vue_refresh_date")
    private Instant vueRefreshDate;

    // ==================== Getters (pas de setters: lecture seule) ====================

    public Integer getProduitId() {
        return produitId;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getCodeCip() {
        return codeCip;
    }

    public ClasseCriticite getClasseCriticite() {
        return classeCriticite;
    }

    public Double getCoefficientSecurite() {
        return coefficientSecurite;
    }

    public Integer getDelaiLivraisonJours() {
        return delaiLivraisonJours;
    }

    public Integer getVmm() {
        return vmm;
    }

    public Integer getMargeSecurite() {
        return margeSecurite;
    }

    public Integer getStockObjectif() {
        return stockObjectif;
    }

    public Integer getStockActuel() {
        return stockActuel;
    }

    public Integer getQuantiteACommander() {
        return quantiteACommander;
    }

    public Instant getDateDernierCalcul() {
        return dateDernierCalcul;
    }

    public Instant getVueRefreshDate() {
        return vueRefreshDate;
    }

    // ==================== toString, equals, hashCode ====================

    @Override
    public String toString() {
        return "SemoisSuggestionView{" +
            "produitId=" + produitId +
            ", libelle='" + libelle + '\'' +
            ", codeCip='" + codeCip + '\'' +
            ", classeCriticite=" + classeCriticite +
            ", vmm=" + vmm +
            ", stockObjectif=" + stockObjectif +
            ", stockActuel=" + stockActuel +
            ", quantiteACommander=" + quantiteACommander +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemoisSuggestionView)) return false;
        SemoisSuggestionView that = (SemoisSuggestionView) o;
        return produitId != null && produitId.equals(that.produitId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.io.Serializable;
import java.time.Instant;

/**
 * Entité en lecture seule mappée sur la vue ordinaire v_semois_suggestion.
 * stock_actuel et quantite_a_commander sont calculés en temps réel depuis stock_produit.
 * vmm, marge_securite, stock_objectif viennent de semois_configuration (pré-calculés par le batch).
 * La classe de criticité vient de produit.classe_criticite (auto-classifié par ClassificationCriticiteService).
 * Le délai de livraison est résolu en cascade : semois_configuration → fournisseur → fournisseur parent → 7j.
 * IMPORTANT: Cette entité est IMMUTABLE (lecture seule).
 * Pour modifier les données, passer par SemoisConfiguration ou SemoisClasseConfig.
 */
@Entity
@Table(name = "v_semois_suggestion")
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

    @Column(name = "fournisseur_id")
    private Integer fournisseurId;

    @Column(name = "fournisseur_libelle")
    private String fournisseurLibelle;

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

    // bigint dans PostgreSQL (résultat d'un SUM)
    @Column(name = "stock_actuel")
    private Long stockActuel;

    // bigint dans PostgreSQL (résultat de GREATEST(...))
    @Column(name = "quantite_a_commander")
    private Long quantiteACommander;

    @Column(name = "date_dernier_calcul")
    private Instant dateDernierCalcul;

    // vue_refresh_date n'existe plus dans v_semois_suggestion (vue ordinaire)

    // ==================== Getters (pas de setters: lecture seule) ====================

    public Integer getFournisseurId() {
        return fournisseurId;
    }

    public String getFournisseurLibelle() {
        return fournisseurLibelle;
    }

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

    public Long getStockActuel() {
        return stockActuel;
    }

    public Long getQuantiteACommander() {
        return quantiteACommander;
    }

    public Instant getDateDernierCalcul() {
        return dateDernierCalcul;
    }

    // ==================== toString, equals, hashCode ====================

    @Override
    public String toString() {
        return "SemoisSuggestionView{" +
            "produitId=" + produitId +
            ", libelle='" + libelle + '\'' +
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

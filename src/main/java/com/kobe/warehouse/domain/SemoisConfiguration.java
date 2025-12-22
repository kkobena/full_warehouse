package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Configuration SEMOIS (Stock Économique Mensuel d'Objectif Interne de Sécurité) par produit.
 * Définit les paramètres utilisés pour calculer le stock objectif et les quantités de réapprovisionnement.
 * <p>
 * SEMOIS = VMM (Ventes Mensuelles Moyennes) + Marge de Sécurité
 * Marge de Sécurité = VMM × (délai_livraison × coefficient_sécurité / 30)
 */
@Entity
@Table(
    name = "semois_configuration",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_semois_config_produit",
        columnNames = {"produit_id"}
    ),
    indexes = {
        @Index(name = "idx_semois_config_produit", columnList = "produit_id"),
        @Index(name = "idx_semois_config_classe", columnList = "classe_criticite"),
        @Index(name = "idx_semois_config_updated", columnList = "updated_at")
    }
)
public class SemoisConfiguration implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    @NotNull
    private Produit produit;

    /**
     * Classe de criticité du produit (A+, A, B, C, D)
     * Détermine le coefficient de sécurité par défaut
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "classe_criticite", length = 3, nullable = false)
    @NotNull
    private ClasseCriticite classeCriticite = ClasseCriticite.B;

    /**
     * Coefficient de sécurité (0.1 à 2.0)
     * Multiplie la marge de sécurité
     * Valeur par défaut selon classe: A+=1.5, A=1.0, B=0.7, C=0.4, D=0.2
     */
    @NotNull
    @DecimalMin("0.1")
    @DecimalMax("2.0")
    @Column(name = "coefficient_securite", precision = 3, scale = 2, nullable = false)
    private BigDecimal coefficientSecurite = BigDecimal.ONE;

    /**
     * Nombre de mois d'historique à utiliser pour calculer VMM (3 à 12)
     * Recommandé: 6 mois
     */
    @NotNull
    @Min(3)
    @Column(name = "nb_mois_historique", nullable = false)
    private Integer nbMoisHistorique = 6;

    /**
     * Délai de livraison du fournisseur en jours (1 à 90)
     * Utilisé pour calculer la marge de sécurité
     */
    @NotNull
    @Min(1)
    @Column(name = "delai_livraison_jours", nullable = false)
    private Integer delaiLivraisonJours = 7;

    /**
     * Stock objectif calculé (cache du dernier calcul)
     * Stock Objectif = VMM + Marge de Sécurité
     */
    @Column(name = "stock_objectif_calcule")
    private Integer stockObjectifCalcule;

    /**
     * VMM (Ventes Mensuelles Moyennes) calculé (cache)
     * Moyenne pondérée des N derniers mois
     */
    @Column(name = "vmm_calcule")
    private Integer vmmCalcule;

    /**
     * Date du dernier calcul SEMOIS pour ce produit
     */
    @Column(name = "date_dernier_calcul")
    private LocalDateTime dateDernierCalcul;

    /**
     * Facteur d'ajustement saisonnier (0.1 à 3.0)
     * 1.0 = pas d'ajustement
     * > 1.0 = hausse saisonnière
     * < 1.0 = baisse saisonnière
     */
    @DecimalMin("0.1")
    @DecimalMax("3.0")
    @Column(name = "facteur_saisonnier_actuel", precision = 3, scale = 2)
    private BigDecimal facteurSaisonnierActuel = BigDecimal.ONE;

    /**
     * Limiter le stock objectif pour produits périssables
     * Si TRUE: Stock Objectif = MIN(Stock Objectif calculé, VMM × 3)
     */
    @Column(name = "limite_peremption")
    private Boolean limitePeremption = false;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    // === Getters & Setters ===

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public ClasseCriticite getClasseCriticite() {
        return classeCriticite;
    }

    public void setClasseCriticite(ClasseCriticite classeCriticite) {
        this.classeCriticite = classeCriticite;
        // Mettre à jour coefficient selon la classe si pas personnalisé
        if (classeCriticite != null && this.coefficientSecurite.compareTo(BigDecimal.ONE) == 0) {
            this.coefficientSecurite = BigDecimal.valueOf(classeCriticite.getCoefficientDefaut());
        }
    }

    public BigDecimal getCoefficientSecurite() {
        return coefficientSecurite;
    }

    public void setCoefficientSecurite(BigDecimal coefficientSecurite) {
        this.coefficientSecurite = coefficientSecurite;
    }

    public Integer getNbMoisHistorique() {
        return nbMoisHistorique;
    }

    public void setNbMoisHistorique(Integer nbMoisHistorique) {
        this.nbMoisHistorique = nbMoisHistorique;
    }

    public Integer getDelaiLivraisonJours() {
        return delaiLivraisonJours;
    }

    public void setDelaiLivraisonJours(Integer delaiLivraisonJours) {
        this.delaiLivraisonJours = delaiLivraisonJours;
    }

    public Integer getStockObjectifCalcule() {
        return stockObjectifCalcule;
    }

    public void setStockObjectifCalcule(Integer stockObjectifCalcule) {
        this.stockObjectifCalcule = stockObjectifCalcule;
    }

    public Integer getVmmCalcule() {
        return vmmCalcule;
    }

    public void setVmmCalcule(Integer vmmCalcule) {
        this.vmmCalcule = vmmCalcule;
    }

    public LocalDateTime getDateDernierCalcul() {
        return dateDernierCalcul;
    }

    public void setDateDernierCalcul(LocalDateTime dateDernierCalcul) {
        this.dateDernierCalcul = dateDernierCalcul;
    }

    public BigDecimal getFacteurSaisonnierActuel() {
        return facteurSaisonnierActuel;
    }

    public void setFacteurSaisonnierActuel(BigDecimal facteurSaisonnierActuel) {
        this.facteurSaisonnierActuel = facteurSaisonnierActuel;
    }

    public Boolean getLimitePeremption() {
        return limitePeremption;
    }

    public void setLimitePeremption(Boolean limitePeremption) {
        this.limitePeremption = limitePeremption;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // === Méthodes utilitaires ===

    /**
     * Initialise la configuration avec les valeurs par défaut selon la classe
     *
     * @param produit Le produit à configurer
     * @param classe La classe de criticité
     * @return La configuration initialisée
     */
    public static SemoisConfiguration createDefault(Produit produit, ClasseCriticite classe) {
        SemoisConfiguration config = new SemoisConfiguration();
        config.setProduit(produit);
        config.setClasseCriticite(classe);
        config.setCoefficientSecurite(BigDecimal.valueOf(classe.getCoefficientDefaut()));
        config.setNbMoisHistorique(6);
        config.setDelaiLivraisonJours(7);
        config.setFacteurSaisonnierActuel(BigDecimal.ONE);
        config.setLimitePeremption(false);
        return config;
    }

    /**
     * Marque cette configuration comme recalculée
     *
     * @param vmm VMM calculé
     * @param stockObjectif Stock objectif calculé
     */
    public void updateCalculs(Integer vmm, Integer stockObjectif) {
        this.vmmCalcule = vmm;
        this.stockObjectifCalcule = stockObjectif;
        this.dateDernierCalcul = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (coefficientSecurite == null && classeCriticite != null) {
            coefficientSecurite = BigDecimal.valueOf(classeCriticite.getCoefficientDefaut());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemoisConfiguration that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SemoisConfiguration{" +
            "id=" + id +
            ", classeCriticite=" + classeCriticite +
            ", coefficientSecurite=" + coefficientSecurite +
            ", nbMoisHistorique=" + nbMoisHistorique +
            ", delaiLivraisonJours=" + delaiLivraisonJours +
            ", vmmCalcule=" + vmmCalcule +
            ", stockObjectifCalcule=" + stockObjectifCalcule +
            '}';
    }
}

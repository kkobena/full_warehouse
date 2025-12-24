package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Configuration des paramètres de classification dynamique de criticité.
 * Table singleton contenant les poids et seuils utilisés pour la classification automatique.
 */
@Entity
@Table(name = "classification_config")
public class ClassificationConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // === Poids des critères (doivent sommer à 1.0) ===

    /**
     * Poids du chiffre d'affaires dans le calcul du score (0.0 à 1.0)
     */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "poids_ca", precision = 3, scale = 2, nullable = false)
    private BigDecimal poidsCa = new BigDecimal("0.50");

    /**
     * Poids de la rotation de stock dans le calcul du score (0.0 à 1.0)
     */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "poids_rotation", precision = 3, scale = 2, nullable = false)
    private BigDecimal poidsRotation = new BigDecimal("0.30");

    /**
     * Poids de la fréquence de vente dans le calcul du score (0.0 à 1.0)
     */
    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    @Column(name = "poids_frequence", precision = 3, scale = 2, nullable = false)
    private BigDecimal poidsFrequence = new BigDecimal("0.20");

    // === Seuils de score pour chaque classe ===

    /**
     * Seuil minimum de score pour la classe A+ (0-100)
     */
    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "seuil_a_plus", nullable = false)
    private Integer seuilAPlus = 90;

    /**
     * Seuil minimum de score pour la classe A (0-100)
     */
    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "seuil_a", nullable = false)
    private Integer seuilA = 80;

    /**
     * Seuil minimum de score pour la classe B (0-100)
     */
    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "seuil_b", nullable = false)
    private Integer seuilB = 60;

    /**
     * Seuil minimum de score pour la classe C (0-100)
     * Tout score inférieur donne la classe D
     */
    @NotNull
    @Min(0)
    @Max(100)
    @Column(name = "seuil_c", nullable = false)
    private Integer seuilC = 40;

    // === Seuils de rotation annuelle (pour affichage) ===

    @NotNull
    @Column(name = "rotation_a_plus", precision = 5, scale = 2, nullable = false)
    private BigDecimal rotationAPlus = new BigDecimal("12.0");

    @NotNull
    @Column(name = "rotation_a", precision = 5, scale = 2, nullable = false)
    private BigDecimal rotationA = new BigDecimal("8.0");

    @NotNull
    @Column(name = "rotation_b", precision = 5, scale = 2, nullable = false)
    private BigDecimal rotationB = new BigDecimal("4.0");

    @NotNull
    @Column(name = "rotation_c", precision = 5, scale = 2, nullable = false)
    private BigDecimal rotationC = new BigDecimal("2.0");

    // === Périodes d'analyse ===

    /**
     * Nombre de mois d'historique à analyser (6 à 24)
     */
    @NotNull
    @Min(6)
    @Max(24)
    @Column(name = "nb_mois_analyse", nullable = false)
    private Integer nbMoisAnalyse = 12;

    /**
     * Nombre minimum de mois pour considérer un produit comme "nouveau"
     * Les produits nouveaux gardent leur classe par défaut (B)
     */
    @NotNull
    @Min(1)
    @Max(12)
    @Column(name = "nb_mois_min_nouveau_produit", nullable = false)
    private Integer nbMoisMinNouveauProduit = 6;

    // === Stabilité ===

    /**
     * Écart minimum de score requis pour changer de classe (hysteresis)
     * Évite les oscillations de classe pour les produits proches des seuils
     */
    @NotNull
    @Min(0)
    @Max(50)
    @Column(name = "changement_min_score", nullable = false)
    private Integer changementMinScore = 10;

    // === Activation ===

    /**
     * Active ou désactive la classification automatique
     */
    @Column(name = "auto_classification_enabled", nullable = false)
    private Boolean autoClassificationEnabled = true;

    // === Audit ===

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private AppUser updatedBy;

    // === Getters & Setters ===

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BigDecimal getPoidsCa() {
        return poidsCa;
    }

    public ClassificationConfig setPoidsCa(BigDecimal poidsCa) {
        this.poidsCa = poidsCa;
        return this;
    }

    public BigDecimal getPoidsRotation() {
        return poidsRotation;
    }

    public ClassificationConfig setPoidsRotation(BigDecimal poidsRotation) {
        this.poidsRotation = poidsRotation;
        return this;
    }

    public BigDecimal getPoidsFrequence() {
        return poidsFrequence;
    }

    public ClassificationConfig setPoidsFrequence(BigDecimal poidsFrequence) {
        this.poidsFrequence = poidsFrequence;
        return this;
    }

    public Integer getSeuilAPlus() {
        return seuilAPlus;
    }

    public ClassificationConfig setSeuilAPlus(Integer seuilAPlus) {
        this.seuilAPlus = seuilAPlus;
        return this;
    }

    public Integer getSeuilA() {
        return seuilA;
    }

    public ClassificationConfig setSeuilA(Integer seuilA) {
        this.seuilA = seuilA;
        return this;
    }

    public Integer getSeuilB() {
        return seuilB;
    }

    public ClassificationConfig setSeuilB(Integer seuilB) {
        this.seuilB = seuilB;
        return this;
    }

    public Integer getSeuilC() {
        return seuilC;
    }

    public ClassificationConfig setSeuilC(Integer seuilC) {
        this.seuilC = seuilC;
        return this;
    }

    public BigDecimal getRotationAPlus() {
        return rotationAPlus;
    }

    public ClassificationConfig setRotationAPlus(BigDecimal rotationAPlus) {
        this.rotationAPlus = rotationAPlus;
        return this;
    }

    public BigDecimal getRotationA() {
        return rotationA;
    }

    public ClassificationConfig setRotationA(BigDecimal rotationA) {
        this.rotationA = rotationA;
        return this;
    }

    public BigDecimal getRotationB() {
        return rotationB;
    }

    public ClassificationConfig setRotationB(BigDecimal rotationB) {
        this.rotationB = rotationB;
        return this;
    }

    public BigDecimal getRotationC() {
        return rotationC;
    }

    public ClassificationConfig setRotationC(BigDecimal rotationC) {
        this.rotationC = rotationC;
        return this;
    }

    public Integer getNbMoisAnalyse() {
        return nbMoisAnalyse;
    }

    public ClassificationConfig setNbMoisAnalyse(Integer nbMoisAnalyse) {
        this.nbMoisAnalyse = nbMoisAnalyse;
        return this;
    }

    public Integer getNbMoisMinNouveauProduit() {
        return nbMoisMinNouveauProduit;
    }

    public ClassificationConfig setNbMoisMinNouveauProduit(Integer nbMoisMinNouveauProduit) {
        this.nbMoisMinNouveauProduit = nbMoisMinNouveauProduit;
        return this;
    }

    public Integer getChangementMinScore() {
        return changementMinScore;
    }

    public ClassificationConfig setChangementMinScore(Integer changementMinScore) {
        this.changementMinScore = changementMinScore;
        return this;
    }

    public Boolean getAutoClassificationEnabled() {
        return autoClassificationEnabled;
    }

    public ClassificationConfig setAutoClassificationEnabled(Boolean autoClassificationEnabled) {
        this.autoClassificationEnabled = autoClassificationEnabled;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public AppUser getUpdatedBy() {
        return updatedBy;
    }

    public ClassificationConfig setUpdatedBy(AppUser updatedBy) {
        this.updatedBy = updatedBy;
        return this;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Vérifie que la somme des poids est égale à 1.0
     */
    public boolean isPoidsValide() {
        BigDecimal somme = poidsCa.add(poidsRotation).add(poidsFrequence);
        return somme.compareTo(BigDecimal.ONE) == 0;
    }

    /**
     * Vérifie que les seuils sont cohérents (décroissants)
     */
    public boolean isSeuilsValides() {
        return seuilAPlus > seuilA && seuilA > seuilB && seuilB > seuilC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassificationConfig that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ClassificationConfig{" +
            "id=" + id +
            ", poidsCa=" + poidsCa +
            ", poidsRotation=" + poidsRotation +
            ", poidsFrequence=" + poidsFrequence +
            ", seuilAPlus=" + seuilAPlus +
            ", seuilA=" + seuilA +
            ", seuilB=" + seuilB +
            ", seuilC=" + seuilC +
            ", autoClassificationEnabled=" + autoClassificationEnabled +
            '}';
    }
}

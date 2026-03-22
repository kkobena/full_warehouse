package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Configuration des paramètres de classification ABC Pareto de criticité.
 * Table singleton — contient les seuils Pareto, les seuils CMM et les flags officine.
 *
 * <p>Schéma de classification :
 * <ul>
 *   <li>Seuils Pareto : ca_cumule_pct ≤ seuilParetoAPlus → A_PLUS, ≤ A → A, etc.</li>
 *   <li>Override fréquence : si frequence_mois &lt; seuilFrequenceMinMois → D (même si Pareto dit mieux)</li>
 *   <li>Override ordonnance : si estMedicamentEssentiel → jamais en dessous de B (ou CMM si activerClassificationOrdo)</li>
 *   <li>Override garde : si estProduitGarde → toujours A_PLUS</li>
 * </ul>
 */
@Entity
@Table(name = "classification_config")
public class ClassificationConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // === Seuils Pareto (% CA cumulé) ===
    // Interprétation : produit dont ca_cumule_pct ≤ seuil → classe correspondante.
    // ca_cumule_pct faible = produit très vendeur (contribue aux premiers % du CA).

    /** Les produits représentant les premiers seuilParetoAPlus % du CA → A_PLUS */
    @NotNull
    @Min(1)
    @Max(99)
    @Column(name = "seuil_pareto_a_plus", nullable = false)
    private Integer seuilParetoAPlus = 60;

    /** Les produits dans l'intervalle ]seuilParetoAPlus, seuilParetoA] % du CA cumulé → A */
    @NotNull
    @Min(1)
    @Max(99)
    @Column(name = "seuil_pareto_a", nullable = false)
    private Integer seuilParetoA = 80;

    /** Les produits dans l'intervalle ]seuilParetoA, seuilParetoB] → B */
    @NotNull
    @Min(1)
    @Max(99)
    @Column(name = "seuil_pareto_b", nullable = false)
    private Integer seuilParetoB = 95;

    /** Les produits dans l'intervalle ]seuilParetoB, seuilParetoC] → C ; au-delà → D */
    @NotNull
    @Min(1)
    @Max(100)
    @Column(name = "seuil_pareto_c", nullable = false)
    private Integer seuilParetoC = 99;

    // === Fréquence de vente ===

    /**
     * Nombre minimum de mois distincts avec des ventes sur 12 mois.
     * Si frequence_mois < seuilFrequenceMinMois → classe D, même si le Pareto est bon.
     * Protège contre les ventes ponctuelles massives (ex. commande annuelle unique).
     */
    @NotNull
    @Min(0)
    @Max(12)
    @Column(name = "seuil_frequence_min_mois", nullable = false)
    private Integer seuilFrequenceMinMois = 3;

    // === Seuils CMM pour médicaments essentiels (activerClassificationOrdo requis) ===

    /** CMM ≥ cmmSeuilAPlus → classe A_PLUS pour produit essentiel */
    @NotNull
    @Min(0)
    @Column(name = "cmm_seuil_a_plus", nullable = false)
    private Integer cmmSeuilAPlus = 50;

    /** CMM ≥ cmmSeuilA → classe A pour produit essentiel */
    @NotNull
    @Min(0)
    @Column(name = "cmm_seuil_a", nullable = false)
    private Integer cmmSeuilA = 20;

    /** CMM ≥ cmmSeuilB → classe B pour produit essentiel (plancher) */
    @NotNull
    @Min(0)
    @Column(name = "cmm_seuil_b", nullable = false)
    private Integer cmmSeuilB = 5;

    /** CMM ≥ cmmSeuilC → classe C pour produit essentiel (sinon D) */
    @NotNull
    @Min(0)
    @Column(name = "cmm_seuil_c", nullable = false)
    private Integer cmmSeuilC = 1;

    // === Stabilité (hysteresis) ===

    /**
     * Écart minimum en points Pareto (ca_cumule_pct) entre la position actuelle
     * et la frontière de la classe cible pour déclencher un reclassement.
     * Évite les oscillations pour les produits proches des seuils.
     */
    @NotNull
    @Min(0)
    @Max(20)
    @Column(name = "changement_min_pourcentage", nullable = false)
    private Integer changementMinPourcentage = 3;

    // === Activation options ===

    /** Active la classification CMM pour les produits sous ordonnance (estMedicamentEssentiel) */
    @NotNull
    @Column(name = "activer_classification_ordo", nullable = false)
    private Boolean activerClassificationOrdo = true;

    /** Active la correction saisonnière (phase future) */
    @NotNull
    @Column(name = "activer_correction_saisonniere", nullable = false)
    private Boolean activerCorrectionSaisonniere = false;

    /**
     * Ratio max_mensuel / VMM à partir duquel un produit est détecté comme saisonnier.
     * Actif uniquement si {@code activerCorrectionSaisonniere = true}.
     */
    @NotNull
    @Min(2)
    @Max(10)
    @Column(name = "indice_saisonnalite_min", nullable = false)
    private Integer indiceSaisonnaliteMin = 3;

    /**
     * Fenêtre glissante en mois pour le recalcul du score sur le pic saisonnier.
     * Actif uniquement si {@code activerCorrectionSaisonniere = true}.
     */
    @NotNull
    @Min(1)
    @Max(6)
    @Column(name = "nb_mois_saison_analyse", nullable = false)
    private Integer nbMoisSaisonAnalyse = 3;

    // === Période d'analyse ===

    /**
     * Nombre minimum de mois depuis la création pour traiter un produit comme "établi".
     * Les produits plus jeunes gardent leur classe par défaut (B) sans reclassification.
     */
    @NotNull
    @Min(1)
    @Max(12)
    @Column(name = "nb_mois_min_nouveau_produit", nullable = false)
    private Integer nbMoisMinNouveauProduit = 6;

    // === Activation globale ===

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

    public Integer getSeuilParetoAPlus() {
        return seuilParetoAPlus;
    }

    public ClassificationConfig setSeuilParetoAPlus(Integer seuilParetoAPlus) {
        this.seuilParetoAPlus = seuilParetoAPlus;
        return this;
    }

    public Integer getSeuilParetoA() {
        return seuilParetoA;
    }

    public ClassificationConfig setSeuilParetoA(Integer seuilParetoA) {
        this.seuilParetoA = seuilParetoA;
        return this;
    }

    public Integer getSeuilParetoB() {
        return seuilParetoB;
    }

    public ClassificationConfig setSeuilParetoB(Integer seuilParetoB) {
        this.seuilParetoB = seuilParetoB;
        return this;
    }

    public Integer getSeuilParetoC() {
        return seuilParetoC;
    }

    public ClassificationConfig setSeuilParetoC(Integer seuilParetoC) {
        this.seuilParetoC = seuilParetoC;
        return this;
    }

    public Integer getSeuilFrequenceMinMois() {
        return seuilFrequenceMinMois;
    }

    public ClassificationConfig setSeuilFrequenceMinMois(Integer seuilFrequenceMinMois) {
        this.seuilFrequenceMinMois = seuilFrequenceMinMois;
        return this;
    }

    public Integer getCmmSeuilAPlus() {
        return cmmSeuilAPlus;
    }

    public ClassificationConfig setCmmSeuilAPlus(Integer cmmSeuilAPlus) {
        this.cmmSeuilAPlus = cmmSeuilAPlus;
        return this;
    }

    public Integer getCmmSeuilA() {
        return cmmSeuilA;
    }

    public ClassificationConfig setCmmSeuilA(Integer cmmSeuilA) {
        this.cmmSeuilA = cmmSeuilA;
        return this;
    }

    public Integer getCmmSeuilB() {
        return cmmSeuilB;
    }

    public ClassificationConfig setCmmSeuilB(Integer cmmSeuilB) {
        this.cmmSeuilB = cmmSeuilB;
        return this;
    }

    public Integer getCmmSeuilC() {
        return cmmSeuilC;
    }

    public ClassificationConfig setCmmSeuilC(Integer cmmSeuilC) {
        this.cmmSeuilC = cmmSeuilC;
        return this;
    }

    public Integer getChangementMinPourcentage() {
        return changementMinPourcentage;
    }

    public ClassificationConfig setChangementMinPourcentage(Integer changementMinPourcentage) {
        this.changementMinPourcentage = changementMinPourcentage;
        return this;
    }

    public Boolean getActiverClassificationOrdo() {
        return activerClassificationOrdo;
    }

    public ClassificationConfig setActiverClassificationOrdo(Boolean activerClassificationOrdo) {
        this.activerClassificationOrdo = activerClassificationOrdo;
        return this;
    }

    public Boolean getActiverCorrectionSaisonniere() {
        return activerCorrectionSaisonniere;
    }

    public ClassificationConfig setActiverCorrectionSaisonniere(Boolean activerCorrectionSaisonniere) {
        this.activerCorrectionSaisonniere = activerCorrectionSaisonniere;
        return this;
    }

    public Integer getIndiceSaisonnaliteMin() {
        return indiceSaisonnaliteMin;
    }

    public ClassificationConfig setIndiceSaisonnaliteMin(Integer indiceSaisonnaliteMin) {
        this.indiceSaisonnaliteMin = indiceSaisonnaliteMin;
        return this;
    }

    public Integer getNbMoisSaisonAnalyse() {
        return nbMoisSaisonAnalyse;
    }

    public ClassificationConfig setNbMoisSaisonAnalyse(Integer nbMoisSaisonAnalyse) {
        this.nbMoisSaisonAnalyse = nbMoisSaisonAnalyse;
        return this;
    }

    public Integer getNbMoisMinNouveauProduit() {
        return nbMoisMinNouveauProduit;
    }

    public ClassificationConfig setNbMoisMinNouveauProduit(Integer nbMoisMinNouveauProduit) {
        this.nbMoisMinNouveauProduit = nbMoisMinNouveauProduit;
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
     * Vérifie que les seuils Pareto sont cohérents (strictement croissants, ≤ 100).
     */
    public boolean isSeuilsValides() {
        return seuilParetoAPlus < seuilParetoA
            && seuilParetoA < seuilParetoB
            && seuilParetoB < seuilParetoC
            && seuilParetoC <= 100;
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
            ", seuilParetoAPlus=" + seuilParetoAPlus +
            ", seuilParetoA=" + seuilParetoA +
            ", seuilParetoB=" + seuilParetoB +
            ", seuilParetoC=" + seuilParetoC +
            ", seuilFrequenceMinMois=" + seuilFrequenceMinMois +
            ", changementMinPourcentage=" + changementMinPourcentage +
            ", activerClassificationOrdo=" + activerClassificationOrdo +
            ", autoClassificationEnabled=" + autoClassificationEnabled +
            '}';
    }
}

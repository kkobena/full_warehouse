package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ClassificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Log des changements de classe de criticité des produits.
 * Permet de tracer l'historique des reclassifications automatiques et manuelles.
 */
@Entity
@Table(
    name = "classification_criticite_log",
    indexes = {
        @Index(name = "idx_classif_log_produit", columnList = "produit_id"),
        @Index(name = "idx_classif_log_date", columnList = "created_at"),
        @Index(name = "idx_classif_log_type", columnList = "classification_type")
    }
)
public class ClassificationCriticiteLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "produit_id", nullable = false)
    @NotNull
    private Produit produit;

    /**
     * Ancienne classe de criticité (null si première classification)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ancienne_classe", length = 3)
    private ClasseCriticite ancienneClasse;

    /**
     * Nouvelle classe de criticité attribuée
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "nouvelle_classe", length = 3, nullable = false)
    @NotNull
    private ClasseCriticite nouvelleClasse;

    /**
     * VMM (Ventes Mensuelles Moyennes) sur 12 mois au moment du changement
     */
    @Column(name = "vmm_12_mois")
    private Integer vmm12Mois;

    /**
     * Chiffre d'affaires sur 12 mois en centimes
     */
    @Column(name = "ca_12_mois")
    private Long ca12Mois;

    /**
     * Rotation annuelle (ventes / stock moyen)
     */
    @Column(name = "rotation_annuelle", precision = 6, scale = 2)
    private BigDecimal rotationAnnuelle;

    /**
     * Nombre de mois avec ventes sur les 12 derniers mois
     */
    @Column(name = "frequence_vente_mois")
    private Integer frequenceVenteMois;

    /**
     * Score total calculé (0-100)
     */
    @Column(name = "score_total", precision = 5, scale = 2)
    private BigDecimal scoreTotal;

    /**
     * Raison du changement de classe
     */
    @Column(name = "raison_changement", length = 255)
    private String raisonChangement;

    /**
     * Type de classification (AUTO, MANUAL, INITIAL)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "classification_type", length = 20, nullable = false)
    @NotNull
    private ClassificationType classificationType = ClassificationType.AUTO;

    /**
     * Utilisateur ayant effectué le changement (null si automatique)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull
    private LocalDateTime createdAt = LocalDateTime.now();

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

    public ClassificationCriticiteLog setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public ClasseCriticite getAncienneClasse() {
        return ancienneClasse;
    }

    public ClassificationCriticiteLog setAncienneClasse(ClasseCriticite ancienneClasse) {
        this.ancienneClasse = ancienneClasse;
        return this;
    }

    public ClasseCriticite getNouvelleClasse() {
        return nouvelleClasse;
    }

    public ClassificationCriticiteLog setNouvelleClasse(ClasseCriticite nouvelleClasse) {
        this.nouvelleClasse = nouvelleClasse;
        return this;
    }

    public Integer getVmm12Mois() {
        return vmm12Mois;
    }

    public ClassificationCriticiteLog setVmm12Mois(Integer vmm12Mois) {
        this.vmm12Mois = vmm12Mois;
        return this;
    }

    public Long getCa12Mois() {
        return ca12Mois;
    }

    public ClassificationCriticiteLog setCa12Mois(Long ca12Mois) {
        this.ca12Mois = ca12Mois;
        return this;
    }

    public BigDecimal getRotationAnnuelle() {
        return rotationAnnuelle;
    }

    public ClassificationCriticiteLog setRotationAnnuelle(BigDecimal rotationAnnuelle) {
        this.rotationAnnuelle = rotationAnnuelle;
        return this;
    }

    public Integer getFrequenceVenteMois() {
        return frequenceVenteMois;
    }

    public ClassificationCriticiteLog setFrequenceVenteMois(Integer frequenceVenteMois) {
        this.frequenceVenteMois = frequenceVenteMois;
        return this;
    }

    public BigDecimal getScoreTotal() {
        return scoreTotal;
    }

    public ClassificationCriticiteLog setScoreTotal(BigDecimal scoreTotal) {
        this.scoreTotal = scoreTotal;
        return this;
    }

    public String getRaisonChangement() {
        return raisonChangement;
    }

    public ClassificationCriticiteLog setRaisonChangement(String raisonChangement) {
        this.raisonChangement = raisonChangement;
        return this;
    }

    public ClassificationType getClassificationType() {
        return classificationType;
    }

    public ClassificationCriticiteLog setClassificationType(ClassificationType classificationType) {
        this.classificationType = classificationType;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public ClassificationCriticiteLog setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassificationCriticiteLog that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ClassificationCriticiteLog{" +
            "id=" + id +
            ", ancienneClasse=" + ancienneClasse +
            ", nouvelleClasse=" + nouvelleClasse +
            ", scoreTotal=" + scoreTotal +
            ", classificationType=" + classificationType +
            ", createdAt=" + createdAt +
            '}';
    }
}

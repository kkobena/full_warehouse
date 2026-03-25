package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "classe_criticite", length = 3, nullable = false)
    @NotNull
    private ClasseCriticite classeCriticite = ClasseCriticite.B;

    @Column(name = "delai_livraison_jours")
    private Integer delaiLivraisonJours;

    /**
     * Axe 5 — Surcharge de la fréquence de commande pour ce produit spécifique (nullable).
     * {@code null} = utiliser la fréquence du groupe fournisseur (ou défaut 7 jours).
     */
    @Column(name = "frequence_commande_jours")
    private Integer frequenceCommandeJours;

    @Column(name = "marge_securite")
    private Integer margeSecurite;

    @Column(name = "stock_objectif_calcule")
    private Integer stockObjectifCalcule;

    @Column(name = "vmm_calcule")
    private Integer vmmCalcule;

    @Column(name = "date_dernier_calcul")
    private LocalDateTime dateDernierCalcul;

    @DecimalMin("0.1")
    @DecimalMax("3.0")
    @Column(name = "facteur_saisonnier_actuel", precision = 3, scale = 2)
    private BigDecimal facteurSaisonnierActuel = BigDecimal.ONE;

    /**
     * Indique que le facteur saisonnier a été saisi manuellement par le pharmacien.
     * Quand {@code true}, l'auto-calcul SEMOIS (Axe 4) ne l'écrasera pas.
     * Quand {@code false} (défaut), le facteur est recalculé chaque mois depuis
     * l'historique des ventes N-1 / N-2 du même mois.
     */
    @Column(name = "facteur_saisonnier_manuel", nullable = false)
    private boolean facteurSaisonnierManuel = false;

    @Column(name = "limite_peremption")
    private Boolean limitePeremption;

    @NotNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

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
    }

    public Integer getDelaiLivraisonJours() {
        return delaiLivraisonJours;
    }

    public void setDelaiLivraisonJours(Integer delaiLivraisonJours) {
        this.delaiLivraisonJours = delaiLivraisonJours;
    }

    public Integer getFrequenceCommandeJours() {
        return frequenceCommandeJours;
    }

    public void setFrequenceCommandeJours(Integer frequenceCommandeJours) {
        this.frequenceCommandeJours = frequenceCommandeJours;
    }

    public Integer getMargeSecurite() {
        return margeSecurite;
    }

    public void setMargeSecurite(Integer margeSecurite) {
        this.margeSecurite = margeSecurite;
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

    public boolean isFacteurSaisonnierManuel() {
        return facteurSaisonnierManuel;
    }

    public void setFacteurSaisonnierManuel(boolean facteurSaisonnierManuel) {
        this.facteurSaisonnierManuel = facteurSaisonnierManuel;
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

    public static SemoisConfiguration createDefault(Produit produit, ClasseCriticite classe) {
        SemoisConfiguration config = new SemoisConfiguration();
        config.setProduit(produit);
        config.setClasseCriticite(classe);
        config.setFacteurSaisonnierActuel(BigDecimal.ONE);
        return config;
    }

    public void updateCalculs(Integer vmm, Integer margeSecurite, Integer stockObjectif) {
        this.vmmCalcule = vmm;
        this.margeSecurite = margeSecurite;
        this.stockObjectifCalcule = stockObjectif;
        this.dateDernierCalcul = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
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
            ", delaiLivraisonJours=" + delaiLivraisonJours +
            ", vmmCalcule=" + vmmCalcule +
            ", stockObjectifCalcule=" + stockObjectifCalcule +
            '}';
    }
}

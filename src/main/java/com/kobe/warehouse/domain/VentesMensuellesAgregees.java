package com.kobe.warehouse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * Agrégation mensuelle des ventes par produit.
 * Utilisé pour le calcul du VMM (Ventes Mensuelles Moyennes) dans la méthode SEMOIS.
 * <p>
 * Stratégie d'agrégation:
 * - Recalcul quotidien pour le mois en cours
 * - Recalcul quotidien pendant J+0 à J+7 pour le mois précédent
 * - Gel définitif à J+8 (is_frozen = TRUE)
 */
@Entity
@Table(
    name = "ventes_mensuelles_agregees",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_ventes_agregees_produit_mois",
        columnNames = {"produit_id", "annee_mois"}
    ),
    indexes = {
        @Index(name = "idx_ventes_mensuelles_produit", columnList = "produit_id"),
        @Index(name = "idx_ventes_mensuelles_date", columnList = "annee_mois"),
        @Index(name = "idx_ventes_mensuelles_frozen", columnList = "is_frozen, annee_mois"),
        @Index(name = "idx_ventes_mensuelles_updated", columnList = "updated_at")
    }
)
public class VentesMensuellesAgregees implements Serializable {

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
     * Mois au format YYYY-MM (ex: "2025-12")
     */
    @NotNull
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "Format attendu: YYYY-MM")
    @Column(name = "annee_mois", length = 7, nullable = false)
    private String anneeMois;

    /**
     * Quantité totale vendue durant le mois
     */
    @NotNull
    @Min(0)
    @Column(name = "quantite_vendue", nullable = false)
    private Integer quantiteVendue = 0;

    /**
     * Montant total du chiffre d'affaires en centimes
     */
    @NotNull
    @Min(0)
    @Column(name = "montant_ca", nullable = false)
    private Integer montantCa = 0;

    /**
     * Nombre de transactions de vente distinctes
     */
    @NotNull
    @Min(0)
    @Column(name = "nombre_ventes", nullable = false)
    private Integer nombreVentes = 0;

    /**
     * Indique si le mois est gelé (immuable après J+8)
     */
    @NotNull
    @Column(name = "is_frozen", nullable = false)
    private Boolean isFrozen = false;

    /**
     * Date à laquelle le mois a été gelé définitivement
     */
    @Column(name = "freeze_date")
    private LocalDateTime freezeDate;

    /**
     * Indique si le produit était en rupture fournisseur documentée durant ce mois.
     * Quand TRUE, ce mois est exclu du calcul VMM SEMOIS pour éviter de sous-estimer
     * la consommation réelle (les ventes auraient été plus élevées sans la rupture).
     */
    @NotNull
    @Column(name = "est_rupture_fournisseur", nullable = false)
    private Boolean estRuptureFournisseur = false;

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

    public String getAnneeMois() {
        return anneeMois;
    }

    public void setAnneeMois(String anneeMois) {
        this.anneeMois = anneeMois;
    }

    public Integer getQuantiteVendue() {
        return quantiteVendue;
    }

    public void setQuantiteVendue(Integer quantiteVendue) {
        this.quantiteVendue = quantiteVendue;
    }

    public Integer getMontantCa() {
        return montantCa;
    }

    public void setMontantCa(Integer montantCa) {
        this.montantCa = montantCa;
    }

    public Integer getNombreVentes() {
        return nombreVentes;
    }

    public void setNombreVentes(Integer nombreVentes) {
        this.nombreVentes = nombreVentes;
    }

    public Boolean getIsFrozen() {
        return isFrozen;
    }

    public void setIsFrozen(Boolean frozen) {
        isFrozen = frozen;
    }

    public LocalDateTime getFreezeDate() {
        return freezeDate;
    }

    public void setFreezeDate(LocalDateTime freezeDate) {
        this.freezeDate = freezeDate;
    }

    public Boolean getEstRuptureFournisseur() {
        return estRuptureFournisseur;
    }

    public void setEstRuptureFournisseur(Boolean estRuptureFournisseur) {
        this.estRuptureFournisseur = estRuptureFournisseur;
    }

    /** Marque ce mois comme mois de rupture fournisseur (exclu du calcul VMM). */
    public void marquerRupture() {
        this.estRuptureFournisseur = true;
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



    /**
     * Convertit anneeMois (String) en YearMonth
     *
     * @return Le mois au format YearMonth
     */
    public YearMonth getYearMonth() {
        return anneeMois != null ? YearMonth.parse(anneeMois) : null;
    }

    /**
     * Définit le mois à partir d'un YearMonth
     *
     * @param yearMonth Le mois à définir
     */
    public void setYearMonth(YearMonth yearMonth) {
        this.anneeMois = yearMonth != null ? yearMonth.toString() : null;
    }

    /**
     * Gèle définitivement ce mois
     */
    public void freeze() {
        this.isFrozen = true;
        this.freezeDate = LocalDateTime.now();
    }

    /**
     * Dégèle ce mois (usage exceptionnel uniquement)
     */
    public void unfreeze() {
        this.isFrozen = false;
        this.freezeDate = null;
    }

    /**
     * Vérifie si ce mois peut être mis à jour
     *
     * @return true si le mois n'est pas gelé
     */
    public boolean isModifiable() {
        return !Boolean.TRUE.equals(isFrozen);
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
        if (!(o instanceof VentesMensuellesAgregees that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "VentesMensuellesAgregees{" +
            "id=" + id +
            ", anneeMois='" + anneeMois + '\'' +
            ", quantiteVendue=" + quantiteVendue +
            ", montantCa=" + montantCa +
            ", nombreVentes=" + nombreVentes +
            ", isFrozen=" + isFrozen +
            ", estRuptureFournisseur=" + estRuptureFournisseur +
            '}';
    }
}

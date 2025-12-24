package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entité en lecture seule mappée sur la vue v_produit_metriques_classification.
 * Cette vue contient les métriques de classification pour chaque produit actif.
 *
 * IMPORTANT: Cette entité est IMMUTABLE (lecture seule).
 * Les données sont calculées dynamiquement par la vue SQL.
 */
@Entity
@Table(name = "v_produit_metriques_classification")
@Immutable
public class ProduitMetriquesClassification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "produit_id")
    private Integer produitId;

    @Column(name = "libelle")
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "classe_actuelle", length = 10)
    private ClasseCriticite classeActuelle;

    @Column(name = "vmm_12_mois")
    private Integer vmm12Mois;

    @Column(name = "ca_12_mois")
    private Long ca12Mois;

    @Column(name = "qte_vendue_12_mois")
    private Integer qteVendue12Mois;

    @Column(name = "frequence_vente_mois")
    private Integer frequenceVenteMois;

    @Column(name = "stock_actuel")
    private Integer stockActuel;

    @Column(name = "rotation_annuelle")
    private BigDecimal rotationAnnuelle;

    @Column(name = "anciennete_mois")
    private Integer ancienneteMois;

    @Column(name = "est_nouveau_produit")
    private Boolean estNouveauProduit;

    @Column(name = "date_creation_produit")
    private LocalDateTime dateCreationProduit;

    // ==================== Getters (pas de setters: lecture seule) ====================

    public Integer getProduitId() {
        return produitId;
    }

    public String getLibelle() {
        return libelle;
    }

    public ClasseCriticite getClasseActuelle() {
        return classeActuelle;
    }

    public Integer getVmm12Mois() {
        return vmm12Mois;
    }

    public Long getCa12Mois() {
        return ca12Mois;
    }

    public Integer getQteVendue12Mois() {
        return qteVendue12Mois;
    }

    public Integer getFrequenceVenteMois() {
        return frequenceVenteMois;
    }

    public Integer getStockActuel() {
        return stockActuel;
    }

    public BigDecimal getRotationAnnuelle() {
        return rotationAnnuelle;
    }

    public Integer getAncienneteMois() {
        return ancienneteMois;
    }

    public Boolean getEstNouveauProduit() {
        return estNouveauProduit;
    }

    public LocalDateTime getDateCreationProduit() {
        return dateCreationProduit;
    }

    // ==================== Méthodes utilitaires ====================

    /**
     * Vérifie si le produit est nouveau selon le seuil donné
     *
     * @param seuilMois Nombre de mois minimum pour ne plus être considéré nouveau
     * @return true si le produit a moins de seuilMois mois d'ancienneté
     */
    public boolean isNouveauProduit(int seuilMois) {
        return ancienneteMois != null && ancienneteMois < seuilMois;
    }

    /**
     * Retourne la rotation annuelle ou 0 si null
     */
    public BigDecimal getRotationAnnuelleOrZero() {
        return rotationAnnuelle != null ? rotationAnnuelle : BigDecimal.ZERO;
    }

    /**
     * Retourne le CA 12 mois ou 0 si null
     */
    public long getCa12MoisOrZero() {
        return ca12Mois != null ? ca12Mois : 0L;
    }

    /**
     * Retourne la fréquence de vente ou 0 si null
     */
    public int getFrequenceVenteMoisOrZero() {
        return frequenceVenteMois != null ? frequenceVenteMois : 0;
    }

    // ==================== toString, equals, hashCode ====================

    @Override
    public String toString() {
        return "ProduitMetriquesClassification{" +
            "produitId=" + produitId +
            ", libelle='" + libelle + '\'' +
            ", classeActuelle=" + classeActuelle +
            ", vmm12Mois=" + vmm12Mois +
            ", ca12Mois=" + ca12Mois +
            ", rotationAnnuelle=" + rotationAnnuelle +
            ", frequenceVenteMois=" + frequenceVenteMois +
            ", ancienneteMois=" + ancienneteMois +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProduitMetriquesClassification that)) return false;
        return produitId != null && produitId.equals(that.produitId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

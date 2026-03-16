package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Historique des changements de prix d'achat et de vente d'un produit fournisseur.
 * Enregistré automatiquement à chaque entrée en stock modifiant un prix.
 * Permet l'audit comptable et la traçabilité réglementaire des prix.
 */
@Entity
@Table(
    name = "fournisseur_produit_price_history",
    indexes = {
        @Index(columnList = "fournisseur_produit_id", name = "fp_price_history_fp_id_idx"),
        @Index(columnList = "changed_at", name = "fp_price_history_changed_at_idx"),
    }
)
public class FournisseurProduitPriceHistory implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fournisseur_produit_id", nullable = false)
    private FournisseurProduit fournisseurProduit;

    @NotNull
    @Column(name = "old_prix_achat", nullable = false)
    private Integer oldPrixAchat;

    @NotNull
    @Column(name = "new_prix_achat", nullable = false)
    private Integer newPrixAchat;

    @NotNull
    @Column(name = "old_prix_uni", nullable = false)
    private Integer oldPrixUni;

    @NotNull
    @Column(name = "new_prix_uni", nullable = false)
    private Integer newPrixUni;

    @NotNull
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id")
    private AppUser changedBy;

    @Column(name = "commande_id")
    private Integer commandeId;

    @Column(name = "receipt_reference", length = 20)
    private String receiptReference;

    public Integer getId() {
        return id;
    }

    public FournisseurProduit getFournisseurProduit() {
        return fournisseurProduit;
    }

    public FournisseurProduitPriceHistory setFournisseurProduit(FournisseurProduit fournisseurProduit) {
        this.fournisseurProduit = fournisseurProduit;
        return this;
    }

    public Integer getOldPrixAchat() {
        return oldPrixAchat;
    }

    public FournisseurProduitPriceHistory setOldPrixAchat(Integer oldPrixAchat) {
        this.oldPrixAchat = oldPrixAchat;
        return this;
    }

    public Integer getNewPrixAchat() {
        return newPrixAchat;
    }

    public FournisseurProduitPriceHistory setNewPrixAchat(Integer newPrixAchat) {
        this.newPrixAchat = newPrixAchat;
        return this;
    }

    public Integer getOldPrixUni() {
        return oldPrixUni;
    }

    public FournisseurProduitPriceHistory setOldPrixUni(Integer oldPrixUni) {
        this.oldPrixUni = oldPrixUni;
        return this;
    }

    public Integer getNewPrixUni() {
        return newPrixUni;
    }

    public FournisseurProduitPriceHistory setNewPrixUni(Integer newPrixUni) {
        this.newPrixUni = newPrixUni;
        return this;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public FournisseurProduitPriceHistory setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
        return this;
    }

    public AppUser getChangedBy() {
        return changedBy;
    }

    public FournisseurProduitPriceHistory setChangedBy(AppUser changedBy) {
        this.changedBy = changedBy;
        return this;
    }

    public Integer getCommandeId() {
        return commandeId;
    }

    public FournisseurProduitPriceHistory setCommandeId(Integer commandeId) {
        this.commandeId = commandeId;
        return this;
    }

    public String getReceiptReference() {
        return receiptReference;
    }

    public FournisseurProduitPriceHistory setReceiptReference(String receiptReference) {
        this.receiptReference = receiptReference;
        return this;
    }
}

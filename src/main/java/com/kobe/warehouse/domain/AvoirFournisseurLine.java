package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "avoir_fournisseur_line")
public class AvoirFournisseurLine implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "avoir_fournisseur_id", nullable = false)
    private AvoirFournisseur avoirFournisseur;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "retour_bon_item_id", nullable = false)
    private RetourBonItem retourBonItem;

    @NotNull
    @Column(name = "qty_mvt", nullable = false)
    private Integer qtyMvt;

    @NotNull
    @Column(name = "prix_achat", nullable = false)
    private long prixAchat;

    @Column(name = "commentaire", length = 150)
    private String commentaire;

    public Integer getId() { return id; }
    public AvoirFournisseurLine setId(Integer id) { this.id = id; return this; }

    public AvoirFournisseur getAvoirFournisseur() { return avoirFournisseur; }
    public AvoirFournisseurLine setAvoirFournisseur(AvoirFournisseur avoirFournisseur) { this.avoirFournisseur = avoirFournisseur; return this; }

    public RetourBonItem getRetourBonItem() { return retourBonItem; }
    public AvoirFournisseurLine setRetourBonItem(RetourBonItem retourBonItem) { this.retourBonItem = retourBonItem; return this; }

    public Integer getQtyMvt() { return qtyMvt; }
    public AvoirFournisseurLine setQtyMvt(Integer qtyMvt) { this.qtyMvt = qtyMvt; return this; }

    public long getPrixAchat() { return prixAchat; }
    public AvoirFournisseurLine setPrixAchat(long prixAchat) { this.prixAchat = prixAchat; return this; }

    public String getCommentaire() { return commentaire; }
    public AvoirFournisseurLine setCommentaire(String commentaire) { this.commentaire = commentaire; return this; }
}

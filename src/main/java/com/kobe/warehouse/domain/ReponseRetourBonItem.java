package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "reponse_retour_bon_item")
public class ReponseRetourBonItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "date_mtv", nullable = false)
    private LocalDateTime dateMtv = LocalDateTime.now();

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "reponse_retour_bon_id", nullable = false, referencedColumnName = "id")
    private ReponseRetourBon reponseRetourBon;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "retour_bon_item_id", nullable = false, referencedColumnName = "id")
    private RetourBonItem retourBonItem;

    @NotNull
    @Min(0)
    @Column(name = "qty_mvt", nullable = false, length = 5)
    private Integer qtyMvt;

    @NotNull
    @Column(name = "prix_achat", length = 8, nullable = false)
    private Integer prixAchat;

    public Integer getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
    }

    public Integer getId() {
        return id;
    }

    public ReponseRetourBonItem setId(Integer id) {
        this.id = id;
        return this;
    }

    public @NotNull LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public ReponseRetourBonItem setDateMtv(@NotNull LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public @NotNull ReponseRetourBon getReponseRetourBon() {
        return reponseRetourBon;
    }

    public ReponseRetourBonItem setReponseRetourBon(@NotNull ReponseRetourBon reponseRetourBon) {
        this.reponseRetourBon = reponseRetourBon;
        return this;
    }

    public @NotNull RetourBonItem getRetourBonItem() {
        return retourBonItem;
    }

    public ReponseRetourBonItem setRetourBonItem(@NotNull RetourBonItem retourBonItem) {
        this.retourBonItem = retourBonItem;
        return this;
    }

    public @NotNull @Min(0) Integer getQtyMvt() {
        return qtyMvt;
    }

    public ReponseRetourBonItem setQtyMvt(@NotNull @Min(0) Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }
}

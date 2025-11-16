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
@Table(name = "retour_bon_item")
public class RetourBonItem implements Serializable {

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
    @JoinColumn(name = "retour_bon_id", referencedColumnName = "id")
    private RetourBon retourBon;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "motif_retour_id", referencedColumnName = "id")
    private MotifRetourProduit motifRetour;

    @ManyToOne(optional = false)
    @NotNull
    private OrderLine orderLine;

    @ManyToOne
    private Lot lot;

    @NotNull
    @Min(1)
    @Column(name = "qty_mvt", nullable = false, length = 8)
    private Integer qtyMvt;

    @Column(name = "init_stock", nullable = false, length = 8)
    private Integer initStock;

    @Column(name = "after_stock", length = 8)
    private Integer afterStock;

    @Column(name = "accepted_qty", length = 8)
    private Integer acceptedQty;

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

    public void setId(Integer id) {
        this.id = id;
    }

    public @NotNull LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public void setDateMtv(@NotNull LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
    }

    public @NotNull RetourBon getRetourBon() {
        return retourBon;
    }

    public void setRetourBon(@NotNull RetourBon retourBon) {
        this.retourBon = retourBon;
    }

    public @NotNull MotifRetourProduit getMotifRetour() {
        return motifRetour;
    }

    public void setMotifRetour(@NotNull MotifRetourProduit motifRetour) {
        this.motifRetour = motifRetour;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public void setOrderLine(OrderLine orderLine) {
        this.orderLine = orderLine;
    }

    public Lot getLot() {
        return lot;
    }

    public void setLot(Lot lot) {
        this.lot = lot;
    }

    public @NotNull @Min(1) Integer getQtyMvt() {
        return qtyMvt;
    }

    public void setQtyMvt(@NotNull @Min(1) Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Integer getInitStock() {
        return initStock;
    }

    public void setInitStock(Integer initStock) {
        this.initStock = initStock;
    }

    public Integer getAcceptedQty() {
        return acceptedQty;
    }

    public void setAcceptedQty(Integer acceptedQty) {
        this.acceptedQty = acceptedQty;
    }

    public Integer getAfterStock() {
        return afterStock;
    }

    public void setAfterStock(Integer afterStock) {
        this.afterStock = afterStock;
    }
}

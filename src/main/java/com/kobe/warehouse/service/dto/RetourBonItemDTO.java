package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.RetourBonItem;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RetourBonItemDTO {
    private Integer id;
    private LocalDateTime dateMtv;
    private Integer retourBonId;
    private Integer motifRetourId;
    private String motifRetourLibelle;
    private Integer orderLineId;
    private LocalDate orderLineOrderDate;
    private String produitLibelle;
    private String produitCip;
    private Integer produitId;
    private Integer qtyMvt;
    private Integer initStock;
    private Integer afterStock;
    private Integer lotId;
    private String lotNumero;
    private Integer orderLineQuantityRequested;
    private Integer orderLineQuantityReceived;

    public RetourBonItemDTO() {
    }

    public RetourBonItemDTO(RetourBonItem item) {
        this.id = item.getId();
        this.dateMtv = item.getDateMtv();
        if (item.getRetourBon() != null) {
            this.retourBonId = item.getRetourBon().getId();
        }
        if (item.getMotifRetour() != null) {
            this.motifRetourId = item.getMotifRetour().getId();
            this.motifRetourLibelle = item.getMotifRetour().getLibelle();
        }
        if (item.getOrderLine() != null) {
            this.orderLineId = item.getOrderLine().getId().getId();
            this.orderLineOrderDate = item.getOrderLine().getId().getOrderDate();
            this.orderLineQuantityRequested = item.getOrderLine().getQuantityRequested();
            this.orderLineQuantityReceived = item.getOrderLine().getQuantityReceived();
            if (item.getOrderLine().getFournisseurProduit() != null) {
                this.produitCip = item.getOrderLine().getFournisseurProduit().getCodeCip();
                if (item.getOrderLine().getFournisseurProduit().getProduit() != null) {
                    this.produitLibelle = item.getOrderLine().getFournisseurProduit().getProduit().getLibelle();
                    this.produitId = item.getOrderLine().getFournisseurProduit().getProduit().getId();
                }
            }
        }
        if (item.getLot() != null) {
            this.lotId = item.getLot().getId();
            this.lotNumero = item.getLot().getNumLot();
        }
        this.qtyMvt = item.getQtyMvt();
        this.initStock = item.getInitStock();
        this.afterStock = item.getAfterStock();
    }

    public Integer getId() {
        return id;
    }

    public RetourBonItemDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public RetourBonItemDTO setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public Integer getRetourBonId() {
        return retourBonId;
    }

    public RetourBonItemDTO setRetourBonId(Integer retourBonId) {
        this.retourBonId = retourBonId;
        return this;
    }

    public Integer getMotifRetourId() {
        return motifRetourId;
    }

    public RetourBonItemDTO setMotifRetourId(Integer motifRetourId) {
        this.motifRetourId = motifRetourId;
        return this;
    }

    public String getMotifRetourLibelle() {
        return motifRetourLibelle;
    }

    public RetourBonItemDTO setMotifRetourLibelle(String motifRetourLibelle) {
        this.motifRetourLibelle = motifRetourLibelle;
        return this;
    }

    public Integer getOrderLineId() {
        return orderLineId;
    }

    public RetourBonItemDTO setOrderLineId(Integer orderLineId) {
        this.orderLineId = orderLineId;
        return this;
    }

    public LocalDate getOrderLineOrderDate() {
        return orderLineOrderDate;
    }

    public RetourBonItemDTO setOrderLineOrderDate(LocalDate orderLineOrderDate) {
        this.orderLineOrderDate = orderLineOrderDate;
        return this;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public RetourBonItemDTO setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public String getProduitCip() {
        return produitCip;
    }

    public RetourBonItemDTO setProduitCip(String produitCip) {
        this.produitCip = produitCip;
        return this;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public RetourBonItemDTO setProduitId(Integer produitId) {
        this.produitId = produitId;
        return this;
    }

    public Integer getQtyMvt() {
        return qtyMvt;
    }

    public RetourBonItemDTO setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public Integer getInitStock() {
        return initStock;
    }

    public RetourBonItemDTO setInitStock(Integer initStock) {
        this.initStock = initStock;
        return this;
    }

    public Integer getAfterStock() {
        return afterStock;
    }

    public RetourBonItemDTO setAfterStock(Integer afterStock) {
        this.afterStock = afterStock;
        return this;
    }

    public Integer getLotId() {
        return lotId;
    }

    public RetourBonItemDTO setLotId(Integer lotId) {
        this.lotId = lotId;
        return this;
    }

    public String getLotNumero() {
        return lotNumero;
    }

    public RetourBonItemDTO setLotNumero(String lotNumero) {
        this.lotNumero = lotNumero;
        return this;
    }

    public Integer getOrderLineQuantityRequested() {
        return orderLineQuantityRequested;
    }

    public RetourBonItemDTO setOrderLineQuantityRequested(Integer orderLineQuantityRequested) {
        this.orderLineQuantityRequested = orderLineQuantityRequested;
        return this;
    }

    public Integer getOrderLineQuantityReceived() {
        return orderLineQuantityReceived;
    }

    public RetourBonItemDTO setOrderLineQuantityReceived(Integer orderLineQuantityReceived) {
        this.orderLineQuantityReceived = orderLineQuantityReceived;
        return this;
    }
}

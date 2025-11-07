package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.RetourBonItem;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RetourBonItemDTO {
    private Long id;
    private LocalDateTime dateMtv;
    private Long retourBonId;
    private Long motifRetourId;
    private String motifRetourLibelle;
    private Long orderLineId;
    private LocalDate orderLineOrderDate;
    private String produitLibelle;
    private String produitCip;
    private Long produitId;
    private Integer qtyMvt;
    private Integer initStock;
    private Integer afterStock;
    private Long lotId;
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

    public Long getId() {
        return id;
    }

    public RetourBonItemDTO setId(Long id) {
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

    public Long getRetourBonId() {
        return retourBonId;
    }

    public RetourBonItemDTO setRetourBonId(Long retourBonId) {
        this.retourBonId = retourBonId;
        return this;
    }

    public Long getMotifRetourId() {
        return motifRetourId;
    }

    public RetourBonItemDTO setMotifRetourId(Long motifRetourId) {
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

    public Long getOrderLineId() {
        return orderLineId;
    }

    public RetourBonItemDTO setOrderLineId(Long orderLineId) {
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

    public Long getProduitId() {
        return produitId;
    }

    public RetourBonItemDTO setProduitId(Long produitId) {
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

    public Long getLotId() {
        return lotId;
    }

    public RetourBonItemDTO setLotId(Long lotId) {
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

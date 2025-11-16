package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.ReponseRetourBonItem;
import java.time.LocalDateTime;

public class ReponseRetourBonItemDTO {

    private Integer id;
    private LocalDateTime dateMtv;
    private Integer reponseRetourBonId;
    private Integer retourBonItemId;
    private Integer qtyMvt;

    // Display properties (not persisted, for frontend convenience)
    private String produitLibelle;
    private String produitCip;
    private String lotNumero;
    private Integer requestedQty;
    private Integer prixAchat;

    public ReponseRetourBonItemDTO() {}

    public ReponseRetourBonItemDTO(ReponseRetourBonItem item) {
        this.id = item.getId();
        this.dateMtv = item.getDateMtv();
        if (item.getReponseRetourBon() != null) {
            this.reponseRetourBonId = item.getReponseRetourBon().getId();
        }
        if (item.getRetourBonItem() != null) {
            this.retourBonItemId = item.getRetourBonItem().getId();
        }
        this.qtyMvt = item.getQtyMvt();
        this.prixAchat = item.getPrixAchat();
    }

    public Integer getId() {
        return id;
    }

    public ReponseRetourBonItemDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public ReponseRetourBonItemDTO setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public Integer getReponseRetourBonId() {
        return reponseRetourBonId;
    }

    public ReponseRetourBonItemDTO setReponseRetourBonId(Integer reponseRetourBonId) {
        this.reponseRetourBonId = reponseRetourBonId;
        return this;
    }

    public Integer getRetourBonItemId() {
        return retourBonItemId;
    }

    public ReponseRetourBonItemDTO setRetourBonItemId(Integer retourBonItemId) {
        this.retourBonItemId = retourBonItemId;
        return this;
    }

    public Integer getQtyMvt() {
        return qtyMvt;
    }

    public ReponseRetourBonItemDTO setQtyMvt(Integer qtyMvt) {
        this.qtyMvt = qtyMvt;
        return this;
    }

    public Integer getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(Integer prixAchat) {
        this.prixAchat = prixAchat;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public ReponseRetourBonItemDTO setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
        return this;
    }

    public String getProduitCip() {
        return produitCip;
    }

    public ReponseRetourBonItemDTO setProduitCip(String produitCip) {
        this.produitCip = produitCip;
        return this;
    }

    public String getLotNumero() {
        return lotNumero;
    }

    public ReponseRetourBonItemDTO setLotNumero(String lotNumero) {
        this.lotNumero = lotNumero;
        return this;
    }

    public Integer getRequestedQty() {
        return requestedQty;
    }

    public ReponseRetourBonItemDTO setRequestedQty(Integer requestedQty) {
        this.requestedQty = requestedQty;
        return this;
    }
}

package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Decondition;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class DeconditionDTO {

    private Integer stockAfter;
    private Integer stockBefore;
    private LocalDateTime dateMtv;
    private int qtyMvt;

    @NotNull
    private Integer produitId;

    public DeconditionDTO() {}

    public DeconditionDTO(Decondition decondition) {
        this.produitId = decondition.getProduit().getId();
        this.qtyMvt = decondition.getQtyMvt();
        this.stockAfter = decondition.getStockAfter();
        this.stockBefore = decondition.getStockBefore();
    }

    public int getQtyMvt() {
        return qtyMvt;
    }

    public void setQtyMvt(int qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Integer getProduitId() {
        return produitId;
    }

    public void setProduitId(Integer produitId) {
        this.produitId = produitId;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public DeconditionDTO setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public Integer getStockAfter() {
        return stockAfter;
    }

    public DeconditionDTO setStockAfter(Integer stockAfter) {
        this.stockAfter = stockAfter;
        return this;
    }

    public Integer getStockBefore() {
        return stockBefore;
    }

    public DeconditionDTO setStockBefore(Integer stockBefore) {
        this.stockBefore = stockBefore;
        return this;
    }
}

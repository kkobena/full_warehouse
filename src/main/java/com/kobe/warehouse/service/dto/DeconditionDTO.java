package com.kobe.warehouse.service.dto;

import javax.validation.constraints.NotNull;

public class DeconditionDTO {
    private int qtyMvt;
    @NotNull
   private Long produitId;

    public int getQtyMvt() {
        return qtyMvt;
    }

    public void setQtyMvt(int qtyMvt) {
        this.qtyMvt = qtyMvt;
    }

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }
}

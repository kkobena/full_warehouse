package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.util.Objects;

public class MvStockValuationRayonId implements Serializable {
    private Integer produitId;
    private Integer rayonId;

    public Integer getProduitId() {
        return produitId;
    }

    public void setProduitId(Integer produitId) {
        this.produitId = produitId;
    }

    public Integer getRayonId() {
        return rayonId;
    }

    public void setRayonId(Integer rayonId) {
        this.rayonId = rayonId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MvStockValuationRayonId that = (MvStockValuationRayonId) o;
        return Objects.equals(produitId, that.produitId) && Objects.equals(rayonId, that.rayonId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(produitId, rayonId);
    }
}

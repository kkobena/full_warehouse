package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Entité en lecture seule mappée sur la vue matérialisée mv_stock_valuation.
 * IMPORTANT: Cette entité est IMMUTABLE (lecture seule).
 */
@Entity
@Table(name = "mv_stock_valuation_by_rayon")
@Immutable // Hibernate: empêche INSERT/UPDATE/DELETE
public class MvStockValuationByRayonView  extends StockValuationView  {

    @Column(name = "rayon")
    private String rayon;
    @Column(name = "rayon_id")
    private Integer rayonId;

    public String getRayon() {
        return rayon;
    }

    public void setRayon(String rayon) {
        this.rayon = rayon;
    }

    public Integer getRayonId() {
        return rayonId;
    }

    public void setRayonId(Integer rayonId) {
        this.rayonId = rayonId;
    }
}

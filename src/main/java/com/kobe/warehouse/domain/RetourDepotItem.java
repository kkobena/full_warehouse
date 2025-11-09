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

@Entity
@Table(name = "retour_depot_item")
public class RetourDepotItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "retour_depot_id", referencedColumnName = "id")
    private RetourDepot retourDepot;


    @NotNull
    @Min(1)
    @Column(name = "qty_mvt", nullable = false, length = 8)
    private Integer qtyMvt;

    @Column(name = "init_stock", nullable = false, length = 8)
    private Integer initStock;

    @Column(name = "after_stock", length = 8)
    private Integer afterStock;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RetourDepot getRetourDepot() {
        return retourDepot;
    }

    public RetourDepotItem setRetourDepot(RetourDepot retourDepot) {
        this.retourDepot = retourDepot;
        return this;
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

    public Integer getAfterStock() {
        return afterStock;
    }

    public void setAfterStock(Integer afterStock) {
        this.afterStock = afterStock;
    }
}

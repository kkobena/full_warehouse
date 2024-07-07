package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "date_mtv", nullable = false)
  private LocalDateTime dateMtv = LocalDateTime.now();

  @ManyToOne(optional = false)
  @NotNull
  private ReponseRetourBon reponseRetourBon;

  @ManyToOne(optional = false)
  @NotNull
  private RetourBonItem retourBonItem;

  @NotNull
  @Min(0)
  @Column(name = "qty_mvt", nullable = false, length = 5)
  private Integer qtyMvt;

  @Column(name = "init_stock", nullable = false, length = 8)
  private Integer initStock;

  @Column(name = "after_stock", length = 8)
  private Integer afterStock;

    public Long getId() {
        return id;
    }

    public ReponseRetourBonItem setId(Long id) {
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

    public Integer getInitStock() {
        return initStock;
    }

    public ReponseRetourBonItem setInitStock(Integer initStock) {
        this.initStock = initStock;
        return this;
    }

    public Integer getAfterStock() {
        return afterStock;
    }

    public ReponseRetourBonItem setAfterStock(Integer afterStock) {
        this.afterStock = afterStock;
        return this;
    }
}

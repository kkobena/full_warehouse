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
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "retour_bon_item")
public class RetourBonItem implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "date_mtv", nullable = false)
  private LocalDateTime dateMtv = LocalDateTime.now();

  @ManyToOne(optional = false)
  @NotNull
  private RetourBon retourBon;

  @ManyToOne(optional = false)
  @NotNull
  private MotifRetourProduit motifRetour;

  @ManyToOne(optional = false)
  @NotNull
  private DeliveryReceiptItem deliveryReceiptItem;

    @ManyToOne
    private Lot lot;

    @NotNull
    @Min(1)
    @Column(name = "qty_mvt", nullable = false,length = 8)
    private Integer qtyMvt;
    @Column(name = "init_stock", nullable = false,length = 8)
    private Integer initStock;

    @Column(name = "after_stock",length = 8)
    private Integer afterStock;
}

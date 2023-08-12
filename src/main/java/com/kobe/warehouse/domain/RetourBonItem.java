package com.kobe.warehouse.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
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
    @Min(0)

    @Column(name = "qty_mvt", nullable = false,length = 8)
    private Integer qtyMvt;
    @Column(name = "init_stock", nullable = false,length = 8)
    private Integer initStock;

    @Column(name = "after_stock",length = 8)
    private Integer afterStock;
}

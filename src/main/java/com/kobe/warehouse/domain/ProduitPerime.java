package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "produit_perime",
    indexes = {@Index(columnList = "peremption_date", name = "produit_perime_index")})
public class ProduitPerime implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @NotNull
  private Produit produit;

  @ManyToOne private Lot lot;

  @Column(name = "created", nullable = false)
  @NotNull
  private LocalDateTime created = LocalDateTime.now();

  @Min(1)
  private int quantity;

  @NotNull
  @Column(name = "peremption_date", nullable = false)
  private LocalDate peremptionDate;

  @ManyToOne(optional = false)
  @NotNull
  private User user;

  @Min(1)
  @NotNull
  @Column(name = "init_stock", nullable = false)
  private int initStock;

  @NotNull
  @Column(name = "after_stock", nullable = false)
  private int afterStock;
}

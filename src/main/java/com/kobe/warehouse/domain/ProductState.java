package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import java.io.Serializable;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
    name = "product_state",
    indexes = {@Index(columnList = "state", name = "state_index")})
public class ProductState implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDateTime updated = LocalDateTime.now();

  @ManyToOne(optional = false)
  @NotNull
  private Produit produit;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "state", nullable = false, length = 1)
  private ProductStateEnum state;
}

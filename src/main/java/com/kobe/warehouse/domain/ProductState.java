package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_state",
    indexes = {
        @Index(columnList = "state", name = "state_index")
    }
)
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
    @Column(name = "state", nullable = false,length = 1)
    private ProductStateEnum state;

}

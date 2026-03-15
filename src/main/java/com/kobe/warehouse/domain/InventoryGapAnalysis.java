package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CauseEcart;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_gap_analysis")
public class InventoryGapAnalysis implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_inventory_line_id", nullable = false)
    private StoreInventoryLine storeInventoryLine;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cause", nullable = false, length = 30)
    private CauseEcart cause;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public StoreInventoryLine getStoreInventoryLine() { return storeInventoryLine; }
    public void setStoreInventoryLine(StoreInventoryLine storeInventoryLine) {
        this.storeInventoryLine = storeInventoryLine;
    }

    public CauseEcart getCause() { return cause; }
    public void setCause(CauseEcart cause) { this.cause = cause; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

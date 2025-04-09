package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
    name = "lot",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "num_lot", "receipt_item_id" }) },
    indexes = {
        @Index(columnList = "num_lot", name = "num_lot_index"),
        @Index(columnList = "receipt_refernce", name = "lot_receipt_refernce_index"),
    }
)
public class Lot implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "num_lot", nullable = false)
    private String numLot;

    @NotNull
    @Column(name = "receipt_refernce", nullable = false)
    private String receiptRefernce;

    @ManyToOne(optional = false)
    private DeliveryReceiptItem receiptItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "quantity_received_ug", nullable = false, columnDefinition = "int default '0'")
    private Integer ugQuantityReceived = 0;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate = LocalDateTime.now();

    @Column(name = "manufacturing_date")
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @OneToMany(mappedBy = "lot")
    private List<LotSold> lotSolds = new ArrayList<>();

    public Integer getUgQuantityReceived() {
        return ugQuantityReceived;
    }

    public Lot setUgQuantityReceived(Integer ugQuantityReceived) {
        this.ugQuantityReceived = ugQuantityReceived;
        return this;
    }

    public Long getId() {
        return id;
    }

    public Lot setId(Long id) {
        this.id = id;
        return this;
    }

    public String getReceiptRefernce() {
        return receiptRefernce;
    }

    public Lot setReceiptRefernce(String receiptRefernce) {
        this.receiptRefernce = receiptRefernce;
        return this;
    }

    public String getNumLot() {
        return numLot;
    }

    public Lot setNumLot(String numLot) {
        this.numLot = numLot;
        return this;
    }

    public DeliveryReceiptItem getReceiptItem() {
        return receiptItem;
    }

    public Lot setReceiptItem(DeliveryReceiptItem receiptItem) {
        this.receiptItem = receiptItem;
        return this;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Lot setQuantity(Integer quantity) {
        this.quantity = quantity;
        return this;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Lot setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public LocalDate getManufacturingDate() {
        return manufacturingDate;
    }

    public Lot setManufacturingDate(LocalDate manufacturingDate) {
        this.manufacturingDate = manufacturingDate;
        return this;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public Lot setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public List<LotSold> getLotSolds() {
        return lotSolds;
    }

    public Lot setLotSolds(List<LotSold> lotSolds) {
        this.lotSolds = lotSolds;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lot lot = (Lot) o;
        return id.equals(lot.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.Formula;
import org.springframework.data.domain.Persistable;

/**
 * A OrderLine.
 */
@Entity
@Table(
    name = "order_line",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "commande_id", "fournisseur_produit_id", "order_date" }) }
)
@IdClass(OrderLineId.class)
public class OrderLine implements Persistable<OrderLineId>, Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @Column(name = "quantity_received")
    private Integer quantityReceived;

    @Id
    @Column(name = "order_date")
    private LocalDate orderDate = LocalDate.now();

    @NotNull
    @Column(name = "init_stock", nullable = false)
    private Integer initStock;

    @Column(name = "final_stock")
    private Integer finalStock;

    @NotNull
    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;

    @Column(name = "quantity_returned")
    private Integer quantityReturned;

    @Column(name = "discount_amount", nullable = false)
    private int discountAmount = 0;

    @Formula("quantity_requested*order_unit_price")
    private Integer orderAmount; // montant vente  commande

    @Formula("quantity_requested*order_cost_amount")
    private Integer grossAmount; // montant achat commande

    @Column(name = "net_amount")
    private Integer netAmount = 0;

    @Column(name = "tax_amount")
    private Integer taxAmount = 0;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "receipt_date")
    private LocalDateTime receiptDate;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "orderLines", allowSetters = true)
    private Commande commande;

    @NotNull
    @Column(name = "order_unit_price", nullable = false)
    private Integer orderUnitPrice; // prix uni commande

    @NotNull
    @Column(name = "order_cost_amount", nullable = false)
    private Integer orderCostAmount; // prix d'achat commande

    @Column(name = "free_qty")
    private int freeQty;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fournisseur_produit_id", referencedColumnName = "id")
    private FournisseurProduit fournisseurProduit;

    @Column(name = "provisional_code")
    private Boolean provisionalCode = Boolean.FALSE;

    @OneToMany(mappedBy = "orderLine", cascade = { jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.REMOVE })
    private List<Lot> lots = new ArrayList<>();

    @Column(name = "is_updated")
    private Boolean updated = Boolean.FALSE;

    @ManyToOne
    private Tva tva;

    @Column(name = "date_peremption")
    private LocalDate datePeremption;

    @Transient
    private boolean isNew = true;

    public Integer getFinalStock() {
        return finalStock;
    }

    public void setFinalStock(Integer finalStock) {
        this.finalStock = finalStock;
    }

    public LocalDate getDatePeremption() {
        return datePeremption;
    }

    public void setDatePeremption(LocalDate datePeremption) {
        this.datePeremption = datePeremption;
    }

    public OrderLineId getId() {
        return new OrderLineId(id, orderDate);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getQuantityReceived() {
        return quantityReceived;
    }

    public void setQuantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public @NotNull Integer getInitStock() {
        return initStock;
    }

    public OrderLine setInitStock(Integer initStock) {
        this.initStock = initStock;
        return this;
    }

    public @NotNull Integer getQuantityRequested() {
        return quantityRequested;
    }

    public void setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    public Integer getQuantityReturned() {
        return quantityReturned;
    }

    public void setQuantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
    }

    public int getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(int discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
    }

    public Integer getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

    public @NotNull LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public @NotNull LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getReceiptDate() {
        return receiptDate;
    }

    public OrderLine setReceiptDate(LocalDateTime receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public Commande getCommande() {
        return commande;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public @NotNull Integer getOrderUnitPrice() {
        return orderUnitPrice;
    }

    public OrderLine setOrderUnitPrice(Integer orderUnitPrice) {
        this.orderUnitPrice = orderUnitPrice;
        return this;
    }

    public @NotNull Integer getOrderCostAmount() {
        return orderCostAmount;
    }

    public OrderLine setOrderCostAmount(Integer orderCostAmount) {
        this.orderCostAmount = orderCostAmount;
        return this;
    }

    public int getFreeQty() {
        return freeQty;
    }

    public OrderLine setFreeQty(int quantityUg) {
        this.freeQty = quantityUg;
        return this;
    }

    public Tva getTva() {
        return tva;
    }

    public void setTva(Tva tva) {
        this.tva = tva;
    }

    public List<Lot> getLots() {
        return lots;
    }

    public void setLots(List<Lot> lots) {
        this.lots = lots;
    }

    public Boolean getUpdated() {
        return updated;
    }

    public void setUpdated(Boolean updated) {
        this.updated = updated;
    }

    public FournisseurProduit getFournisseurProduit() {
        return fournisseurProduit;
    }

    public OrderLine setFournisseurProduit(FournisseurProduit fournisseurProduit) {
        this.fournisseurProduit = fournisseurProduit;
        return this;
    }

    public Boolean getProvisionalCode() {
        return provisionalCode;
    }

    public OrderLine setProvisionalCode(Boolean provisionalCode) {
        this.provisionalCode = provisionalCode;
        return this;
    }

    public OrderLine quantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
        return this;
    }

    public OrderLine quantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }

    public OrderLine quantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
        return this;
    }

    public OrderLine discountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public OrderLine orderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
        return this;
    }

    public OrderLine grossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
        return this;
    }

    public OrderLine netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public OrderLine taxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public OrderLine createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public OrderLine updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public OrderLine commande(Commande commande) {
        this.commande = commande;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrderLine)) {
            return false;
        }
        return id != null && id.equals(((OrderLine) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "OrderLine{"
            + "id="
            + getId()
            + ", receiptDate='"
            + ", quantityReceived="
            + getQuantityReceived()
            + ", quantityRequested="
            + getQuantityRequested()
            + ", quantityReturned="
            + getQuantityReturned()
            + ", discountAmount="
            + getDiscountAmount()
            + ", orderAmount="
            + getOrderAmount()
            + ", grossAmount="
            + getGrossAmount()
            + ", netAmount="
            + getNetAmount()
            + ", taxAmount="
            + getTaxAmount()
            + ", createdAt='"
            + getCreatedAt()
            + "'"
            + ", updatedAt='"
            + getUpdatedAt()

            + "}";
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PrePersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}

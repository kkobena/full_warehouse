package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.annotations.Formula;

/**
 * A OrderLine.
 */
@Getter
@Entity
@Table(
    name = "order_line",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"commande_id", "fournisseur_produit_id"})
    })
public class OrderLine implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quantity_received")
    private Integer quantityReceived;

    @NotNull
    @Column(name = "init_stock", nullable = false)
    private Integer initStock;

    @NotNull
    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;

    @Column(name = "quantity_returned")
    private Integer quantityReturned;

    @Column(name = "discount_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer discountAmount = 0;

    @NotNull
    @Column(name = "order_amount", nullable = false)
    private Integer orderAmount;// montant vente  commande

    @NotNull
    @Column(name = "gross_amount", nullable = false)
    private Integer grossAmount; //montant achat commande

    @Column(name = "net_amount", columnDefinition = "int default '0'")
    private Integer netAmount = 0;

    @Column(name = "tax_amount", columnDefinition = "int default '0'")
    private Integer taxAmount = 0;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "receipt_date")
    private LocalDateTime receiptDate;
    @NotNull
    @Column(name = "cost_amount", nullable = false)
    private Integer costAmount; //prix achat machine ligne de commande

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "orderLines", allowSetters = true)
    private Commande commande;


    @NotNull
    @Column(name = "order_unit_price", nullable = false)
    private Integer orderUnitPrice;  //prix uni commande

    @NotNull
    @Column(name = "regular_unit_price", nullable = false, columnDefinition = "int default '0'")
    private Integer regularUnitPrice; //prix unitaire machine

    @NotNull
    @Column(name = "order_cost_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer orderCostAmount; //prix d'achat commande

    @Formula("quantity_requested*order_cost_amount")
    private Integer effectifGrossIncome; //montant achat commande

    @Formula("quantity_requested*order_unit_price")
    private Integer effectifOrderAmount; // montant vente commande


    @Column(name = "quantity_ug", columnDefinition = "int default '0'")
    private Integer quantityUg = 0;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "orderLines", allowSetters = true)
    private FournisseurProduit fournisseurProduit;

    @Column(name = "provisional_code")
    private Boolean provisionalCode = Boolean.FALSE;

    public OrderLine setEffectifGrossIncome(Integer effectifGrossIncome) {
        this.effectifGrossIncome = effectifGrossIncome;
        return this;
    }

    public OrderLine setReceiptDate(LocalDateTime receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public OrderLine setQuantityUg(Integer quantityUg) {
        this.quantityUg = quantityUg;
        return this;
    }

    public OrderLine setEffectifOrderAmount(Integer effectifOrderAmount) {
        this.effectifOrderAmount = effectifOrderAmount;
        return this;
    }

    public OrderLine setProvisionalCode(Boolean provisionalCode) {
        this.provisionalCode = provisionalCode;
        return this;
    }

    public OrderLine setInitStock(Integer initStock) {
        this.initStock = initStock;
        return this;
    }

    public OrderLine setOrderCostAmount(Integer orderCostAmount) {
        this.orderCostAmount = orderCostAmount;
        return this;
    }

    public OrderLine setOrderUnitPrice(Integer orderUnitPrice) {
        this.orderUnitPrice = orderUnitPrice;
        return this;
    }

    public OrderLine setFournisseurProduit(FournisseurProduit fournisseurProduit) {
        this.fournisseurProduit = fournisseurProduit;
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setQuantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public OrderLine quantityReceived(Integer quantityReceived) {
        this.quantityReceived = quantityReceived;
        return this;
    }

    public void setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
    }

    public OrderLine quantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }

    public void setQuantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
    }

    public OrderLine quantityReturned(Integer quantityReturned) {
        this.quantityReturned = quantityReturned;
        return this;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public OrderLine discountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public void setOrderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
    }

    public OrderLine orderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
        return this;
    }

    public void setGrossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
    }

    public OrderLine grossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
        return this;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public OrderLine netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

    public OrderLine taxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OrderLine createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OrderLine updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public OrderLine costAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public void setCommande(Commande commande) {
        this.commande = commande;
    }

    public OrderLine commande(Commande commande) {
        this.commande = commande;
        return this;
    }



    public OrderLine setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
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
            + "'"
            + ", costAmount="
            + getCostAmount()
            + "}";
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace(System.err);
            return null;
        }
    }
}

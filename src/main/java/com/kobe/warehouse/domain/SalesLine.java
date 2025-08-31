package com.kobe.warehouse.domain;

import com.kobe.warehouse.service.sale.calculation.dto.Rate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Objects.isNull;

/**
 * A SalesLine.
 */
@Entity
@IdClass(SaleLineId.class)
@Table(name = "sales_line", uniqueConstraints = {@UniqueConstraint(columnNames = {"produit_id", "sales_id", "sale_date"})})
public class SalesLine implements Persistable<SaleLineId>, Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Transient
    private boolean isNew = true;
    @Id
    private Long id;
    @Id
    @Column(name = "sale_date")
    private LocalDate saleDate = LocalDate.now();
    @NotNull
    @Column(name = "quantity_sold", nullable = false)
    private Integer quantitySold;
    @NotNull
    @Column(name = "quantity_requested", nullable = false)
    private Integer quantityRequested;
    @NotNull
    @Column(name = "quantity_ug", nullable = false, columnDefinition = "int default '0'")
    private Integer quantityUg = 0;
    @NotNull
    @Column(name = "quantity_avoir", nullable = false, columnDefinition = "int default '0'")
    private Integer quantityAvoir = 0;
    @NotNull
    @Column(name = "regular_unit_price", nullable = false, columnDefinition = "int default '0'")
    private Integer regularUnitPrice;
    @NotNull
    @Column(name = "discount_unit_price", nullable = false, columnDefinition = "int default '0'")
    private Integer discountUnitPrice = 0;
    @NotNull
    @Column(name = "net_unit_price", nullable = false, columnDefinition = "int default '0'")
    private Integer netUnitPrice = 0;
    @NotNull
    @Column(name = "discount_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer discountAmount = 0;
    @NotNull
    @Column(name = "sales_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer salesAmount = 0;
    @NotNull
    @Column(name = "tax_value", nullable = false, columnDefinition = "int default '0'")
    private Integer taxValue = 0;
    @NotNull
    @Column(name = "cost_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer costAmount = 0;
    @Column(name = "calculation_base_price")
    private Integer calculationBasePrice;
    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @NotNull
    @ManyToOne(optional = false)
    private Sales sales;
    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Produit produit;
    @NotNull
    @Column(name = "effective_update_date", nullable = false)
    private LocalDateTime effectiveUpdateDate;
    @Column(name = "to_ignore", nullable = false)
    private boolean toIgnore = false;
    @Column(name = "amount_to_be_taken_into_account", nullable = false)
    private Integer amountToBeTakenIntoAccount;
    @Column(name = "after_stock")
    private Integer afterStock;
    @Column(name = "taux_remise")
    private float tauxRemise = 0.0f;
    @Column(name = "init_stock")
    private Integer initStock;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "lots")
    private List<LotSold> lots = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", name = "rates")
    private List<Rate> rates = new ArrayList<>();

    @Override
    public SaleLineId getId() {
        return new SaleLineId(id, saleDate);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public float getTauxRemise() {
        return tauxRemise;
    }

    public void setTauxRemise(float tauxRemise) {
        this.tauxRemise = tauxRemise;
    }

    public List<Rate> getRates() {
        return rates;
    }

//    public Long getId() {
//        return id;
//    }

    public void setRates(List<Rate> rates) {
        this.rates = rates;
    }

    public @NotNull Integer getQuantitySold() {
        return quantitySold;
    }

    public void setQuantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
    }

    public @NotNull Integer getQuantityRequested() {
        return quantityRequested;
    }

    public SalesLine setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }


    public @NotNull Integer getQuantityUg() {
        return quantityUg;
    }

    public SalesLine setQuantityUg(Integer quantityUg) {
        this.quantityUg = quantityUg;
        return this;
    }

    public @NotNull Integer getQuantityAvoir() {
        return quantityAvoir;
    }

    public SalesLine setQuantityAvoir(Integer quantiyAvoir) {
        quantityAvoir = quantiyAvoir;
        return this;
    }


    public @NotNull Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public void setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

    public @NotNull Integer getDiscountUnitPrice() {
        return discountUnitPrice;
    }

    public void setDiscountUnitPrice(Integer discountUnitPrice) {
        this.discountUnitPrice = discountUnitPrice;
    }

    public List<LotSold> getLots() {
        if (lots == null) {
            lots = new ArrayList<>();
        }
        return lots;
    }

    public SalesLine setLots(List<LotSold> lots) {
        this.lots = lots;
        return this;
    }

    public @NotNull Integer getNetUnitPrice() {
        return netUnitPrice;
    }

    public void setNetUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
    }

    public @NotNull Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }


    public @NotNull Integer getSalesAmount() {
        return salesAmount;
    }

    public void setSalesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
    }


    public @NotNull Integer getTaxValue() {
        return taxValue;
    }

    public SalesLine setTaxValue(Integer taxValue) {
        this.taxValue = taxValue;
        return this;
    }

    public @NotNull Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
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

    public @NotNull Sales getSales() {
        return sales;
    }

    public void setSales(Sales sales) {
        this.sales = sales;
    }

    public @NotNull Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public @NotNull LocalDateTime getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public SalesLine setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    public boolean isToIgnore() {
        return toIgnore;
    }

    public SalesLine setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
        return this;
    }

    public Integer getAmountToBeTakenIntoAccount() {
        if (isNull(amountToBeTakenIntoAccount)) {
            amountToBeTakenIntoAccount = salesAmount;
        }
        return amountToBeTakenIntoAccount;
    }

    public SalesLine setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }

    public Integer getAfterStock() {
        return afterStock;
    }

    public SalesLine setAfterStock(Integer afterStock) {
        this.afterStock = afterStock;
        return this;
    }

    public Integer getInitStock() {
        return initStock;
    }

    public SalesLine setInitStock(Integer initStock) {
        this.initStock = initStock;
        return this;
    }


    public SalesLine quantitySold(Integer quantitySold) {
        this.quantitySold = quantitySold;
        return this;
    }

    public SalesLine regularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
        return this;
    }

    public SalesLine discountUnitPrice(Integer discountUnitPrice) {
        this.discountUnitPrice = discountUnitPrice;
        return this;
    }

    public SalesLine netUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
        return this;
    }

    public SalesLine discountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public SalesLine salesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
        return this;
    }


    public SalesLine costAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public SalesLine createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public SalesLine updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public SalesLine sales(Sales sales) {
        this.sales = sales;
        return this;
    }

    public Integer getCalculationBasePrice() {
        return calculationBasePrice;
    }

    public void setCalculationBasePrice(Integer calculationBasePrice) {
        this.calculationBasePrice = calculationBasePrice;
    }

    public SalesLine produit(Produit produit) {
        this.produit = produit;
        return this;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SalesLine salesLine = (SalesLine) o;
        return Objects.equals(id, salesLine.id) && Objects.equals(saleDate, salesLine.saleDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, saleDate);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "SalesLine{"
            + "id="
            + getId()
            + ", quantitySold="
            + getQuantitySold()
            + ", regularUnitPrice="
            + getRegularUnitPrice()
            + ", discountUnitPrice="
            + getDiscountUnitPrice()
            + ", netUnitPrice="
            + getNetUnitPrice()
            + ", discountAmount="
            + getDiscountAmount()
            + ", salesAmount="
            + getSalesAmount()
            + ", netAmount="

            + ", costAmount="
            + getCostAmount()
            + ", createdAt='"
            + getCreatedAt()
            + "'"
            + ", updatedAt='"
            + getUpdatedAt()
            + "'"
            + "}";
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException _) {
            return null;
        }
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
}

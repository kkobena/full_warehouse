package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PaimentStatut;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Comment;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Commande.
 */
@Entity
@Table(
    name = "commande",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"receipt_reference", "fournisseur_id"})},
    indexes = {
        @Index(columnList = "order_status", name = "order_status_index"),
        @Index(columnList = "paiment_status", name = "receipt_paiment_status_index"),
        @Index(columnList = "receipt_reference", name = "receipt_reference_index"),
    }
)
public class Commande implements Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_reference", length = 20)
    private String orderReference;

    @Column(name = "receipt_reference", length = 20)
    private String receiptReference;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    @Column(name = "discount_amount", columnDefinition = "int default '0'")
    private int discountAmount;

    //   @Formula("(SELECT SUM(oi.quantity_requested*oi.order_unit_price) FROM order_line oi WHERE oi.commande_id = id)")
    @Comment("montant vente de la commande en cours de traitement")
    @Column(name = "order_amount")
    private Integer orderAmount;

    //  @Formula("(SELECT SUM((oi.quantity_received+oi.free_qty)*oi.order_unit_price) FROM order_line oi WHERE oi.commande_id = id)")
    @Comment("montant vente de la commande finalisée")
    @Column(name = "final_amount")
    private Integer finalAmount;
    @Comment("montant achat de la commande")
    @NotNull
    @Column(name = "gross_amount", nullable = false)
    private Integer grossAmount;

    @Column(name = "ht_amount", columnDefinition = "int default '0'")
    private int htAmount = 0;

    @Column(name = "tax_amount", columnDefinition = "int default '0'")
    private int taxAmount = 0;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", length = 10)
    private OrderStatut orderStatus = OrderStatut.REQUESTED;

    @OneToMany(mappedBy = "commande", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private List<OrderLine> orderLines = new ArrayList<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "paiment_status")
    private PaimentStatut paimentStatut = PaimentStatut.UNPAID;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_type")
    private TypeDeliveryReceipt type = TypeDeliveryReceipt.ORDER;

    @ManyToOne(optional = false)
    @NotNull
    private User user;

    @ManyToOne(optional = false)
    @NotNull
    private Fournisseur fournisseur;

    public Integer getFinalAmount() {
        return finalAmount;
    }

    public void setFinalAmount(Integer finalAmount) {
        this.finalAmount = finalAmount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public void setOrderReference(String orderReference) {
        this.orderReference = orderReference;
    }

    public String getReceiptReference() {
        return receiptReference;
    }

    public Commande setReceiptReference(String receiptReference) {
        this.receiptReference = receiptReference;
        return this;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public Commande setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(int discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public @NotNull Integer getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
    }

    public @NotNull Integer getGrossAmount() {
        return grossAmount;
    }

    public void setGrossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
    }

    public Integer getHtAmount() {
        return htAmount;
    }

    public void setHtAmount(int htAmount) {
        this.htAmount = htAmount;
    }

    public void setHtAmount(Integer netAmount) {
        this.htAmount = netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(int taxAmount) {
        this.taxAmount = taxAmount;
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

    public @NotNull OrderStatut getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatut orderStatus) {
        this.orderStatus = orderStatus;
    }

    public List<OrderLine> getOrderLines() {
        return orderLines;
    }

    public void setOrderLines(List<OrderLine> orderLines) {
        this.orderLines = orderLines;
    }

    public @NotNull User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public @NotNull Fournisseur getFournisseur() {
        return fournisseur;
    }

    public Commande setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
        return this;
    }

    public Commande orderRefernce(String orderRefernce) {
        this.orderReference = orderRefernce;
        return this;
    }

    public Commande discountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public Commande orderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
        return this;
    }

    public Commande grossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
        return this;
    }

    public Commande netAmount(Integer netAmount) {
        this.htAmount = netAmount;
        return this;
    }

    public Commande taxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public Commande createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Commande updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public Commande orderStatus(OrderStatut orderStatus) {
        this.orderStatus = orderStatus;
        return this;
    }

    public Commande orderLines(List<OrderLine> orderLines) {
        this.orderLines = orderLines;
        return this;
    }

    public Commande addOrderLine(OrderLine orderLine) {
        orderLines.add(orderLine);
        orderLine.setCommande(this);
        return this;
    }

    public PaimentStatut getPaimentStatut() {
        return paimentStatut;
    }

    public void setPaimentStatut(PaimentStatut paimentStatut) {
        this.paimentStatut = paimentStatut;
    }

    public TypeDeliveryReceipt getType() {
        return type;
    }

    public void setType(TypeDeliveryReceipt type) {
        this.type = type;
    }

    public Commande removeOrderLine(OrderLine orderLine) {
        orderLines.remove(orderLine);
        orderLine.setCommande(null);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Commande)) {
            return false;
        }
        return id != null && id.equals(((Commande) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Commande{"
            + "id="
            + getId()
            + ", orderRefernce='"
            + getOrderReference()
            + "'"
            + ", receiptDate='"
            + getReceiptDate()
            + "'"
            + ", discountAmount="
            + getDiscountAmount()
            + ", orderAmount="
            + getOrderAmount()
            + ", grossAmount="
            + getGrossAmount()
            + ", netAmount="
            + getHtAmount()
            + ", taxAmount="
            + getTaxAmount()
            + ", createdAt='"
            + getCreatedAt()
            + "'"
            + ", updatedAt='"
            + getUpdatedAt()
            + "'"
            + ", orderStatus='"
            + getOrderStatus()
            + "'"
            + "}";
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

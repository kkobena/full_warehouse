package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.PaimentStatut;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
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
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
    name = "delivery_receipt",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "number_transaction" }),
        @UniqueConstraint(columnNames = { "receipt_reference", "fournisseur_id" }),
    },
    indexes = {
        @Index(columnList = "receipt_date DESC", name = "receipt_date_index"),
        @Index(columnList = "receipt_status", name = "receipt_status_index"),
        @Index(columnList = "paiment_status", name = "receipt_paiment_status_index"),
        @Index(columnList = "receipt_reference", name = "receipt_reference_index"),
        @Index(columnList = "number_transaction", name = "number_transaction_index"),
    }
)
public class DeliveryReceipt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number_transaction")
    private String numberTransaction;

    @Column(name = "sequence_bon")
    private String sequenceBon;

    @Column(name = "receipt_reference")
    private String receiptReference;

    @Column(name = "order_reference")
    private String orderReference;

    @NotNull
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;

    @Column(name = "discount_amount", columnDefinition = "int default '0'")
    private Integer discountAmount = 0;

    @Column(name = "receipt_amount")
    private Integer receiptAmount;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;

    @ManyToOne(optional = false)
    private User createdUser;

    @ManyToOne(optional = false)
    private User modifiedUser;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "receipt_status")
    private ReceiptStatut receiptStatut;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "paiment_status")
    private PaimentStatut paimentStatut = PaimentStatut.UNPAID;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "receipt_type")
    private TypeDeliveryReceipt type = TypeDeliveryReceipt.ORDER;

    @OneToMany(mappedBy = "deliveryReceipt")
    private Set<PaymentFournisseur> paymentFournisseurs = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    private Fournisseur fournisseur;

    @Column(name = "net_amount", columnDefinition = "int default '0'")
    private Integer netAmount = 0;

    @Column(name = "tax_amount", columnDefinition = "int default '0'")
    private Integer taxAmount = 0;

    @OneToMany(mappedBy = "deliveryReceipt", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<DeliveryReceiptItem> receiptItems = new ArrayList<>();



    public Long getId() {
        return id;
    }

    public DeliveryReceipt setId(Long id) {
        this.id = id;
        return this;
    }

    public String getNumberTransaction() {
        return numberTransaction;
    }

    public DeliveryReceipt setNumberTransaction(String numberTransaction) {
        this.numberTransaction = numberTransaction;
        return this;
    }

    public String getSequenceBon() {
        return sequenceBon;
    }

    public DeliveryReceipt setSequenceBon(String sequenceBon) {
        this.sequenceBon = sequenceBon;
        return this;
    }

    public String getReceiptReference() {
        return receiptReference;
    }

    public DeliveryReceipt setReceiptReference(String receiptReference) {
        this.receiptReference = receiptReference;
        return this;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public DeliveryReceipt setOrderReference(String orderReference) {
        this.orderReference = orderReference;
        return this;
    }

    public @NotNull LocalDate getReceiptDate() {
        return receiptDate;
    }

    public DeliveryReceipt setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public DeliveryReceipt setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public Integer getReceiptAmount() {
        return receiptAmount;
    }

    public DeliveryReceipt setReceiptAmount(Integer receiptAmount) {
        this.receiptAmount = receiptAmount;
        return this;
    }

    public @NotNull LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public DeliveryReceipt setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
        return this;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public DeliveryReceipt setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
        return this;
    }

    public User getCreatedUser() {
        return createdUser;
    }

    public DeliveryReceipt setCreatedUser(User createdUser) {
        this.createdUser = createdUser;
        return this;
    }

    public User getModifiedUser() {
        return modifiedUser;
    }

    public DeliveryReceipt setModifiedUser(User modifiedUser) {
        this.modifiedUser = modifiedUser;
        return this;
    }

    public @NotNull ReceiptStatut getReceiptStatut() {
        return receiptStatut;
    }

    public DeliveryReceipt setReceiptStatut(ReceiptStatut receiptStatut) {
        this.receiptStatut = receiptStatut;
        return this;
    }

    public @NotNull PaimentStatut getPaimentStatut() {
        return paimentStatut;
    }

    public DeliveryReceipt setPaimentStatut(PaimentStatut paimentStatut) {
        this.paimentStatut = paimentStatut;
        return this;
    }

    public @NotNull TypeDeliveryReceipt getType() {
        return type;
    }

    public DeliveryReceipt setType(TypeDeliveryReceipt type) {
        this.type = type;
        return this;
    }

    public Set<PaymentFournisseur> getPaymentFournisseurs() {
        return paymentFournisseurs;
    }

    public DeliveryReceipt setPaymentFournisseurs(Set<PaymentFournisseur> paymentFournisseurs) {
        this.paymentFournisseurs = paymentFournisseurs;
        return this;
    }

    public @NotNull Fournisseur getFournisseur() {
        return fournisseur;
    }

    public DeliveryReceipt setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
        return this;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public DeliveryReceipt setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public DeliveryReceipt setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public List<DeliveryReceiptItem> getReceiptItems() {
        return receiptItems;
    }

    public DeliveryReceipt setReceiptItems(List<DeliveryReceiptItem> receiptItems) {
        this.receiptItems = receiptItems;
        return this;
    }


    public Long id() {
        return id;
    }

    public DeliveryReceipt addReceiptItem(DeliveryReceiptItem receiptItem) {
        receiptItems.add(receiptItem);
        receiptItem.setDeliveryReceipt(this);
        return this;
    }

    public DeliveryReceipt removeReceiptItem(DeliveryReceiptItem receiptItem) {
        receiptItems.remove(receiptItem);
        receiptItem.setDeliveryReceipt(null);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeliveryReceipt that = (DeliveryReceipt) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

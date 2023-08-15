package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Entity
@Table(
    name = "delivery_receipt",
    uniqueConstraints = {
      @UniqueConstraint(columnNames = {"number_transaction"}),
      @UniqueConstraint(columnNames = {"receipt_refernce", "fournisseur_id"})
    },
    indexes = {
      @Index(columnList = "receipt_date DESC", name = "receipt_date_index"),
      @Index(columnList = "receipt_refernce", name = "receipt_refernce_index"),
      @Index(columnList = "number_transaction", name = "number_transaction_index")
    })
public class DeliveryReceipt implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "number_transaction")
  private String numberTransaction;

  @Column(name = "sequence_bon")
  private String sequenceBon;

  @Column(name = "receipt_refernce")
  private String receiptRefernce;

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

  @OneToMany(
      mappedBy = "deliveryReceipt",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
  private List<DeliveryReceiptItem> receiptItems = new ArrayList<>();


    public DeliveryReceipt setType(TypeDeliveryReceipt type) {
    this.type = type;
    return this;
  }

    public DeliveryReceipt setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }


    public DeliveryReceipt setOrderReference(String orderReference) {
    this.orderReference = orderReference;
    return this;
  }

    public DeliveryReceipt setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  public Long id() {
    return id;
  }

    public DeliveryReceipt setNumberTransaction(String numberTransaction) {
    this.numberTransaction = numberTransaction;
    return this;
  }

    public DeliveryReceipt setSequenceBon(String sequenceBon) {
    this.sequenceBon = sequenceBon;
    return this;
  }

    public DeliveryReceipt setReceiptRefernce(String receiptRefernce) {
    this.receiptRefernce = receiptRefernce;
    return this;
  }

    public DeliveryReceipt setReceiptDate(LocalDate receiptDate) {
    this.receiptDate = receiptDate;
    return this;
  }

    public DeliveryReceipt setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

    public DeliveryReceipt setReceiptAmount(Integer receiptAmount) {
    this.receiptAmount = receiptAmount;
    return this;
  }

    public DeliveryReceipt setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

    public DeliveryReceipt setModifiedDate(LocalDateTime modifiedDate) {
    this.modifiedDate = modifiedDate;
    return this;
  }

    public DeliveryReceipt setCreatedUser(User createdUser) {
    this.createdUser = createdUser;
    return this;
  }

    public DeliveryReceipt setModifiedUser(User modifiedUser) {
    this.modifiedUser = modifiedUser;
    return this;
  }

    public DeliveryReceipt setReceiptStatut(ReceiptStatut receiptStatut) {
    this.receiptStatut = receiptStatut;
    return this;
  }

    public DeliveryReceipt setPaymentFournisseurs(Set<PaymentFournisseur> paymentFournisseurs) {
    this.paymentFournisseurs = paymentFournisseurs;
    return this;
  }

    public DeliveryReceipt setFournisseur(Fournisseur fournisseur) {
    this.fournisseur = fournisseur;
    return this;
  }

    public DeliveryReceipt setId(Long id) {
    this.id = id;
    return this;
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

    public DeliveryReceipt setReceiptItems(List<DeliveryReceiptItem> receiptItems) {
    this.receiptItems = receiptItems;
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

package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
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
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "number_transaction")
  private String numberTransaction;

  @Column(name = "sequence_bon")
  private String sequenceBon;

  @Column(name = "receipt_refernce")
  private String receiptRefernce;

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
  private ReceiptStatut ReceiptStatut;

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
  private Set<DeliveryReceiptItem> receiptItems = new HashSet<>();

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

  public Long id() {
    return id;
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

  public String getReceiptRefernce() {
    return receiptRefernce;
  }

  public DeliveryReceipt setReceiptRefernce(String receiptRefernce) {
    this.receiptRefernce = receiptRefernce;
    return this;
  }

  public LocalDate getReceiptDate() {
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

  public LocalDateTime getCreatedDate() {
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

  public com.kobe.warehouse.domain.enumeration.ReceiptStatut getReceiptStatut() {
    return ReceiptStatut;
  }

  public DeliveryReceipt setReceiptStatut(ReceiptStatut receiptStatut) {
    ReceiptStatut = receiptStatut;
    return this;
  }

  public Set<PaymentFournisseur> getPaymentFournisseurs() {
    return paymentFournisseurs;
  }

  public DeliveryReceipt setPaymentFournisseurs(Set<PaymentFournisseur> paymentFournisseurs) {
    this.paymentFournisseurs = paymentFournisseurs;
    return this;
  }

  public Fournisseur getFournisseur() {
    return fournisseur;
  }

  public DeliveryReceipt setFournisseur(Fournisseur fournisseur) {
    this.fournisseur = fournisseur;
    return this;
  }

  public Long getId() {
    return id;
  }

  public DeliveryReceipt setId(Long id) {
    this.id = id;
    return this;
  }

  public Set<DeliveryReceiptItem> getReceiptItems() {
    return receiptItems;
  }

  public DeliveryReceipt setReceiptItems(Set<DeliveryReceiptItem> receiptItems) {
    this.receiptItems = receiptItems;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DeliveryReceipt that = (DeliveryReceipt) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

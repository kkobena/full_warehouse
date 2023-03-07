package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.LotJsonValue;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

/** A Commande. */
@Entity
@Table(name = "commande")
public class Commande implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_refernce")
  private String orderRefernce;

  @Column(name = "sequence_bon")
  private String sequenceBon;

  @Column(name = "receipt_refernce")
  private String receiptRefernce;

  @Column(name = "receipt_date")
  private LocalDate receiptDate;

  @Column(name = "discount_amount", columnDefinition = "int default '0'")
  private Integer discountAmount = 0;

  @NotNull
  @Column(name = "order_amount", nullable = false)
  private Integer orderAmount;

  @NotNull
  @Column(name = "gross_amount", nullable = false)
  private Integer grossAmount;

  @Column(name = "net_amount", columnDefinition = "int default '0'")
  private Integer netAmount = 0;

  @Column(name = "tax_amount", columnDefinition = "int default '0'")
  private Integer taxAmount = 0;

  @Column(name = "receipt_amount")
  private Integer receiptAmount;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @NotNull
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "order_status")
  private OrderStatut orderStatus;

  @OneToMany(
      mappedBy = "commande",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
  private Set<OrderLine> orderLines = new HashSet<>();

  @ManyToOne(optional = false)
  @JsonIgnoreProperties(value = "commandes", allowSetters = true)
  private DateDimension dateDimension;

  @ManyToOne(optional = false)
  @NotNull
  private Magasin magasin;

  @ManyToOne(optional = false)
  @NotNull
  private User user;

  @ManyToOne(optional = false)
  @NotNull
  private User lastUserEdit;

  @Column(name = "type_suggession")
  private String typeSuggession;

  @ManyToOne(optional = false)
  @NotNull
  private Fournisseur fournisseur;

  @Type(type = "io.hypersistence.utils.hibernate.type.json.JsonType")
  @Column(columnDefinition = "json", name = "lots")
  private Set<LotJsonValue> lots = new HashSet<>();

  public Set<LotJsonValue> getLots() {
    if (lots == null) {
      lots = new HashSet<>();
    }
    return lots;
  }

  public Commande setLots(Set<LotJsonValue> lots) {
    this.lots = lots;
    return this;
  }

  public String getSequenceBon() {
    return sequenceBon;
  }

  public Commande setSequenceBon(String sequenceBon) {
    this.sequenceBon = sequenceBon;
    return this;
  }

  public String getReceiptRefernce() {
    return receiptRefernce;
  }

  public Commande setReceiptRefernce(String receiptRefernce) {
    this.receiptRefernce = receiptRefernce;
    return this;
  }

  public User getLastUserEdit() {
    return lastUserEdit;
  }

  public Commande setLastUserEdit(User lastUserEdit) {
    this.lastUserEdit = lastUserEdit;
    return this;
  }

  public Integer getReceiptAmount() {
    return receiptAmount;
  }

  public Commande setReceiptAmount(Integer receiptAmount) {
    this.receiptAmount = receiptAmount;
    return this;
  }

  public Fournisseur getFournisseur() {
    return fournisseur;
  }

  public Commande setFournisseur(Fournisseur fournisseur) {
    this.fournisseur = fournisseur;
    return this;
  }

  public String getTypeSuggession() {
    return typeSuggession;
  }

  public Commande setTypeSuggession(String typeSuggession) {
    this.typeSuggession = typeSuggession;
    return this;
  }

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Magasin getMagasin() {
    return magasin;
  }

  public void setMagasin(Magasin magasin) {
    this.magasin = magasin;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOrderRefernce() {
    return orderRefernce;
  }

  public void setOrderRefernce(String orderRefernce) {
    this.orderRefernce = orderRefernce;
  }

  public Commande orderRefernce(String orderRefernce) {
    this.orderRefernce = orderRefernce;
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

  public void setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
  }

  public Commande discountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  public Integer getOrderAmount() {
    return orderAmount;
  }

  public void setOrderAmount(Integer orderAmount) {
    this.orderAmount = orderAmount;
  }

  public Commande orderAmount(Integer orderAmount) {
    this.orderAmount = orderAmount;
    return this;
  }

  public Integer getGrossAmount() {
    return grossAmount;
  }

  public void setGrossAmount(Integer grossAmount) {
    this.grossAmount = grossAmount;
  }

  public Commande grossAmount(Integer grossAmount) {
    this.grossAmount = grossAmount;
    return this;
  }

  public Integer getNetAmount() {
    return netAmount;
  }

  public void setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
  }

  public Commande netAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  public Integer getTaxAmount() {
    return taxAmount;
  }

  public void setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
  }

  public Commande taxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Commande createdAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Commande updatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public OrderStatut getOrderStatus() {
    return orderStatus;
  }

  public void setOrderStatus(OrderStatut orderStatus) {
    this.orderStatus = orderStatus;
  }

  public Commande orderStatus(OrderStatut orderStatus) {
    this.orderStatus = orderStatus;
    return this;
  }

  public Set<OrderLine> getOrderLines() {
    return orderLines;
  }

  public void setOrderLines(Set<OrderLine> orderLines) {
    this.orderLines = orderLines;
  }

  public Commande orderLines(Set<OrderLine> orderLines) {
    this.orderLines = orderLines;
    return this;
  }

  public Commande addOrderLine(OrderLine orderLine) {
    orderLines.add(orderLine);
    orderLine.setCommande(this);
    return this;
  }

  public Commande removeOrderLine(OrderLine orderLine) {
    orderLines.remove(orderLine);
    orderLine.setCommande(null);
    return this;
  }

  public DateDimension getDateDimension() {
    return dateDimension;
  }

  public void setDateDimension(DateDimension dateDimension) {
    this.dateDimension = dateDimension;
  }

  public Commande dateDimension(DateDimension dateDimension) {
    this.dateDimension = dateDimension;
    return this;
  }
  // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

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
        + getOrderRefernce()
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
        + getNetAmount()
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
      e.printStackTrace(System.err);
      return null;
    }
  }
}

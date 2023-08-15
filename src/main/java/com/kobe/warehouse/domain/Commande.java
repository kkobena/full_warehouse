package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.LotJsonValue;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import lombok.Getter;
import org.hibernate.annotations.Type;

/** A Commande. */
@Getter
@Entity
@Table(name = "commande")
public class Commande implements Serializable, Cloneable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter
  @Column(name = "order_refernce")
  private String orderRefernce;

  @Getter
  @Column(name = "sequence_bon")
  private String sequenceBon;

  @Getter
  @Column(name = "receipt_refernce")
  private String receiptRefernce;

  @Getter
  @Column(name = "receipt_date")
  private LocalDate receiptDate;

  @Getter
  @Column(name = "discount_amount", columnDefinition = "int default '0'")
  private Integer discountAmount = 0;

  @Getter
  @NotNull
  @Column(name = "order_amount", nullable = false)
  private Integer orderAmount;

  @Getter
  @NotNull
  @Column(name = "gross_amount", nullable = false)
  private Integer grossAmount;

  @Getter
  @Column(name = "net_amount", columnDefinition = "int default '0'")
  private Integer netAmount = 0;

  @Getter
  @Column(name = "tax_amount", columnDefinition = "int default '0'")
  private Integer taxAmount = 0;

  @Getter
  @Column(name = "receipt_amount")
  private Integer receiptAmount;

  @Getter
  @NotNull
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Getter
  @NotNull
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @Getter
  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "order_status")
  private OrderStatut orderStatus;

  @Getter
  @OneToMany(
      mappedBy = "commande",
      cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
  private Set<OrderLine> orderLines = new HashSet<>();


  @Getter
  @ManyToOne(optional = false)
  @NotNull
  private Magasin magasin;

  @Getter
  @ManyToOne(optional = false)
  @NotNull
  private User user;

  @Getter
  @ManyToOne(optional = false)
  @NotNull
  private User lastUserEdit;


  @Getter

  @Enumerated(EnumType.ORDINAL)
  @Column(name = "type_suggession",length = 3)
  private TypeSuggession typeSuggession;

  @Getter
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

  public Commande setSequenceBon(String sequenceBon) {
    this.sequenceBon = sequenceBon;
    return this;
  }

  public Commande setReceiptRefernce(String receiptRefernce) {
    this.receiptRefernce = receiptRefernce;
    return this;
  }

  public Commande setLastUserEdit(User lastUserEdit) {
    this.lastUserEdit = lastUserEdit;
    return this;
  }

  public Commande setReceiptAmount(Integer receiptAmount) {
    this.receiptAmount = receiptAmount;
    return this;
  }

  public Commande setFournisseur(Fournisseur fournisseur) {
    this.fournisseur = fournisseur;
    return this;
  }

  public Commande setTypeSuggession(TypeSuggession typeSuggession) {
    this.typeSuggession = typeSuggession;
    return this;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public void setMagasin(Magasin magasin) {
    this.magasin = magasin;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setOrderRefernce(String orderRefernce) {
    this.orderRefernce = orderRefernce;
  }

  public Commande orderRefernce(String orderRefernce) {
    this.orderRefernce = orderRefernce;
    return this;
  }

  public Commande setReceiptDate(LocalDate receiptDate) {
    this.receiptDate = receiptDate;
    return this;
  }

  public void setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
  }

  public Commande discountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  public void setOrderAmount(Integer orderAmount) {
    this.orderAmount = orderAmount;
  }

  public Commande orderAmount(Integer orderAmount) {
    this.orderAmount = orderAmount;
    return this;
  }

  public void setGrossAmount(Integer grossAmount) {
    this.grossAmount = grossAmount;
  }

  public Commande grossAmount(Integer grossAmount) {
    this.grossAmount = grossAmount;
    return this;
  }

  public void setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
  }

  public Commande netAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  public void setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
  }

  public Commande taxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Commande createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Commande updatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public void setOrderStatus(OrderStatut orderStatus) {
    this.orderStatus = orderStatus;
  }

  public Commande orderStatus(OrderStatut orderStatus) {
    this.orderStatus = orderStatus;
    return this;
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

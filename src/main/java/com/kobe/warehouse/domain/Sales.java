package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;

/** A Sales. */
@Getter
@Entity
@Table(
    name = "sales",
    indexes = {
      @Index(columnList = "statut", name = "vente_statut_index"),
      @Index(columnList = "number_transaction", name = "vente_number_transaction_index"),
      @Index(columnList = "created_at", name = "vente_created_at_index"),
      @Index(columnList = "updated_at", name = "vente_updated_at_index"),
      @Index(columnList = "effective_update_date", name = "vente_effective_update_index"),
      @Index(columnList = "to_ignore", name = "vente_to_ignore_index"),
      @Index(columnList = "payment_status", name = "vente_payment_status_index"),
      @Index(columnList = "nature_vente", name = "vente_nature_vente_index")
    })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Sales implements Serializable, Cloneable {

  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotNull
  @Column(name = "number_transaction", nullable = false)
  private String numberTransaction;

  @NotNull
  @Column(name = "discount_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer discountAmount = 0;

  @NotNull
  @Column(name = "sales_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer salesAmount = 0;

  @NotNull
  @Column(name = "ht_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer htAmount = 0;

  @NotNull
  @Column(name = "net_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer netAmount = 0;

  @NotNull
  @Column(name = "tax_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer taxAmount = 0;

  @NotNull
  @Column(name = "cost_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer costAmount = 0;

  @NotNull
  @Column(name = "amount_to_be_paid", nullable = false, columnDefinition = "int default '0'")
  private Integer amountToBePaid = 0;

  @NotNull
  @Column(name = "payroll_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer payrollAmount = 0; // montant paye

  @NotNull
  @Column(name = "rest_to_pay", nullable = false, columnDefinition = "int default '0'")
  private Integer restToPay = 0;

  @Column(
      name = "amount_to_be_taken_into_account",
      nullable = false,
      columnDefinition = "int default '0'")
  private Integer amountToBeTakenIntoAccount = 0;

  @Column(name = "marge_ug")
  private Integer margeUg = 0;

  @Column(name = "montant_ttc_ug", columnDefinition = "int default '0'")
  private Integer montantttcUg = 0;

  @Column(name = "montant_net_ug", columnDefinition = "int default '0'")
  private Integer montantnetUg = 0;

  @Column(name = "montant_tva_ug", columnDefinition = "int default '0'")
  private Integer montantTvaUg = 0;

  @NotNull
  @Column(name = "discount_amount_hors_ug", nullable = false, columnDefinition = "int default '0'")
  private Integer discountAmountHorsUg = 0;

  @NotNull
  @Column(name = "discount_amount_ug", nullable = false, columnDefinition = "int default '0'")
  private Integer discountAmountUg = 0;

  @NotNull
  @Column(name = "net_ug_amount", nullable = false, columnDefinition = "int default '0'")
  private Integer netUgAmount = 0;

  @NotNull
  @Column(name = "ht_amount_ug", nullable = false, columnDefinition = "int default '0'")
  private Integer htAmountUg = 0;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "statut", nullable = false, length = 20)
  private SalesStatut statut;

  @NotNull
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @NotNull
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @OneToMany(mappedBy = "sales")
  private Set<SalesLine> salesLines = new HashSet<>();

  @ManyToOne private Remise remise;

  @NotNull
  @ManyToOne(optional = false)
  private User user;

  @NotNull
  @ManyToOne(optional = false)
  private User seller;

  @NotNull
  @ManyToOne(optional = false)
  private User cassier;

  @OneToMany(mappedBy = "sales")
  private Set<Payment> payments = new HashSet<>();

  @ManyToOne(optional = false)
  @NotNull
  private Magasin magasin;

  @ManyToOne private Sales canceledSale;

  @ManyToOne(optional = false)
  @NotNull
  private User lastUserEdit;

  @NotNull
  @Column(name = "effective_update_date", nullable = false)
  private LocalDateTime effectiveUpdateDate;

  @Column(name = "to_ignore", nullable = false, columnDefinition = "boolean default false")
  private boolean toIgnore = false;

  @Column(name = "copy", nullable = false, columnDefinition = "boolean default false")
  private Boolean copy = false;

  @Column(name = "imported", nullable = false, columnDefinition = "boolean default false")
  private boolean imported = false;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status", nullable = false, length = 15)
  private PaymentStatus paymentStatus;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "nature_vente", nullable = false, length = 15)
  private NatureVente natureVente;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "type_prescription", nullable = false, length = 15)
  private TypePrescription typePrescription;

  @NotNull
  @Column(name = "differe", nullable = false, columnDefinition = "boolean default false")
  private boolean differe = false;

  @NotNull
  @Column(name = "caisse_num", length = 100)
  private String caisseNum;

  @NotNull
  @Enumerated(EnumType.ORDINAL)
  @Column(name = "statut_caisse", nullable = false)
  private SalesStatut statutCaisse;

  @NotNull
  @Enumerated(EnumType.STRING)
  @Column(name = "ca", nullable = false, length = 30)
  private CategorieChiffreAffaire categorieChiffreAffaire = CategorieChiffreAffaire.CA;

  @NotNull
  @Column(name = "caisse_end_num", length = 100)
  private String caisseEndNum;

  @ManyToOne private Customer customer;

  @Column(name = "canceled", nullable = false, columnDefinition = "boolean default false")
  private Boolean canceled = false;

  @OneToMany(mappedBy = "sale")
  private Set<Ticket> tickets = new HashSet<>();

  @Column(length = 100)
  private String tvaEmbeded;

  @Column(name = "commentaire")
  private String commentaire;

  @NotNull
  @Column(name = "monnaie", nullable = false, columnDefinition = "int default '0'")
  private Integer monnaie = 0;

  @ManyToOne private Avoir avoir;

  public Sales setAvoir(Avoir avoir) {
    this.avoir = avoir;
    return this;
  }

  public Sales setCaisseNum(String caisseNum) {
    this.caisseNum = caisseNum;
    return this;
  }

  public Sales setMonnaie(Integer monnaie) {
    this.monnaie = monnaie;
    return this;
  }

  public Sales setTvaEmbeded(String tvaEmbeded) {
    this.tvaEmbeded = tvaEmbeded;
    return this;
  }

  public Sales setLastUserEdit(User lastUserEdit) {
    this.lastUserEdit = lastUserEdit;
    return this;
  }

  public Sales setCategorieChiffreAffaire(CategorieChiffreAffaire categorieChiffreAffaire) {
    this.categorieChiffreAffaire = categorieChiffreAffaire;
    return this;
  }

  public Sales setCanceled(Boolean canceled) {
    this.canceled = canceled;
    return this;
  }

  public Sales setTickets(Set<Ticket> tickets) {
    this.tickets = tickets;
    return this;
  }

  public Sales setCaisseEndNum(String caisseEndNum) {
    this.caisseEndNum = caisseEndNum;
    return this;
  }

  public Sales setCustomer(Customer customer) {
    this.customer = customer;
    return this;
  }

  public Sales setStatutCaisse(SalesStatut statutCaisse) {
    this.statutCaisse = statutCaisse;
    return this;
  }

  public Sales setNetUgAmount(Integer netUgAmount) {
    this.netUgAmount = netUgAmount;
    return this;
  }

  public Sales setDiscountAmountHorsUg(Integer discountAmountHorsUg) {
    this.discountAmountHorsUg = discountAmountHorsUg;
    return this;
  }

  public Sales setDiscountAmountUg(Integer discountAmountUg) {
    this.discountAmountUg = discountAmountUg;
    return this;
  }

  public Sales setCommentaire(String commentaire) {
    this.commentaire = commentaire;
    return this;
  }

  public Sales setDiffere(boolean differe) {
    this.differe = differe;
    return this;
  }

  public Sales setHtAmountUg(Integer htAmountUg) {
    this.htAmountUg = htAmountUg;
    return this;
  }

  public Sales setCassier(User cassier) {
    this.cassier = cassier;
    return this;
  }

  public Sales setNatureVente(NatureVente natureVente) {
    this.natureVente = natureVente;
    return this;
  }

  public Sales setTypePrescription(TypePrescription typePrescription) {
    this.typePrescription = typePrescription;
    return this;
  }

  public Sales setPaymentStatus(PaymentStatus paymentStatus) {
    this.paymentStatus = paymentStatus;
    return this;
  }

  public Sales setCopy(Boolean copy) {
    this.copy = copy;
    return this;
  }

  public Sales setImported(boolean imported) {
    this.imported = imported;
    return this;
  }

  public Sales setMargeUg(Integer margeUg) {
    this.margeUg = margeUg;
    return this;
  }

  public Sales setMontantttcUg(Integer montantttcUg) {
    this.montantttcUg = montantttcUg;
    return this;
  }

  public Sales setMontantnetUg(Integer montantnetUg) {
    this.montantnetUg = montantnetUg;
    return this;
  }

  public Sales setMontantTvaUg(Integer montantTvaUg) {
    this.montantTvaUg = montantTvaUg;
    return this;
  }

  public Sales setRestToPay(Integer restToPay) {
    this.restToPay = restToPay;
    return this;
  }

  public void setNumberTransaction(String numberTransaction) {
    this.numberTransaction = numberTransaction;
  }

  public Sales setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
    this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
    return this;
  }

  public Sales setToIgnore(boolean toIgnore) {
    this.toIgnore = toIgnore;
    return this;
  }

  public Sales setCanceledSale(Sales canceledSale) {
    this.canceledSale = canceledSale;
    return this;
  }

  public void setMagasin(Magasin magasin) {
    this.magasin = magasin;
  }

  public void setPayments(Set<Payment> payments) {
    this.payments = payments;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public void setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
  }

  public Sales discountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  public Sales setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
    this.effectiveUpdateDate = effectiveUpdateDate;
    return this;
  }

  public Sales setAmountToBePaid(Integer amountToBePaid) {
    this.amountToBePaid = amountToBePaid;
    return this;
  }

  public Sales setPayrollAmount(Integer payrollAmount) {
    this.payrollAmount = payrollAmount;
    return this;
  }

  public void setRemise(Remise remise) {
    this.remise = remise;
  }

  public void setSalesAmount(Integer salesAmount) {
    this.salesAmount = salesAmount;
  }

  public Sales salesAmount(Integer salesAmount) {
    this.salesAmount = salesAmount;
    return this;
  }

  public Sales setHtAmount(Integer htAmount) {
    this.htAmount = htAmount;
    return this;
  }

  public void setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
  }

  public Sales netAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  public void setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
  }

  public Sales taxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  public void setCostAmount(Integer costAmount) {
    this.costAmount = costAmount;
  }

  public Sales costAmount(Integer costAmount) {
    this.costAmount = costAmount;
    return this;
  }

  public void setStatut(SalesStatut statut) {
    this.statut = statut;
  }

  public Sales statut(SalesStatut statut) {
    this.statut = statut;
    return this;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Sales createdAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Sales updatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public void setSalesLines(Set<SalesLine> salesLines) {
    this.salesLines = salesLines;
  }

  public Sales salesLines(Set<SalesLine> salesLines) {
    this.salesLines = salesLines;
    return this;
  }

  public Sales addSalesLine(SalesLine salesLine) {
    salesLines.add(salesLine);
    salesLine.setSales(this);
    return this;
  }

  public Sales removeSalesLine(SalesLine salesLine) {
    salesLines.remove(salesLine);
    salesLine.setSales(null);
    return this;
  }

  public Sales setSeller(User seller) {
    this.seller = seller;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Sales)) {
      return false;
    }
    return id != null && id.equals(((Sales) o).id);
  }

  @Override
  public int hashCode() {
    return 31;
  }

  @Override
  public String toString() {
    return "Sales{" + "id=" + id + '}';
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

package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Persistable;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A Sales.
 */
@Entity
@Table(
    name = "sales",
    indexes = {
        @Index(columnList = "dtype", name = "vente_dtype_index"),
        @Index(columnList = "sale_date", name = "vente_sale_date_index"),
        @Index(columnList = "statut", name = "vente_statut_index"),
        @Index(columnList = "number_transaction", name = "vente_number_transaction_index"),
        @Index(columnList = "ca", name = "vente_ca_index"),
        @Index(columnList = "imported", name = "vente_imported_index"),
        @Index(columnList = "to_ignore", name = "vente_to_ignore_index"),
        @Index(columnList = "payment_status", name = "vente_payment_status_index"),
        @Index(columnList = "nature_vente", name = "vente_nature_vente_index"),
    }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@IdClass(SaleId.class)
public class Sales implements Persistable<SaleId>, Serializable, Cloneable {

    @Serial
    private static final long serialVersionUID = 1L;
    @Transient
    private boolean isNew = true;
    @Id
    private Long id;
    @Id
    @Column(name = "sale_date")
    private LocalDate saleDate = LocalDate.now();

    @Column(name = "dtype", insertable = false, updatable = false)
    private String type;

    @NotNull
    @Column(name = "number_transaction", nullable = false)
    private String numberTransaction;

    @NotNull
    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount = 0;

    @NotNull
    @Column(name = "sales_amount", nullable = false)
    private Integer salesAmount = 0;

    @Transient
    private Integer htAmount = 0;

    @Transient
    private Integer netAmount = 0;

    @Transient
    private Integer taxAmount = 0;

    @Transient
    private Integer costAmount = 0;

    @NotNull
    @Column(name = "amount_to_be_paid", nullable = false)
    private Integer amountToBePaid = 0;

    @NotNull
    @Column(name = "payroll_amount", nullable = false)
    private Integer payrollAmount = 0; // montant paye

    @NotNull
    @Column(name = "rest_to_pay", nullable = false)
    private Integer restToPay = 0;

    @Column(name = "amount_to_be_taken_into_account", nullable = false)
    private Integer amountToBeTakenIntoAccount;


    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private SalesStatut statut;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "sales")
    private Set<SalesLine> salesLines = new HashSet<>();
    @ManyToOne
    private Remise remise;
    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser user;
    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser seller;
    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private AppUser caissier;
    @OneToMany(mappedBy = "sale", fetch = FetchType.LAZY)
    private Set<SalePayment> payments = new HashSet<>();
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private Magasin magasin;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "canceled_sale_id", referencedColumnName = "id"),
        @JoinColumn(name = "canceled_sale_date", referencedColumnName = "sale_date")
    })
    private Sales canceledSale;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    private AppUser lastUserEdit;
    @NotNull
    @Column(name = "effective_update_date", nullable = false)
    private LocalDateTime effectiveUpdateDate = LocalDateTime.now();
    @Column(name = "to_ignore", nullable = false)
    private boolean toIgnore = false;
    @Column(name = "copy", nullable = false)
    private Boolean copy = false;
    @Column(name = "imported", nullable = false)
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
    @Column(name = "origine_vente", nullable = false)
    private OrigineVente origineVente;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_prescription", nullable = false, length = 15)
    private TypePrescription typePrescription;
    @NotNull
    @Column(name = "differe", nullable = false)
    private boolean differe = false;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ca", nullable = false)
    private CategorieChiffreAffaire categorieChiffreAffaire = CategorieChiffreAffaire.CA;
    @ManyToOne(fetch = FetchType.LAZY)
    private Poste caisse;
    @ManyToOne(fetch = FetchType.LAZY)
    private Poste lastCaisse;
    @ManyToOne
    private Customer customer;
    @Column(name = "canceled", nullable = false)
    private Boolean canceled = false;
    @Column(length = 100)
    private String tvaEmbeded;
    @Column(name = "commentaire")
    private String commentaire;
    @NotNull
    @Column(name = "monnaie", nullable = false)
    private Integer monnaie = 0;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_register_id", referencedColumnName = "id")
    private CashRegister cashRegister;


    public SaleId getId() {
        return new SaleId(id, saleDate);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull String getNumberTransaction() {
        return numberTransaction;
    }

    public void setNumberTransaction(String numberTransaction) {
        this.numberTransaction = numberTransaction;
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

    public @NotNull Integer getHtAmount() {
        return htAmount;
    }

    public Sales setHtAmount(Integer htAmount) {
        this.htAmount = htAmount;
        return this;
    }

    public @NotNull Integer getNetAmount() {
        netAmount = Objects.requireNonNullElse(salesAmount, 0) - Objects.requireNonNullElse(discountAmount, 0);
        return netAmount;

    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public @NotNull Integer getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

    public @NotNull Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public @NotNull Integer getAmountToBePaid() {
        return amountToBePaid;
    }

    public Sales setAmountToBePaid(Integer amountToBePaid) {
        this.amountToBePaid = amountToBePaid;
        return this;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public @NotNull Integer getPayrollAmount() {
        return payrollAmount;
    }

    public Sales setPayrollAmount(Integer payrollAmount) {
        this.payrollAmount = payrollAmount;
        return this;
    }

    public @NotNull Integer getRestToPay() {
        return restToPay;
    }

    public Sales setRestToPay(Integer restToPay) {
        this.restToPay = restToPay;
        return this;
    }

    public Integer getAmountToBeTakenIntoAccount() {
        if (amountToBeTakenIntoAccount == null) {
            amountToBeTakenIntoAccount = salesAmount;
        }
        return amountToBeTakenIntoAccount;
    }

    public Sales setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }


    public @NotNull SalesStatut getStatut() {
        return statut;
    }

    public void setStatut(SalesStatut statut) {
        this.statut = statut;
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

    public Set<SalesLine> getSalesLines() {
        return salesLines;
    }

    public void setSalesLines(Set<SalesLine> salesLines) {
        this.salesLines = salesLines;
    }

    public Remise getRemise() {
        return remise;
    }

    public void setRemise(Remise remise) {
        this.remise = remise;
    }

    public @NotNull AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public @NotNull AppUser getSeller() {
        return seller;
    }

    public Sales setSeller(AppUser seller) {
        this.seller = seller;
        return this;
    }

    public @NotNull AppUser getCaissier() {
        return caissier;
    }

    public Sales setCaissier(AppUser caissier) {
        this.caissier = caissier;
        return this;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<SalePayment> getPayments() {
        return payments;
    }

    public void setPayments(Set<SalePayment> payments) {
        this.payments = payments;
    }

    public @NotNull Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public Sales getCanceledSale() {
        return canceledSale;
    }

    public Sales setCanceledSale(Sales canceledSale) {
        this.canceledSale = canceledSale;
        return this;
    }

    public @NotNull AppUser getLastUserEdit() {
        return lastUserEdit;
    }

    public Sales setLastUserEdit(AppUser lastUserEdit) {
        this.lastUserEdit = lastUserEdit;
        return this;
    }

    public @NotNull LocalDateTime getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public Sales setEffectiveUpdateDate(LocalDateTime effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    public boolean isToIgnore() {
        return toIgnore;
    }

    public Sales setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
        return this;
    }

    public Boolean getCopy() {
        return copy;
    }

    public Sales setCopy(Boolean copy) {
        this.copy = copy;
        return this;
    }

    public boolean isImported() {
        return imported;
    }

    public Sales setImported(boolean imported) {
        this.imported = imported;
        return this;
    }

    public @NotNull PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Sales setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
        return this;
    }

    public @NotNull NatureVente getNatureVente() {
        return natureVente;
    }

    public Sales setNatureVente(NatureVente natureVente) {
        this.natureVente = natureVente;
        return this;
    }

    public @NotNull OrigineVente getOrigineVente() {
        return origineVente;
    }

    public Sales setOrigineVente(OrigineVente origineVente) {
        this.origineVente = origineVente;
        return this;
    }

    public @NotNull TypePrescription getTypePrescription() {
        return typePrescription;
    }

    public Sales setTypePrescription(TypePrescription typePrescription) {
        this.typePrescription = typePrescription;
        return this;
    }

    @NotNull
    public boolean isDiffere() {
        return differe;
    }

    public Sales setDiffere(boolean differe) {
        this.differe = differe;
        return this;
    }

    public @NotNull CategorieChiffreAffaire getCategorieChiffreAffaire() {
        return categorieChiffreAffaire;
    }

    public Sales setCategorieChiffreAffaire(CategorieChiffreAffaire categorieChiffreAffaire) {
        this.categorieChiffreAffaire = categorieChiffreAffaire;
        return this;
    }

    public Poste getCaisse() {
        return caisse;
    }

    public Sales setCaisse(Poste caisse) {
        this.caisse = caisse;
        return this;
    }

    public Poste getLastCaisse() {
        return lastCaisse;
    }

    public Sales setLastCaisse(Poste lastCaisse) {
        this.lastCaisse = lastCaisse;
        return this;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Sales setCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public Boolean getCanceled() {
        return canceled;
    }

    public Sales setCanceled(Boolean canceled) {
        this.canceled = canceled;
        return this;
    }

    public String getTvaEmbeded() {
        return tvaEmbeded;
    }

    public Sales setTvaEmbeded(String tvaEmbeded) {
        this.tvaEmbeded = tvaEmbeded;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public Sales setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public @NotNull Integer getMonnaie() {
        return monnaie;
    }

    public Sales setMonnaie(Integer monnaie) {
        if (Objects.nonNull(monnaie) && monnaie < 0) {
            monnaie = 0;
        }
        this.monnaie = monnaie;
        return this;
    }


    public CashRegister getCashRegister() {
        return cashRegister;
    }

    public Sales setCashRegister(CashRegister cashRegister) {
        this.cashRegister = cashRegister;
        return this;
    }


    public Sales discountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public Sales salesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
        return this;
    }

    public Sales netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public Sales taxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public Sales costAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public Sales statut(SalesStatut statut) {
        this.statut = statut;
        return this;
    }

    public Sales createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Sales updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Sales sales = (Sales) o;
        return Objects.equals(id, sales.id) && Objects.equals(saleDate, sales.saleDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, saleDate);
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

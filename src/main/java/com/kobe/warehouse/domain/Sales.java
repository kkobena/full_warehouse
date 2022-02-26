package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.kobe.warehouse.domain.enumeration.*;

/**
 * A Sales.
 */
@Entity
@Table(name = "sales"
    , indexes = {
    @Index(columnList = "statut", name = "vente_statut_index"),
    @Index(columnList = "number_transaction", name = "vente_number_transaction_index"),
    @Index(columnList = "created_at", name = "vente_created_at_index"),
    @Index(columnList = "updated_at", name = "vente_updated_at_index"),
    @Index(columnList = "effective_update_date", name = "vente_effective_update_index"),
    @Index(columnList = "to_ignore", name = "vente_to_ignore_index"),
    @Index(columnList = "payment_status", name = "vente_payment_status_index"),
    @Index(columnList = "nature_vente", name = "vente_nature_vente_index")
}
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Sales implements Serializable , Cloneable{
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(name = "number_transaction", nullable = false)
    private String numberTransaction;
    @NotNull
    @Column(name = "discount_amount", nullable = false,columnDefinition = "int default '0'")
    private Integer discountAmount=0;
    @NotNull
    @Column(name = "sales_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer salesAmount = 0;
    @NotNull
    @Column(name = "ht_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer htAmount = 0;//montant net
    @NotNull
    @Column(name = "net_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer netAmount = 0;
    @NotNull
    @Column(name = "tax_amount", nullable = false,columnDefinition = "int default '0'")
    private Integer taxAmount = 0;
    @NotNull
    @Column(name = "cost_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer costAmount = 0;
    @NotNull
    @Column(name = "amount_to_be_paid", nullable = false, columnDefinition = "int default '0'")
    private Integer amountToBePaid=0;
    @NotNull
    @Column(name = "payroll_amount", nullable = false, columnDefinition = "int default '0'")
    private Integer payrollAmount = 0;//montant paye
    @NotNull
    @Column(name = "rest_to_pay", nullable = false, columnDefinition = "int default '0'")
    private Integer restToPay = 0;
    @Column(name = "amount_to_be_taken_into_account", nullable = false, columnDefinition = "int default '0'")
    private Integer amountToBeTakenIntoAccount=0;
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
    @Column(name = "statut", nullable = false)
    private SalesStatut statut;
    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @NotNull
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @OneToMany(mappedBy = "sales")
    private Set<SalesLine> salesLines = new HashSet<>();
    @ManyToOne
    private Remise remise;
    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "sales", allowSetters = true)
    private DateDimension dateDimension;
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
    @ManyToOne
    private Sales canceledSale;
    @NotNull
    @Column(name = "effective_update_date", nullable = false)
    private Instant effectiveUpdateDate;
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
    @Column(name = "caisse_num",  length = 100)
    private String caisseNum;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "statut_caisse", nullable = false)
    private SalesStatut statutCaisse;
    @NotNull
    @Column(name = "caisse_end_num",  length = 100)
    private String caisseEndNum;
    @ManyToOne
    private Customer customer;
    @Column(name = "canceled", nullable = false, columnDefinition = "boolean default false")
    private Boolean canceled = false;
    public String getCaisseNum() {
        return caisseNum;
    }
    @OneToMany(mappedBy = "sale")
    private Set<Ticket> tickets = new HashSet<>();
    @Column(length = 100)
    private String tvaEmbeded;

    public String getTvaEmbeded() {
        return tvaEmbeded;
    }

    public Sales setTvaEmbeded(String tvaEmbeded) {
        this.tvaEmbeded = tvaEmbeded;
        return this;
    }

    public Boolean getCanceled() {
        return canceled;
    }

    public Set<Ticket> getTickets() {
        return tickets;
    }

    public Sales setTickets(Set<Ticket> tickets) {
        this.tickets = tickets;
        return this;
    }

    public Sales setCanceled(Boolean canceled) {
        this.canceled = canceled;
        return this;
    }

    public String getCaisseEndNum() {
        return caisseEndNum;
    }

    public Sales setCaisseEndNum(String caisseEndNum) {
        this.caisseEndNum = caisseEndNum;
        return this;
    }

    public Sales setCaisseNum(String caisseNum) {
        this.caisseNum = caisseNum;
        return this;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Sales setCustomer(Customer customer) {
        this.customer = customer;
        return this;
    }

    public SalesStatut getStatutCaisse() {
        return statutCaisse;
    }

    public Sales setStatutCaisse(SalesStatut statutCaisse) {
        this.statutCaisse = statutCaisse;
        return this;
    }

    public Integer getNetUgAmount() {
        return netUgAmount;
    }

    public Sales setNetUgAmount(Integer netUgAmount) {
        this.netUgAmount = netUgAmount;
        return this;
    }

    public Integer getDiscountAmountHorsUg() {
        return discountAmountHorsUg;
    }

    public Sales setDiscountAmountHorsUg(Integer discountAmountHorsUg) {
        this.discountAmountHorsUg = discountAmountHorsUg;
        return this;
    }

    public Integer getDiscountAmountUg() {
        return discountAmountUg;
    }

    public Sales setDiscountAmountUg(Integer discountAmountUg) {
        this.discountAmountUg = discountAmountUg;
        return this;
    }

    public boolean isDiffere() {
        return differe;
    }

    public Sales setDiffere(boolean differe) {
        this.differe = differe;
        return this;
    }

    public Integer getHtAmountUg() {
        return htAmountUg;
    }

    public Sales setHtAmountUg(Integer htAmountUg) {
        this.htAmountUg = htAmountUg;
        return this;
    }

    public User getCassier() {
        return cassier;
    }

    public Sales setCassier(User cassier) {
        this.cassier = cassier;
        return this;
    }

    public NatureVente getNatureVente() {
        return natureVente;
    }

    public Sales setNatureVente(NatureVente natureVente) {
        this.natureVente = natureVente;
        return this;
    }

    public TypePrescription getTypePrescription() {
        return typePrescription;
    }

    public Sales setTypePrescription(TypePrescription typePrescription) {
        this.typePrescription = typePrescription;
        return this;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public Sales setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
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

    public Integer getMargeUg() {
        return margeUg;
    }

    public Sales setMargeUg(Integer margeUg) {
        this.margeUg = margeUg;
        return this;
    }

    public Integer getMontantttcUg() {
        return montantttcUg;
    }

    public Sales setMontantttcUg(Integer montantttcUg) {
        this.montantttcUg = montantttcUg;
        return this;
    }

    public Integer getMontantnetUg() {
        return montantnetUg;
    }

    public Sales setMontantnetUg(Integer montantnetUg) {
        this.montantnetUg = montantnetUg;
        return this;
    }

    public Integer getMontantTvaUg() {
        return montantTvaUg;
    }

    public Sales setMontantTvaUg(Integer montantTvaUg) {
        this.montantTvaUg = montantTvaUg;
        return this;
    }


    public Integer getRestToPay() {
        return restToPay;
    }

    public Sales setRestToPay(Integer restToPay) {
        this.restToPay = restToPay;
        return this;
    }

    public String getNumberTransaction() {
        return numberTransaction;
    }

    public Integer getAmountToBeTakenIntoAccount() {
        return amountToBeTakenIntoAccount;
    }

    public Sales setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }

    public boolean isToIgnore() {
        return toIgnore;
    }

    public Sales setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
        return this;
    }


    public Sales getCanceledSale() {
        return canceledSale;
    }

    public Sales setCanceledSale(Sales canceledSale) {
        this.canceledSale = canceledSale;
        return this;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public void setNumberTransaction(String numberTransaction) {
        this.numberTransaction = numberTransaction;
    }

    public Set<Payment> getPayments() {
        return payments;
    }

    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public Sales discountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public Instant getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public Sales setEffectiveUpdateDate(Instant effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    public Integer getAmountToBePaid() {
        return amountToBePaid;
    }

    public Sales setAmountToBePaid(Integer amountToBePaid) {
        this.amountToBePaid = amountToBePaid;
        return this;
    }

    public Integer getPayrollAmount() {
        return payrollAmount;
    }

    public Sales setPayrollAmount(Integer payrollAmount) {
        this.payrollAmount = payrollAmount;
        return this;
    }

    public Remise getRemise() {
        return remise;
    }

    public void setRemise(Remise remise) {
        this.remise = remise;
    }

    public void setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getSalesAmount() {
        return salesAmount;
    }

    public Sales salesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
        return this;
    }

    public void setSalesAmount(Integer salesAmount) {
        this.salesAmount = salesAmount;
    }

    public Integer getHtAmount() {
        return htAmount;
    }

    public Sales setHtAmount(Integer htAmount) {
        this.htAmount = htAmount;
        return this;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public Sales netAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public void setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public Sales taxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public void setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public Sales costAmount(Integer costAmount) {
        this.costAmount = costAmount;
        return this;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public SalesStatut getStatut() {
        return statut;
    }

    public Sales statut(SalesStatut statut) {
        this.statut = statut;
        return this;
    }

    public void setStatut(SalesStatut statut) {
        this.statut = statut;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Sales createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Sales updatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<SalesLine> getSalesLines() {
        return salesLines;
    }

    public Sales salesLines(Set<SalesLine> salesLines) {
        this.salesLines = salesLines;
        return this;
    }

    public Sales addSalesLine(SalesLine salesLine) {
        this.salesLines.add(salesLine);
        salesLine.setSales(this);
        return this;
    }

    public Sales removeSalesLine(SalesLine salesLine) {
        this.salesLines.remove(salesLine);
        salesLine.setSales(null);
        return this;
    }

    public void setSalesLines(Set<SalesLine> salesLines) {
        this.salesLines = salesLines;
    }


    public DateDimension getDateDimension() {
        return dateDimension;
    }

    public Sales dateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
        return this;
    }

    public User getSeller() {
        return seller;
    }

    public Sales setSeller(User seller) {
        this.seller = seller;
        return this;
    }

    public void setDateDimension(DateDimension dateDimension) {
        this.dateDimension = dateDimension;
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
        final StringBuffer sb = new StringBuffer("Sales{");
        sb.append("id=").append(id);
        sb.append(", numberTransaction='").append(numberTransaction).append('\'');
        sb.append(", discountAmount=").append(discountAmount);
        sb.append(", salesAmount=").append(salesAmount);
        sb.append(", htAmount=").append(htAmount);
        sb.append(", netAmount=").append(netAmount);
        sb.append(", taxAmount=").append(taxAmount);
        sb.append(", costAmount=").append(costAmount);
        sb.append(", amountToBePaid=").append(amountToBePaid);
        sb.append(", payrollAmount=").append(payrollAmount);
        sb.append(", restToPay=").append(restToPay);
        sb.append(", amountToBeTakenIntoAccount=").append(amountToBeTakenIntoAccount);
        sb.append(", margeUg=").append(margeUg);
        sb.append(", montantttcUg=").append(montantttcUg);
        sb.append(", montantnetUg=").append(montantnetUg);
        sb.append(", montantTvaUg=").append(montantTvaUg);
        sb.append(", discountAmountHorsUg=").append(discountAmountHorsUg);
        sb.append(", discountAmountUg=").append(discountAmountUg);
        sb.append(", netUgAmount=").append(netUgAmount);
        sb.append(", htAmountUg=").append(htAmountUg);
        sb.append(", statut=").append(statut);
        sb.append(", createdAt=").append(createdAt);
        sb.append(", updatedAt=").append(updatedAt);
        sb.append(", remise=").append(remise);
        sb.append(", dateDimension=").append(dateDimension);
        sb.append(", effectiveUpdateDate=").append(effectiveUpdateDate);
        sb.append(", toIgnore=").append(toIgnore);
        sb.append(", copy=").append(copy);
        sb.append(", imported=").append(imported);
        sb.append(", paymentStatus=").append(paymentStatus);
        sb.append(", natureVente=").append(natureVente);
        sb.append(", typePrescription=").append(typePrescription);
        sb.append(", differe=").append(differe);
        sb.append(", caisseNum='").append(caisseNum).append('\'');
        sb.append(", statutCaisse=").append(statutCaisse);
        sb.append(", caisseEndNum='").append(caisseEndNum).append('\'');
        sb.append('}');
        return sb.toString();
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

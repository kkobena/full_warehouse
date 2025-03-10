package com.kobe.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A InvoicePayment.
 */

@Entity
@Table(name = "invoice_payment")
public class InvoicePayment implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "montant_attendu", nullable = false)
    private Integer amount;

    @NotNull
    @Column(name = "montant_paye", nullable = false)
    private Integer paidAmount;

    @NotNull
    @Column(name = "created", nullable = false)
    private LocalDateTime created = LocalDateTime.now();

    @NotNull
    @ManyToOne(optional = false)
    private FactureTiersPayant factureTiersPayant;

    @ManyToOne
    private PaymentMode paymentMode;

    @ManyToOne(optional = false)
    @NotNull
    private CashRegister cashRegister;

    @Column(name = "montant_verse")
    private Integer montantVerse;

    @OneToMany(mappedBy = "invoicePayment", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<InvoicePaymentItem> invoicePaymentItems = new ArrayList<>();

    @ManyToOne
    private InvoicePayment parent;

    @ManyToOne
    private Banque banque;

    private Boolean grouped = false;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate = LocalDate.now();

    @OneToMany(mappedBy = "parent", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<InvoicePayment> invoicePayments = new ArrayList<>();

    public Boolean getGrouped() {
        return grouped;
    }

    public InvoicePayment setGrouped(Boolean grouped) {
        this.grouped = grouped;
        return this;
    }

    public Banque getBanque() {
        return banque;
    }

    public InvoicePayment setBanque(Banque banque) {
        this.banque = banque;
        return this;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public InvoicePayment setInvoiceDate(LocalDate invoiceDate) {
        this.invoiceDate = invoiceDate;
        return this;
    }

    public Integer getMontantVerse() {
        return montantVerse;
    }

    public InvoicePayment setMontantVerse(Integer montantVerse) {
        this.montantVerse = montantVerse;
        return this;
    }

    public List<InvoicePayment> getInvoicePayments() {
        return invoicePayments;
    }

    public InvoicePayment setInvoicePayments(List<InvoicePayment> invoicePayments) {
        this.invoicePayments = invoicePayments;
        return this;
    }

    public InvoicePayment getParent() {
        return parent;
    }

    public InvoicePayment setParent(InvoicePayment parent) {
        this.parent = parent;
        return this;
    }

    public @NotNull Integer getAmount() {
        return amount;
    }

    public InvoicePayment setAmount(@NotNull Integer amount) {
        this.amount = amount;
        return this;
    }

    public @NotNull CashRegister getCashRegister() {
        return cashRegister;
    }

    public InvoicePayment setCashRegister(@NotNull CashRegister cashRegister) {
        this.cashRegister = cashRegister;
        return this;
    }

    public @NotNull LocalDateTime getCreated() {
        return created;
    }

    public InvoicePayment setCreated(@NotNull LocalDateTime created) {
        this.created = created;
        return this;
    }

    public @NotNull FactureTiersPayant getFactureTiersPayant() {
        return factureTiersPayant;
    }

    public InvoicePayment setFactureTiersPayant(@NotNull FactureTiersPayant factureTiersPayant) {
        this.factureTiersPayant = factureTiersPayant;
        return this;
    }

    public Long getId() {
        return id;
    }

    public InvoicePayment setId(Long id) {
        this.id = id;
        return this;
    }

    public List<InvoicePaymentItem> getInvoicePaymentItems() {
        return invoicePaymentItems;
    }

    public InvoicePayment setInvoicePaymentItems(List<InvoicePaymentItem> invoicePaymentItems) {
        this.invoicePaymentItems = invoicePaymentItems;
        return this;
    }

    public @NotNull Integer getPaidAmount() {
        return paidAmount;
    }

    public InvoicePayment setPaidAmount(@NotNull Integer paidAmount) {
        this.paidAmount = paidAmount;
        return this;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public InvoicePayment setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
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
        InvoicePayment that = (InvoicePayment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}

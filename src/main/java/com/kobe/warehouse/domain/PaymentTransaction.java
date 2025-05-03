package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A PaymentTransaction.
 */
@Entity
@Table(name = "payment_transaction", indexes = { @Index(columnList = "categorie_ca", name = "pt_categorie_ca_id_index") })
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
public class PaymentTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "expected_amount", nullable = false)
    private Integer expectedAmount;

    @NotNull
    @Column(name = "paid_amount", nullable = false)
    private Integer paidAmount;

    @NotNull
    @Column(name = "reel_amount", nullable = false)
    private Integer reelAmount;

    @Column(name = "montant_verse", nullable = false)
    private Integer montantVerse = 0;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt= LocalDateTime.now();

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "payments", allowSetters = true)
    private PaymentMode paymentMode;

    @NotNull
    @ManyToOne(optional = false)
    private CashRegister cashRegister;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "categorie_ca", nullable = false)
    private CategorieChiffreAffaire categorieChiffreAffaire = CategorieChiffreAffaire.CA;

    @Column(name = "transaction_date", nullable = false)
    @NotNull
    private LocalDate transactionDate = LocalDate.now();

    private boolean credit;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "type_transaction", nullable = false)
    private TypeFinancialTransaction typeFinancialTransaction;

    private String commentaire;

    @Column(name = "dtype", insertable = false, updatable = false)
    private String type;

    public Integer getExpectedAmount() {
        return expectedAmount;
    }

    public PaymentTransaction setExpectedAmount(Integer expectedAmount) {
        this.expectedAmount = expectedAmount;
        return this;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPaidAmount() {
        return paidAmount;
    }

    public PaymentTransaction setPaidAmount(Integer paidAmount) {
        this.paidAmount = paidAmount;
        return this;
    }

    public Integer getReelAmount() {
        return reelAmount;
    }

    public PaymentTransaction setReelAmount(Integer reelAmount) {
        this.reelAmount = reelAmount;
        return this;
    }

    public Integer getMontantVerse() {
        return montantVerse;
    }

    public PaymentTransaction setMontantVerse(Integer montantVerse) {
        this.montantVerse = montantVerse;
        return this;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public PaymentTransaction setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public PaymentTransaction setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public boolean isCredit() {
        return credit;
    }

    public PaymentTransaction setCredit(boolean credit) {
        this.credit = credit;
        return this;
    }

    public TypeFinancialTransaction getTypeFinancialTransaction() {
        return typeFinancialTransaction;
    }

    public PaymentTransaction setTypeFinancialTransaction(TypeFinancialTransaction typeFinancialTransaction) {
        this.typeFinancialTransaction = typeFinancialTransaction;
        return this;
    }

    public PaymentTransaction createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public PaymentTransaction setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    public CashRegister getCashRegister() {
        return cashRegister;
    }

    public PaymentTransaction setCashRegister(CashRegister cashRegister) {
        this.cashRegister = cashRegister;
        return this;
    }

    public CategorieChiffreAffaire getCategorieChiffreAffaire() {
        return categorieChiffreAffaire;
    }

    public PaymentTransaction setCategorieChiffreAffaire(CategorieChiffreAffaire categorieChiffreAffaire) {
        this.categorieChiffreAffaire = categorieChiffreAffaire;
        return this;
    }

    public PaymentTransaction paymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentTransaction)) {
            return false;
        }
        return id != null && id.equals(((PaymentTransaction) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

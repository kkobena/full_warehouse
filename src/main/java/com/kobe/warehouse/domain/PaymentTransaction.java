package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.data.domain.Persistable;

/**
 * A PaymentTransaction.
 */
@Entity
@Table(
    name = "payment_transaction",
    indexes = {
        @Index(columnList = "categorie_ca", name = "pt_categorie_ca_id_index"),
        @Index(columnList = "type_transaction", name = "pt_type_transaction_index"),
        @Index(columnList = "transaction_date", name = "pt_transaction_date_index"),
    }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@IdClass(PaymentId.class)
public class PaymentTransaction implements Persistable<PaymentId>, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Transient
    private boolean isNew = true;

    @Id
    private Long id;

    @Id
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate = LocalDate.now();

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
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(optional = false)
    @JoinColumn(name = "payment_mode_code", referencedColumnName = "code")
    private PaymentMode paymentMode;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cash_register_id", referencedColumnName = "id")
    private CashRegister cashRegister;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "categorie_ca", nullable = false, length = 20)
    private CategorieChiffreAffaire categorieChiffreAffaire = CategorieChiffreAffaire.CA;

    private boolean credit;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_transaction", nullable = false, length = 50)
    private TypeFinancialTransaction typeFinancialTransaction;

    private String commentaire;

    @Column(name = "transaction_number", length = 13)
    private String transactionNumber;

    @Column(name = "dtype", insertable = false, updatable = false)
    private String type;

    @ManyToOne
    private Banque banque;

    public Integer getExpectedAmount() {
        return expectedAmount;
    }

    public PaymentTransaction setExpectedAmount(Integer expectedAmount) {
        this.expectedAmount = expectedAmount;
        return this;
    }

    public String getTransactionNumber() {
        return transactionNumber;
    }

    public PaymentTransaction setTransactionNumber(String transactionNumber) {
        this.transactionNumber = transactionNumber;
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

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
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

    public PaymentId getId() {
        return new PaymentId(id, transactionDate);
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

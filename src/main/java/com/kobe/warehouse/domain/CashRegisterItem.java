package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(
    name = "cash_register_item",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "cash_register_id", "payment_mode_code", "type_transaction" }) }
)
public class CashRegisterItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "cash_register_id", referencedColumnName = "id")
    private CashRegister cashRegister;

    private Long amount;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "payment_mode_code", referencedColumnName = "code")
    private PaymentMode paymentMode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_transaction", nullable = false, length = 30)
    private TypeFinancialTransaction typeFinancialTransaction;

    public Integer getId() {
        return id;
    }

    public CashRegisterItem setId(Integer id) {
        this.id = id;
        return this;
    }

    public TypeFinancialTransaction getTypeFinancialTransaction() {
        return typeFinancialTransaction;
    }

    public CashRegisterItem setTypeFinancialTransaction(TypeFinancialTransaction typeFinancialTransaction) {
        this.typeFinancialTransaction = typeFinancialTransaction;
        return this;
    }

    public CashRegister getCashRegister() {
        return cashRegister;
    }

    public CashRegisterItem setCashRegister(CashRegister cashRegister) {
        this.cashRegister = cashRegister;
        return this;
    }

    public Long getAmount() {
        return amount;
    }

    public CashRegisterItem setAmount(Long amount) {
        this.amount = amount;
        return this;
    }

    public PaymentMode getPaymentMode() {
        return paymentMode;
    }

    public CashRegisterItem setPaymentMode(PaymentMode paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    @Override
    public String toString() {
        return (
            "CashRegisterItem{" +
            "amount=" +
            amount +
            ", id=" +
            id +
            ", cashRegister=" +
            cashRegister +
            ", paymentMode=" +
            paymentMode +
            ", typeFinancialTransaction=" +
            typeFinancialTransaction +
            '}'
        );
    }
}

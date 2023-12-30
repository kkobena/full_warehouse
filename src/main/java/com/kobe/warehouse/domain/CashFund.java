package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_fund")
public class CashFund implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(name = "amount")
    private Double amount;
    @ManyToOne(optional = false)
    @NotNull
    private User user;
    @NotNull
    @Column(name = "created")
    private LocalDateTime created;
    @Column(name = "updated")
    private LocalDateTime updated = LocalDateTime.now();
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cash_fund_type", nullable = false)
    private CashFundType cashFundType;
    @OneToOne
    private CashRegister cashRegister;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private CashRegisterStatut statut;
    @ManyToOne
    private User validatedBy;

    public CashRegisterStatut getStatut() {
        return statut;
    }

    public CashFund setStatut(CashRegisterStatut statut) {
        this.statut = statut;
        return this;
    }

    public User getValidatedBy() {
        return validatedBy;
    }

    public CashFund setValidatedBy(User validatedBy) {
        this.validatedBy = validatedBy;
        return this;
    }

    public Long getId() {
        return id;
    }

    public CashFund setId(Long id) {
        this.id = id;
        return this;
    }

    public Double getAmount() {
        return amount;
    }

    public CashFund setAmount(Double amount) {
        this.amount = amount;
        return this;
    }

    public User getUser() {
        return user;
    }

    public CashFund setUser(User user) {
        this.user = user;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public CashFund setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public CashFund setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public CashFundType getCashFundType() {
        return cashFundType;
    }

    public CashFund setCashFundType(CashFundType cashFundType) {
        this.cashFundType = cashFundType;
        return this;
    }

    public CashRegister getCashRegister() {
        return cashRegister;
    }

    public CashFund setCashRegister(CashRegister cashRegister) {
        this.cashRegister = cashRegister;
        return this;
    }
}

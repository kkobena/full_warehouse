package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_fund")
public class CashFund implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "amount")
    private Integer amount;

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
    private CashFundStatut statut;

    @ManyToOne
    private User validatedBy;

    public CashFundStatut getStatut() {
        return statut;
    }

    public CashFund setStatut(CashFundStatut statut) {
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

    public Integer getAmount() {
        return amount;
    }

    public CashFund setAmount(Integer amount) {
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

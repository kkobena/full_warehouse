package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "cash_register")
public class CashRegister implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @NotNull
    private User user;
    @NotNull
    @Column(name = "init_amount")
    private Double initAmount;
    @Column(name = "final_amount")
    private Double finalAmount;
    @NotNull
    @Column(name = "begin_time")
    private LocalDateTime beginTime;
    @Column(name = "end_time")
    private LocalDateTime endTime;
    @NotNull
    @Column(name = "created")
    private LocalDateTime created;
    @NotNull
    @Column(name = "updated")
    private LocalDateTime updated=LocalDateTime.now();
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private CashRegisterStatut statut;
    @NotNull
    @OneToOne(mappedBy = "cashRegister")
    private CashFund cashFund;
    public Long getId() {
        return id;
    }

    public CashFund getCashFund() {
        return cashFund;
    }

    public CashRegister setCashFund(CashFund cashFund) {
        this.cashFund = cashFund;
        return this;
    }

    public CashRegister setId(Long id) {
        this.id = id;
        return this;
    }

    public User getUser() {
        return user;
    }

    public CashRegister setUser(User user) {
        this.user = user;
        return this;
    }

    public Double getInitAmount() {
        return initAmount;
    }

    public CashRegister setInitAmount(Double initAmount) {
        this.initAmount = initAmount;
        return this;
    }

    public Double getFinalAmount() {
        return finalAmount;
    }

    public CashRegister setFinalAmount(Double finalAmount) {
        this.finalAmount = finalAmount;
        return this;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public CashRegister setBeginTime(LocalDateTime beginTime) {
        this.beginTime = beginTime;
        return this;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public CashRegister setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public CashRegister setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public CashRegister setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public CashRegisterStatut getStatut() {
        return statut;
    }

    public CashRegister setStatut(CashRegisterStatut statut) {
        this.statut = statut;
        return this;
    }
}

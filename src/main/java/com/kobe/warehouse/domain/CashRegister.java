package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cash_register")
public class CashRegister implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    @NotNull
    private AppUser user;

    @NotNull
    @Column(name = "init_amount")
    private Long initAmount;

    @Column(name = "final_amount")
    private Long finalAmount;

    @Column(name = "cancele_amount")
    private Integer canceledAmount;

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
    private LocalDateTime updated = LocalDateTime.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private CashRegisterStatut statut;

    @NotNull
    @OneToOne(mappedBy = "cashRegister")
    private CashFund cashFund;

    @OneToOne(mappedBy = "cashRegister")
    private Ticketing ticketing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_user_id", referencedColumnName = "id")
    private AppUser updatedUser;

    @OneToMany(mappedBy = "cashRegister", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<CashRegisterItem> cashRegisterItems = new ArrayList<>();

    public List<CashRegisterItem> getCashRegisterItems() {
        return cashRegisterItems;
    }

    public CashRegister setCashRegisterItems(List<CashRegisterItem> cashRegisterItems) {
        this.cashRegisterItems = cashRegisterItems;
        return this;
    }

    public Long getId() {
        return id;
    }

    public CashRegister setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getCanceledAmount() {
        return canceledAmount;
    }

    public CashRegister setCanceledAmount(Integer canceledAmount) {
        this.canceledAmount = canceledAmount;
        return this;
    }

    public Ticketing getTicketing() {
        return ticketing;
    }

    public CashRegister setTicketing(Ticketing ticketing) {
        this.ticketing = ticketing;
        return this;
    }

    public CashFund getCashFund() {
        return cashFund;
    }

    public CashRegister setCashFund(CashFund cashFund) {
        this.cashFund = cashFund;
        return this;
    }

    public AppUser getUser() {
        return user;
    }

    public CashRegister setUser(AppUser user) {
        this.user = user;
        return this;
    }

    public Long getInitAmount() {
        return initAmount;
    }

    public CashRegister setInitAmount(Long initAmount) {
        this.initAmount = initAmount;
        return this;
    }

    public AppUser getUpdatedUser() {
        return updatedUser;
    }

    public CashRegister setUpdatedUser(AppUser updatedUser) {
        this.updatedUser = updatedUser;
        return this;
    }

    public Long getFinalAmount() {
        return finalAmount;
    }

    public CashRegister setFinalAmount(Long finalAmount) {
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

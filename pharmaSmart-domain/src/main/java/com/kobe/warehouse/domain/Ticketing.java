package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "ticketing")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Ticketing implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private int numberOf10Thousand;
    private int numberOf5Thousand;
    private int numberOf2Thousand;
    private int numberOf1Thousand;
    private int numberOf500Hundred;
    private int numberOf200Hundred;
    private int numberOf100Hundred;
    private int numberOf50;
    private int numberOf25;
    private int numberOf10;
    private int numberOf5;
    private int numberOf1;
    private int otherAmount;
    private long totalAmount;

    @NotNull
    @Column(name = "created")
    private LocalDateTime created = LocalDateTime.now();

    @OneToOne(optional = false)
    @NotNull
    @JoinColumn(name = "cash_register_id", referencedColumnName = "id")
    private CashRegister cashRegister;

    public Integer getId() {
        return id;
    }

    public Ticketing setId(Integer id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public Ticketing setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public CashRegister getCashRegister() {
        return cashRegister;
    }

    public Ticketing setCashRegister(CashRegister cashRegister) {
        this.cashRegister = cashRegister;
        return this;
    }

    public int getNumberOf10Thousand() {
        return numberOf10Thousand;
    }

    public Ticketing setNumberOf10Thousand(int numbernumberOf10Thousand) {
        this.numberOf10Thousand = numbernumberOf10Thousand;
        return this;
    }

    public int getNumberOf5Thousand() {
        return numberOf5Thousand;
    }

    public Ticketing setNumberOf5Thousand(int numberOf5Thousand) {
        this.numberOf5Thousand = numberOf5Thousand;
        return this;
    }

    public int getNumberOf2Thousand() {
        return numberOf2Thousand;
    }

    public Ticketing setNumberOf2Thousand(int numberOf2Thousand) {
        this.numberOf2Thousand = numberOf2Thousand;
        return this;
    }

    public int getNumberOf1Thousand() {
        return numberOf1Thousand;
    }

    public Ticketing setNumberOf1Thousand(int numberOf1Thousand) {
        this.numberOf1Thousand = numberOf1Thousand;
        return this;
    }

    public int getNumberOf500Hundred() {
        return numberOf500Hundred;
    }

    public Ticketing setNumberOf500Hundred(int numberOf500Hundred) {
        this.numberOf500Hundred = numberOf500Hundred;
        return this;
    }

    public int getNumberOf200Hundred() {
        return numberOf200Hundred;
    }

    public Ticketing setNumberOf200Hundred(int numberOf200Hundred) {
        this.numberOf200Hundred = numberOf200Hundred;
        return this;
    }

    public int getNumberOf100Hundred() {
        return numberOf100Hundred;
    }

    public Ticketing setNumberOf100Hundred(int numberOf100Hundred) {
        this.numberOf100Hundred = numberOf100Hundred;
        return this;
    }

    public int getNumberOf50() {
        return numberOf50;
    }

    public Ticketing setNumberOf50(int numberOf50) {
        this.numberOf50 = numberOf50;
        return this;
    }

    public int getNumberOf25() {
        return numberOf25;
    }

    public Ticketing setNumberOf25(int numberOf25) {
        this.numberOf25 = numberOf25;
        return this;
    }

    public int getNumberOf10() {
        return numberOf10;
    }

    public Ticketing setNumberOf10(int numberOf10) {
        this.numberOf10 = numberOf10;
        return this;
    }

    public int getNumberOf5() {
        return numberOf5;
    }

    public Ticketing setNumberOf5(int numberOf5) {
        this.numberOf5 = numberOf5;
        return this;
    }

    public int getNumberOf1() {
        return numberOf1;
    }

    public Ticketing setNumberOf1(int numberOf1) {
        this.numberOf1 = numberOf1;
        return this;
    }

    public int getOtherAmount() {
        return otherAmount;
    }

    public Ticketing setOtherAmount(int otherAmount) {
        this.otherAmount = otherAmount;
        return this;
    }

    public long getTotalAmount() {
        return totalAmount;
    }

    public Ticketing setTotalAmount(long totalAmount) {
        this.totalAmount = totalAmount;
        return this;
    }
}

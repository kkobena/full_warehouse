package com.kobe.warehouse.domain;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
public class UninsuredCustomer extends Customer implements Serializable {
    private static final long serialVersionUID = 1L;
    @OneToMany(mappedBy = "uninsuredCustomer")
    private Set<CashSale> cashSales = new HashSet<>();

    public Set<CashSale> getCashSales() {
        return cashSales;
    }

    public void setCashSales(Set<CashSale> cashSales) {
        this.cashSales = cashSales;
    }
}

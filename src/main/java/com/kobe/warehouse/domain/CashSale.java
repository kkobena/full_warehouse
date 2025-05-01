package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import java.io.Serial;
import java.io.Serializable;

@Entity
public class CashSale extends Sales implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne
    private CustomerAccount account;

    public CustomerAccount account() {
        return account;
    }

    public void setAccount(CustomerAccount account) {
        this.account = account;
    }
}

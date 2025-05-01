package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

@Entity
public class AccountTransaction extends PaymentTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @ManyToOne
    private CustomerAccount account;

    public CustomerAccount account() {
        return account;
    }

    public void setAccount(CustomerAccount account) {
        this.account = account;
    }
}

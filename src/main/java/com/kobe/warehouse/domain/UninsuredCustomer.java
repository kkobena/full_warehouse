package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.JoinFormula;

@Entity
public class UninsuredCustomer extends Customer {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinFormula("(SELECT o.id FROM customer_account o WHERE  o.customer=id AND o.enabled LIMIT  1)")
    private CustomerAccount account;

    public CustomerAccount account() {
        return account;
    }

    public void setAccount(CustomerAccount account) {
        this.account = account;
    }
}

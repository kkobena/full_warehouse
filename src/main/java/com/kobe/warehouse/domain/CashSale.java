package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
public class CashSale extends Sales  implements Serializable {
    private static final long serialVersionUID = 1L;
    @ManyToOne
    @JsonIgnoreProperties(value = "cashSales", allowSetters = true)
    private UninsuredCustomer uninsuredCustomer;

    public UninsuredCustomer getUninsuredCustomer() {
        return uninsuredCustomer;
    }

    public void setUninsuredCustomer(UninsuredCustomer uninsuredCustomer) {
        this.uninsuredCustomer = uninsuredCustomer;
    }
}

package com.kobe.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * A InvoicePayment.
 */

@Entity
public class DifferePayment extends PaymentTransaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    @OneToMany(mappedBy = "differePayment", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<DifferePaymentItem> differePaymentItems = new ArrayList<>();

    public List<DifferePaymentItem> getDifferePaymentItems() {
        return differePaymentItems;
    }

    public DifferePayment setDifferePaymentItems(List<DifferePaymentItem> differePaymentItems) {
        this.differePaymentItems = differePaymentItems;
        return this;
    }
}

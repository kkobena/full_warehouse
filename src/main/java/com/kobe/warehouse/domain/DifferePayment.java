package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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

    @ManyToOne(fetch = FetchType.LAZY)
    private Customer differeCustomer;

    @OneToMany(mappedBy = "differePayment",fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<DifferePaymentItem> differePaymentItems = new ArrayList<>();

    public List<DifferePaymentItem> getDifferePaymentItems() {
        return differePaymentItems;
    }

    public DifferePayment setDifferePaymentItems(List<DifferePaymentItem> differePaymentItems) {
        this.differePaymentItems = differePaymentItems;
        return this;
    }

    public DifferePayment() {
        super();
        super.setTypeFinancialTransaction(TypeFinancialTransaction.REGLEMENT_DIFFERE);
    }

    public Customer getDiffereCustomer() {
        return differeCustomer;
    }

    public DifferePayment setDiffereCustomer(Customer differeCustomer) {
        this.differeCustomer = differeCustomer;
        return this;
    }
}

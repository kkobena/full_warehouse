package com.kobe.warehouse.domain;


import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.kobe.warehouse.domain.enumeration.PaymentGroup;

/**
 * A PaymentMode.
 */
@Entity
@Table(name = "payment_mode")
public class PaymentMode implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "libelle", nullable = false,unique = true)
    private String libelle;

    @NotNull
    @Column(name = "code", nullable = false,unique = true)
    private String code;

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "payment_group", nullable = false)
    private PaymentGroup group;

    @OneToMany(mappedBy = "paymentMode")
    private Set<PaymentFournisseur> paymentFournisseurs = new HashSet<>();

    @OneToMany(mappedBy = "paymentMode")
    private Set<Payment> payments = new HashSet<>();


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public PaymentMode libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getCode() {
        return code;
    }

    public PaymentMode code(String code) {
        this.code = code;
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PaymentGroup getGroup() {
        return group;
    }

    public PaymentMode group(PaymentGroup group) {
        this.group = group;
        return this;
    }

    public void setGroup(PaymentGroup group) {
        this.group = group;
    }

    public Set<PaymentFournisseur> getPaymentFournisseurs() {
        return paymentFournisseurs;
    }

    public PaymentMode paymentFournisseurs(Set<PaymentFournisseur> paymentFournisseurs) {
        this.paymentFournisseurs = paymentFournisseurs;
        return this;
    }

    public PaymentMode addPaymentFournisseur(PaymentFournisseur paymentFournisseur) {
        this.paymentFournisseurs.add(paymentFournisseur);
        paymentFournisseur.setPaymentMode(this);
        return this;
    }

    public PaymentMode removePaymentFournisseur(PaymentFournisseur paymentFournisseur) {
        this.paymentFournisseurs.remove(paymentFournisseur);
        paymentFournisseur.setPaymentMode(null);
        return this;
    }

    public void setPaymentFournisseurs(Set<PaymentFournisseur> paymentFournisseurs) {
        this.paymentFournisseurs = paymentFournisseurs;
    }

    public Set<Payment> getPayments() {
        return payments;
    }

    public PaymentMode payments(Set<Payment> payments) {
        this.payments = payments;
        return this;
    }

    public PaymentMode addPayment(Payment payment) {
        this.payments.add(payment);
        payment.setPaymentMode(this);
        return this;
    }

    public PaymentMode removePayment(Payment payment) {
        this.payments.remove(payment);
        payment.setPaymentMode(null);
        return this;
    }

    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentMode)) {
            return false;
        }
        return id != null && id.equals(((PaymentMode) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PaymentMode{" +
            "id=" + getId() +
            ", libelle='" + getLibelle() + "'" +
            ", code='" + getCode() + "'" +
            ", group='" + getGroup() + "'" +
            "}";
    }
}

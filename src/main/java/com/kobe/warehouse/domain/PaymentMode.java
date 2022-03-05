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

    @NotNull
    @Size(max = 50)
    @Id
    @Column(length = 50)
    private String code;
    @NotNull
    @Column(name = "libelle", nullable = false,unique = true)
    private String libelle;


    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "payment_group", nullable = false)
    private PaymentGroup group;


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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentMode)) {
            return false;
        }
        return code != null && code.equals(((PaymentMode) o).code);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PaymentMode{" +

            ", libelle='" + getLibelle() + "'" +
            ", code='" + getCode() + "'" +
            ", group='" + getGroup() + "'" +
            "}";
    }
}

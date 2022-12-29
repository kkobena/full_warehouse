package com.kobe.warehouse.domain;


import com.kobe.warehouse.domain.enumeration.PaymentGroup;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.io.Serializable;

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
    @Column(name = "libelle", nullable = false, unique = true)
    private String libelle;


    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "payment_group", nullable = false)
    private PaymentGroup group;


    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public PaymentMode libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PaymentMode code(String code) {
        this.code = code;
        return this;
    }

    public PaymentGroup getGroup() {
        return group;
    }

    public void setGroup(PaymentGroup group) {
        this.group = group;
    }

    public PaymentMode group(PaymentGroup group) {
        this.group = group;
        return this;
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

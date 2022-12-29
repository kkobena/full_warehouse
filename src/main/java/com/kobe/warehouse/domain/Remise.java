package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.Status;

import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.FutureOrPresent;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A Remise.
 */
@Entity
@Table(name = "remise")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Remise implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @Column(name = "valeur")
    private String valeur;
    @NotNull
    @Column(name = "remise_value", nullable = false)
    private Float remiseValue;
    @NotNull
    @Column(name = "enable", nullable = false, columnDefinition = "boolean default true")
    private boolean enable = true;
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private Status status = Status.ENABLE;
    @NotNull
    @Column(name = "code", unique = true, nullable = false)
    private String code;
    @FutureOrPresent
    private LocalDateTime period;

    public LocalDateTime getPeriod() {
        return period;
    }

    public Remise setPeriod(LocalDateTime period) {
        this.period = period;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Remise setCode(String code) {
        this.code = code;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getValeur() {
        return valeur;
    }

    public void setValeur(String valeur) {
        this.valeur = valeur;
    }

    public Remise valeur(String valeur) {
        this.valeur = valeur;
        return this;
    }

    public Float getRemiseValue() {
        return remiseValue;
    }

    public void setRemiseValue(Float remiseValue) {
        this.remiseValue = remiseValue;
    }

    public Remise remiseValue(Float remiseValue) {
        this.remiseValue = remiseValue;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public Remise setStatus(Status status) {
        this.status = status;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Remise)) {
            return false;
        }
        return id != null && id.equals(((Remise) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Remise{" +
            "id=" + getId() +
            ", valeur='" + getValeur() + "'" +
            ", remiseValue=" + getRemiseValue() +

            "}";
    }
}

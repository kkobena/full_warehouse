package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Remise.
 */
@Entity
@Table(name = "remise")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Remise implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    @Column(name = "begin", nullable = false)
    @FutureOrPresent
    private LocalDate begin = LocalDate.now();

    @NotNull
    @Column(name = "end", nullable = false)
    @FutureOrPresent
    private LocalDate end = LocalDate.now().plusDays(10);

    @Transient
    private float tauxRemise;

    public LocalDate getBegin() {
        return begin;
    }

    public Remise setBegin(LocalDate begin) {
        this.begin = begin;
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

    public float getTauxRemise() {
        if (Objects.nonNull(remiseValue)) {
            tauxRemise = remiseValue / 100;
        }
        return tauxRemise;
    }

    public LocalDate getEnd() {
        return end;
    }

    public Remise setEnd(LocalDate end) {
        this.end = end;
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
        return "Remise{"
            + "id="
            + getId()
            + ", valeur='"
            + getValeur()
            + "'"
            + ", remiseValue="
            + getRemiseValue()
            + "}";
    }
}

package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * A Reference.
 */
@Entity
@Table(name = "reference", uniqueConstraints = { @UniqueConstraint(columnNames = { "mvt_date", "d_type", "num" }) })
public class Reference implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "num", nullable = false)
    private String num;

    @NotNull
    @Min(value = 0)
    @Column(name = "number_transac", nullable = false)
    private Integer numberTransac;

    @NotNull
    @Column(name = "mvt_date", nullable = false)
    private LocalDate mvtDate;

    @NotNull
    @Column(name = "d_type", nullable = false)
    private Integer type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }

    public Reference num(String num) {
        this.num = num;
        return this;
    }

    public Integer getNumberTransac() {
        return numberTransac;
    }

    public void setNumberTransac(Integer numberTransac) {
        this.numberTransac = numberTransac;
    }

    public Reference numberTransac(Integer numberTransac) {
        this.numberTransac = numberTransac;
        return this;
    }

    public LocalDate getMvtDate() {
        return mvtDate;
    }

    public void setMvtDate(LocalDate mvtDate) {
        this.mvtDate = mvtDate;
    }

    public Reference mvtDate(LocalDate mvtDate) {
        this.mvtDate = mvtDate;
        return this;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reference)) {
            return false;
        }
        return id != null && id.equals(((Reference) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Reference{"
            + "id="
            + getId()
            + ", num='"
            + getNum()
            + "'"
            + ", numberTransac="
            + getNumberTransac()
            + ", mvtDate='"
            + getMvtDate()
            + "'"
            + "}";
    }
}

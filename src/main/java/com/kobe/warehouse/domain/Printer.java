package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
    name = "printer",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }), @UniqueConstraint(columnNames = { "name", "poste_id" }) },
    indexes = { @Index(columnList = "name", name = "name_index") }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Printer implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @ColumnDefault("576")
    @NotNull
    @Column(name = "width", nullable = false)
    private int width = 576;

    @NotNull
    @Column(name = "length", nullable = false)
    private Integer length;

    private String address;

    @ColumnDefault("10")
    @NotNull
    @Column(name = "margin_left_and_right", nullable = false)
    private int marginLeftAndRight = 10;

    @ColumnDefault("15")
    @NotNull
    @Column(name = "margin_top", nullable = false)
    private int marginTop = 15;

    @ManyToOne(optional = false)
    private Poste poste;

    public Long getId() {
        return id;
    }

    public Printer setId(Long id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public Printer setName(String name) {
        this.name = name;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public Printer setAddress(String address) {
        this.address = address;
        return this;
    }

    public Integer getWidth() {
        return width;
    }

    public Printer setWidth(Integer width) {
        this.width = width;
        return this;
    }

    public Integer getLength() {
        return length;
    }

    public Printer setLength(Integer length) {
        this.length = length;
        return this;
    }

    public Poste getPoste() {
        return poste;
    }

    public Printer setPoste(Poste poste) {
        this.poste = poste;
        return this;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getMarginLeftAndRight() {
        return marginLeftAndRight;
    }

    public void setMarginLeftAndRight(int marginLeftAndRight) {
        this.marginLeftAndRight = marginLeftAndRight;
    }

    public int getMarginTop() {
        return marginTop;
    }

    public void setMarginTop(int marginTop) {
        this.marginTop = marginTop;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Printer printer = (Printer) o;
        return Objects.equals(id, printer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

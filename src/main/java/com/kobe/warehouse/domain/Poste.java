package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
    name = "poste",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }) },
    indexes = { @Index(columnList = "name", name = "poste_name_index") }
)
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class Poste implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    private String posteNumber;

    @NotNull
    @Column(name = "address", nullable = false)
    private String address;

    @OneToMany(mappedBy = "poste")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Printer> printers = new ArrayList<>();

    public String getPosteNumber() {
        return posteNumber;
    }

    public Poste setPosteNumber(String posteNumber) {
        this.posteNumber = posteNumber;
        return this;
    }

    public Long getId() {
        return id;
    }

    public Poste setId(Long id) {
        this.id = id;
        return this;
    }

    public List<Printer> getPrinters() {
        return printers;
    }

    public Poste setPrinters(List<Printer> printers) {
        this.printers = printers;
        return this;
    }

    public String getName() {
        return name;
    }

    public Poste setName(String name) {
        this.name = name;
        return this;
    }

    public String getAddress() {
        return address;
    }

    public Poste setAddress(String address) {
        this.address = address;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Poste printer = (Poste) o;
        return Objects.equals(id, printer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

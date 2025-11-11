package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(
    name = "poste",
    uniqueConstraints = {@UniqueConstraint(columnNames = {"name"})}
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Poste implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @Column(name = "poste_number", length = 20)
    private String posteNumber;

    @NotNull
    @Column(name = "address", nullable = false, unique = true)
    @Pattern(
        regexp = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
        message = "L'adresse doit être une adresse IP valide"
    )
    private String address;
    @ColumnDefault("false")
    @Column(name = "customer_display")
    private boolean customerDisplay;
    @Column(name = "customer_display_port", length = 10)
    private String customerDisplayPort;

    public boolean isCustomerDisplay() {
        return customerDisplay;
    }

    public String getCustomerDisplayPort() {
        return customerDisplayPort;
    }

    public Poste setCustomerDisplayPort(String customerDisplayPort) {
        this.customerDisplayPort = customerDisplayPort;
        return this;
    }

    public Poste setCustomerDisplay(boolean customerDisplay) {
        this.customerDisplay = customerDisplay;
        return this;
    }

    public String getPosteNumber() {
        return posteNumber;
    }

    public Poste setPosteNumber(String posteNumber) {
        this.posteNumber = posteNumber;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public Poste setId(Integer id) {
        this.id = id;
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

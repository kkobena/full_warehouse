package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.ColumnDefault;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "sequence")
public class WarehouseSequence implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    @ColumnDefault("0")
    @Column(name = "seq_value")
    private Integer value;
    @ColumnDefault("1")
    @Column(name = "increment")
    private short increment = 1;

    public String getName() {
        return name;
    }

    public WarehouseSequence setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getValue() {
        return value;
    }

    public WarehouseSequence setValue(Integer value) {
        this.value = value;
        return this;
    }

    public short getIncrement() {
        return increment;
    }

    public WarehouseSequence setIncrement(short increment) {
        this.increment = increment;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WarehouseSequence that = (WarehouseSequence) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        String sb = "WarehouseSequence{" + "name='" + name + '\'' + ", value=" + value + ", increment=" + increment + '}';
        return sb;
    }
}

package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "warehouse_sequence")
public class WarehouseSequence implements Serializable {

  private static final long serialVersionUID = 1L;
  @Id private String name;

  @Column(name = "seq_value", columnDefinition = "int default '0'")
  private Integer value;

  @Column(name = "increment", columnDefinition = "int(4) default '1'")
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
      String sb = "WarehouseSequence{" + "name='" + name + '\''
          + ", value=" + value
          + ", increment=" + increment
          + '}';
    return sb;
  }
}

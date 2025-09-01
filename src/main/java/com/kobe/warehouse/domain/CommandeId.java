package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class CommandeId implements Serializable {

    private Long id;
    private LocalDate orderDate;

    public CommandeId() {}

    public CommandeId(Long id, LocalDate orderDate) {
        this.id = id;
        this.orderDate = orderDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDate orderDate) {
        this.orderDate = orderDate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        CommandeId orderLineId = (CommandeId) o;
        return Objects.equals(id, orderLineId.id) && Objects.equals(orderDate, orderLineId.orderDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderDate);
    }
}

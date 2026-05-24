package com.kobe.warehouse.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class OrderLineId implements Serializable {

    private Integer id;
    private LocalDate orderDate;

    public OrderLineId() {}

    public OrderLineId(Integer id, LocalDate orderDate) {
        this.id = id;
        this.orderDate = orderDate;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
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
        OrderLineId orderLineId = (OrderLineId) o;
        return Objects.equals(id, orderLineId.id) && Objects.equals(orderDate, orderLineId.orderDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderDate);
    }
}

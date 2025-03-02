package com.kobe.warehouse.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.time.LocalDate;
import java.util.Objects;

public class DailyStock {

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate stockDay;

    private int stock;

    public LocalDate getStockDay() {
        return stockDay;
    }

    public DailyStock setStockDay(LocalDate stockDay) {
        this.stockDay = stockDay;
        return this;
    }

    public int getStock() {
        return stock;
    }

    public DailyStock setStock(int stock) {
        this.stock = stock;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyStock that = (DailyStock) o;
        return stockDay.equals(that.stockDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockDay);
    }

    public DailyStock() {}
}

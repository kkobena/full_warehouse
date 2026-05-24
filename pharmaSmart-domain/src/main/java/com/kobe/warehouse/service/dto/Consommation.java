package com.kobe.warehouse.service.dto;

import java.util.Objects;

public class Consommation {

    private int id;
    private short month;
    private int year;
    private long consommation;

    public int getId() {
        return id;
    }

    public Consommation setId(int id) {
        this.id = id;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Consommation that = (Consommation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Consommation(int id) {
        this.id = id;
    }

    public short getMonth() {
        return month;
    }

    public Consommation setMonth(short month) {
        this.month = month;
        return this;
    }

    public int getYear() {
        return year;
    }

    public Consommation setYear(int year) {
        this.year = year;
        return this;
    }

    public long getConsommation() {
        return consommation;
    }

    public Consommation setConsommation(long consommation) {
        this.consommation = consommation;
        return this;
    }

    public Consommation() {}
}

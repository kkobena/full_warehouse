package com.kobe.warehouse.service.dto;

public class TypeMvtProduitDTO {
    private  int value;
    private  String name;

    public TypeMvtProduitDTO(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public TypeMvtProduitDTO setValue(int value) {
        this.value = value;
        return this;
    }

    public String getName() {
        return name;
    }

    public TypeMvtProduitDTO setName(String name) {
        this.name = name;
        return this;
    }
}

package com.kobe.warehouse.service.dto;

import java.util.Objects;

public class ActionPrivilegeDTO {

    private String name;
    private String libelle;

    public ActionPrivilegeDTO() {}

    public ActionPrivilegeDTO(String name, String libelle) {
        this.name = name;
        this.libelle = libelle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ActionPrivilegeDTO that = (ActionPrivilegeDTO) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    public String getName() {
        return name;
    }

    public ActionPrivilegeDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public ActionPrivilegeDTO setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }
}

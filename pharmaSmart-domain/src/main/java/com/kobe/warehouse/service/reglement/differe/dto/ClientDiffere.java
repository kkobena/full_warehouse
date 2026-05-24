package com.kobe.warehouse.service.reglement.differe.dto;

public interface ClientDiffere {
    String getFirsName();

    String getLastName();

    Long getId();

    default String getFullName() {
        return getFirsName() + " " + getLastName();
    }
}

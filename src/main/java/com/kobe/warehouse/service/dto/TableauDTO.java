package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Tableau;
import jakarta.validation.constraints.NotNull;

public class TableauDTO {

    private Long id;

    @NotNull
    private String code;

    @NotNull
    private Integer value;

    public TableauDTO() {}

    public TableauDTO(Tableau tableau) {
        this.id = tableau.getId();
        this.code = tableau.getCode();
        this.value = tableau.getValue();
    }

    public Long getId() {
        return id;
    }

    public TableauDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public TableauDTO setCode(String code) {
        this.code = code;
        return this;
    }

    public Integer getValue() {
        return value;
    }

    public TableauDTO setValue(Integer value) {
        this.value = value;
        return this;
    }
}

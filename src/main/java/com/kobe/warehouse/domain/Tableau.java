package com.kobe.warehouse.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "tableau", uniqueConstraints = {@UniqueConstraint(columnNames = {"code"})},
    indexes = {
        @Index(columnList = "code", name = "code_index")
    }
)
public class Tableau implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotNull
    @Column(name = "code", nullable = false)
    private String code;
    @NotNull
    @Column(name = "valeur", nullable = false)
    private Integer value;

    public Tableau() {
    }

    public Long getId() {
        return id;
    }

    public Tableau setId(Long id) {
        this.id = id;
        return this;
    }

    public String getCode() {
        return code;
    }

    public Tableau setCode(String code) {
        this.code = code;
        return this;
    }

    public Integer getValue() {
        return value;
    }

    public Tableau setValue(Integer value) {
        this.value = value;
        return this;
    }
}

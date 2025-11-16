package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(
    name = "tableau",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "code" }) },
    indexes = { @Index(columnList = "code", name = "code_index") }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Tableau implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @NotNull
    @Column(name = "valeur", nullable = false)
    private Integer value;

    public Tableau() {}

    public Integer getId() {
        return id;
    }

    public Tableau setId(Integer id) {
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

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

@Entity
@Table(
    name = "dci",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "code" }), @UniqueConstraint(columnNames = { "libelle" }) },
    indexes = { @Index(columnList = "libelle", name = "dci_libelle_index") }
)
public class Dci implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @NotNull
    @Column(name = "libelle", nullable = false)
    private String libelle;

    public Dci() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull String getCode() {
        return code;
    }

    public void setCode(@NotNull String code) {
        this.code = code;
    }

    public @NotNull String getLibelle() {
        return libelle;
    }

    public void setLibelle(@NotNull String libelle) {
        this.libelle = libelle;
    }
}

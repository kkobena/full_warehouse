package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Rayon.
 */
@Entity
@Table(
    name = "rayon",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "libelle", "storage_id" }), @UniqueConstraint(columnNames = { "code", "storage_id" }),
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Rayon implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "code", nullable = false)
    private String code;

    @NotNull
    @Column(name = "libelle", nullable = false)
    private String libelle;

    @ManyToOne(optional = false)
    @NotNull
    private Storage storage;

    @Column(name = "exclude", columnDefinition = "boolean default false")
    private boolean exclude;

    public Rayon id(Long id) {
        this.id = id;
        return this;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public @NotNull String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public @NotNull Storage getStorage() {
        return storage;
    }

    public void setStorage(Storage storage) {
        this.storage = storage;
    }

    public boolean isExclude() {
        return exclude;
    }

    public void setExclude(boolean exclude) {
        this.exclude = exclude;
    }

    public Rayon code(String code) {
        this.code = code;
        return this;
    }

    public Rayon libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rayon)) {
            return false;
        }
        return id != null && id.equals(((Rayon) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    @Override
    public String toString() {
        return (
            "Rayon{" +
            "id=" +
            id +
            ", createdAt=" +
            ", code='" +
            code +
            '\'' +
            ", libelle='" +
            libelle +
            '\'' +
            ", exclude=" +
            exclude +
            '}'
        );
    }
}

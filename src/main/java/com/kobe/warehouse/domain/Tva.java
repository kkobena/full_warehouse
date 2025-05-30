package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Tva.
 */
@Entity
@Table(name = "tva", uniqueConstraints = { @UniqueConstraint(columnNames = { "taux" }) })
public class Tva implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "taux", nullable = false, unique = true)
    private Integer taux = 0;

    public Long getId() {
        return id;
    }

    public Tva setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getTaux() {
        return taux;
    }

    public void setTaux(Integer taux) {
        this.taux = taux;
    }

    public Tva taux(Integer taux) {
        this.taux = taux;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tva)) {
            return false;
        }
        return id != null && id.equals(((Tva) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Tva{" +
            "id=" + getId() +
            ", taux=" + getTaux() +
            "}";
    }
}

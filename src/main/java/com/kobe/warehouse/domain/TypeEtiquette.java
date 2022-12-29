package com.kobe.warehouse.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;


/**
 * A TypeEtiquette.
 */
@Entity
@Table(name = "type_etiquette")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class TypeEtiquette implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @NotNull
    @Column(name = "libelle", nullable = false, unique = true)
    private String libelle;

    public Long getId() {
        return id;
    }

    public TypeEtiquette setId(Long id) {
        this.id = id;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public TypeEtiquette setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeEtiquette)) {
            return false;
        }
        return id != null && id.equals(((TypeEtiquette) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

}

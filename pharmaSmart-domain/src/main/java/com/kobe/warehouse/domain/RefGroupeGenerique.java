package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

/** Groupe générique BDPM : un princeps et ses génériques, substituables entre eux. Lecture seule. */
@Entity
@Table(name = "ref_groupe_generique")
public class RefGroupeGenerique implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    private Integer id;

    @Column(name = "libelle", nullable = false, length = 500)
    private String libelle;

    protected RefGroupeGenerique() {}

    public Integer getId() {
        return id;
    }

    public String getLibelle() {
        return libelle;
    }
}

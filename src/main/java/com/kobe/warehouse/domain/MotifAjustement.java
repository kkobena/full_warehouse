package com.kobe.warehouse.domain;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "motif_ajustement", uniqueConstraints = {@UniqueConstraint(columnNames = {"libelle"})})
public class MotifAjustement implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false)
    private Long id;
    @NotBlank
    @NotNull
    @Column(name = "libelle", nullable = false, unique = true)
    private String libelle;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public MotifAjustement() {

    }
}

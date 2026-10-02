package com.kobe.warehouse.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;

/** Rubriques du RCP d'une spécialité : texte libre, repris tel quel. Lecture seule. */
@Entity
@Table(name = "ref_specialite_rcp")
public class RefSpecialiteRcp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "cis", length = 8)
    private String cis;

    @Column(name = "indications", columnDefinition = "text")
    private String indications;

    @Column(name = "posologie", columnDefinition = "text")
    private String posologie;

    @Column(name = "contre_indications", columnDefinition = "text")
    private String contreIndications;

    protected RefSpecialiteRcp() {}

    public String getCis() {
        return cis;
    }

    public String getIndications() {
        return indications;
    }

    public String getPosologie() {
        return posologie;
    }

    public String getContreIndications() {
        return contreIndications;
    }
}

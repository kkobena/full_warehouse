package com.kobe.warehouse.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.kobe.warehouse.domain.enumeration.ParcoursProduitStatut;
import java.time.LocalDate;

public class ParcoursProduit {

    private ParcoursProduitStatut produitStatut;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    private LocalDate evtDate;

    public ParcoursProduitStatut getProduitStatut() {
        return produitStatut;
    }

    public ParcoursProduit() {}

    public ParcoursProduit setProduitStatut(ParcoursProduitStatut produitStatut) {
        this.produitStatut = produitStatut;
        return this;
    }

    public LocalDate getEvtDate() {
        return evtDate;
    }

    public ParcoursProduit setEvtDate(LocalDate evtDate) {
        this.evtDate = evtDate;
        return this;
    }
}

package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;

@Entity
public class VenteDepotAgree extends Sales implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "depot_agree_id", referencedColumnName = "id")
    private Magasin depotAgree;

    public @NotNull Magasin getDepotAgree() {
        return depotAgree;
    }

    public VenteDepotAgree setDepotAgree(@NotNull Magasin depotAgree) {
        this.depotAgree = depotAgree;
        return this;
    }
}

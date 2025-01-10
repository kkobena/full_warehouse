package com.kobe.warehouse.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A RemiseProduit.
 */
@Entity
public class RemiseProduit extends Remise implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "remiseProduit", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    private List<GrilleRemise> grilles = new ArrayList<>();

    public List<GrilleRemise> getGrilles() {
        return grilles;
    }

    public void setGrilles(List<GrilleRemise> grilles) {
        this.grilles = grilles;
    }
}

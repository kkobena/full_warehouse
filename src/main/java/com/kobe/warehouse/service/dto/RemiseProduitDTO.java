package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GrilleRemise;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.service.GrilleRemiseDTO;
import java.util.ArrayList;
import java.util.List;

public class RemiseProduitDTO extends RemiseDTO {

    private List<GrilleRemiseDTO> grilles = new ArrayList<>();

    public RemiseProduitDTO() {}

    public RemiseProduitDTO(RemiseProduit remise) {
        super(remise);
        this.setTypeLibelle("Remise produit");
        this.grilles = remise.getGrilles().stream().map(GrilleRemiseDTO::new).toList();
    }

    public RemiseProduitDTO(RemiseProduit remise, List<GrilleRemise> grillesRemise) {
        super(remise);
        this.setTypeLibelle("Remise produit");
        this.grilles = grillesRemise.stream().map(GrilleRemiseDTO::new).toList();
    }

    public List<GrilleRemiseDTO> getGrilles() {
        return grilles;
    }

    public RemiseProduitDTO setGrilles(List<GrilleRemiseDTO> grilles) {
        this.grilles = grilles;
        return this;
    }
}

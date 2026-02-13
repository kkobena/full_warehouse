package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.GrilleRemise;
import com.kobe.warehouse.domain.RemiseProduit;
import com.kobe.warehouse.domain.enumeration.CodeGrilleType;

import java.util.ArrayList;
import java.util.List;

public class RemiseProduitDTO extends RemiseDTO {
    private int vnoDiscountRate;
    private int voDiscountRate;
    private List<GrilleRemiseDTO> grilles = new ArrayList<>();

    public RemiseProduitDTO() {
        // Empty constructor needed for Jackson.
    }

    public RemiseProduitDTO(RemiseProduit remise) {
        super(remise);
        this.setTypeLibelle("Remise produit");
        this.grilles = remise.getGrilles().stream().map(GrilleRemiseDTO::new).toList();
        this.vnoDiscountRate=this.grilles.stream().filter(grilleRemiseDTO -> grilleRemiseDTO.getGrilleType()== CodeGrilleType.VNO).mapToInt(GrilleRemiseDTO::getDiscountRate).max().orElse(0);
        this.voDiscountRate=this.grilles.stream().filter(grilleRemiseDTO -> grilleRemiseDTO.getGrilleType()== CodeGrilleType.VO).mapToInt(GrilleRemiseDTO::getDiscountRate).max().orElse(0);

    }

    public int getVnoDiscountRate() {
        return vnoDiscountRate;
    }

    public void setVnoDiscountRate(int vnoDiscountRate) {
        this.vnoDiscountRate = vnoDiscountRate;
    }

    public int getVoDiscountRate() {
        return voDiscountRate;
    }

    public void setVoDiscountRate(int voDiscountRate) {
        this.voDiscountRate = voDiscountRate;
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

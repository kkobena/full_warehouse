package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.GrilleRemise;
import com.kobe.warehouse.domain.enumeration.CodeGrilleType;
import com.kobe.warehouse.domain.enumeration.CodeRemise;
import com.kobe.warehouse.service.dto.CodeRemiseDTO;

public class GrilleRemiseDTO {

    private Long id;
    private boolean enable;
    private Float remiseValue;
    private String code;
    private float tauxRemise;
    private CodeRemiseDTO codeRemise;
    private CodeGrilleType grilleType;

    public GrilleRemiseDTO() {}

    public GrilleRemiseDTO(GrilleRemise grilleRemise) {
        this.code = grilleRemise.getCode().getValue();
        this.grilleType = grilleRemise.getCode().getCodeGrilleType();
        this.enable = grilleRemise.isEnable();
        this.id = grilleRemise.getId();
        this.remiseValue = grilleRemise.getRemiseValue();
        this.tauxRemise = grilleRemise.getTauxRemise();
        this.codeRemise = CodeRemise.toDTO(CodeRemise.fromGrille(grilleRemise.getCode()));
    }

    public String getCode() {
        return code;
    }

    public GrilleRemiseDTO setCode(String code) {
        this.code = code;
        return this;
    }

    public CodeGrilleType getGrilleType() {
        return grilleType;
    }

    public GrilleRemiseDTO setGrilleType(CodeGrilleType grilleType) {
        this.grilleType = grilleType;
        return this;
    }

    public boolean isEnable() {
        return enable;
    }

    public GrilleRemiseDTO setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public Long getId() {
        return id;
    }

    public GrilleRemiseDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public Float getRemiseValue() {
        return remiseValue;
    }

    public GrilleRemiseDTO setRemiseValue(Float remiseValue) {
        this.remiseValue = remiseValue;
        return this;
    }

    public CodeRemiseDTO getCodeRemise() {
        return codeRemise;
    }

    public GrilleRemiseDTO setCodeRemise(CodeRemiseDTO codeRemise) {
        this.codeRemise = codeRemise;
        return this;
    }

    public float getTauxRemise() {
        return tauxRemise;
    }

    public GrilleRemiseDTO setTauxRemise(float tauxRemise) {
        this.tauxRemise = tauxRemise;
        return this;
    }
}

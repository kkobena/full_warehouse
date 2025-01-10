package com.kobe.warehouse.service.dto;

public class InstallationDataDTO {

    private TypeImportationProduit typeImportation;
    private Long fournisseurId;

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public InstallationDataDTO setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
        return this;
    }

    public TypeImportationProduit getTypeImportation() {
        return typeImportation;
    }

    public InstallationDataDTO setTypeImportation(TypeImportationProduit typeImportation) {
        this.typeImportation = typeImportation;
        return this;
    }
}

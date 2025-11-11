package com.kobe.warehouse.service.dto;

public class InstallationDataDTO {

    private TypeImportationProduit typeImportation;
    private Integer fournisseurId;

    public Integer getFournisseurId() {
        return fournisseurId;
    }

    public InstallationDataDTO setFournisseurId(Integer fournisseurId) {
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

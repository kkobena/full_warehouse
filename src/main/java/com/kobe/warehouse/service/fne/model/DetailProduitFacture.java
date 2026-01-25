package com.kobe.warehouse.service.fne.model;

public interface DetailProduitFacture {
    int getQuantite();

    int getCodeTva();

    String getProduitCode();

    String getProduitCodeEan();

    String getLibelle();

    int getPrixUnitaire();

    double getTauxRemise();
    double getTauxCouverture();
    Integer getTarifReferenceAssurance();
}

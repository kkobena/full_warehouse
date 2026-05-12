package com.kobe.warehouse.domain.enumeration;

public enum StatutLegal {

    SANS_LISTE(
        "Médicament ou produit disponible sans ordonnance. "
            + "Aucune restriction de dispensation, retour possible sous conditions."
    ),

    LISTE_I(
        "Substance vénéneuse de liste I (arrêté). "
            + "Ordonnance obligatoire, renouvelable sauf mention contraire. "
            + "Retour client possible avec vérification de l'état du produit."
    ),

    LISTE_II(
        "Substance vénéneuse de liste II (arrêté). "
            + "Ordonnance obligatoire, non renouvelable par défaut. "
            + "Retour client possible avec vérification de l'état du produit."
    ),

    STUPEFIANTS(
        "Stupéfiant soumis à ordonnance sécurisée (carnet à souche). "
            + "Traçabilité lot obligatoire, quantité maximale délivrable réglementée. "
            + "Retour client interdit — destruction réglementaire obligatoire."
    ),

    PSO(
        "Prescription Sécurisée Obligatoire : psychotropes et autres substances "
            + "à prescription sécurisée sans être classés stupéfiants. "
            + "Ordonnance sécurisée obligatoire. Retour client interdit."
    );

    private final String description;

    StatutLegal(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOrdonnanceObligatoire() {
        return this != SANS_LISTE;
    }

    public boolean isRetourInterdit() {
        return this == STUPEFIANTS || this == PSO;
    }

    public boolean isTracabilityLotObligatoire() {
        return this == STUPEFIANTS || this == PSO;
    }
}

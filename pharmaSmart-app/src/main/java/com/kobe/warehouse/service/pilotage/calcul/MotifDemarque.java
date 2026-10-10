package com.kobe.warehouse.service.pilotage.calcul;

public record MotifDemarque(String libelle, long quantite, long valeur, long valeurReference) {
    public MotifDemarque plus(MotifDemarque reference) {
        return new MotifDemarque(libelle, quantite, valeur, valeurReference + reference.valeurReference);
    }
}

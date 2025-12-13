package com.kobe.warehouse.service.dto.dashboard;

public record AnalyseABCDTO(
    ClasseABCItem classeA,
    ClasseABCItem classeB,
    ClasseABCItem classeC
) {
    public record ClasseABCItem(
        Integer nombreProduits,
        Double pourcentageProduits,
        Double pourcentageCA,
        Long valeur
    ) {
    }
}

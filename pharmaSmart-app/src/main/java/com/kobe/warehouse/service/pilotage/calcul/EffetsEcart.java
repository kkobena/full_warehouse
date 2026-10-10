package com.kobe.warehouse.service.pilotage.calcul;

public record EffetsEcart(Long frequentation, Long articles, Long prix) {
    public static final EffetsEcart INCALCULABLES = new EffetsEcart(null, null, null);

    public static EffetsEcart calculer(MesuresPilotage periode, MesuresPilotage reference) {
        if (reference.nbVentes() == 0 || reference.quantiteServie() == 0 || periode.nbVentes() == 0 || periode.quantiteServie() == 0) {
            return INCALCULABLES;
        }
        double ventes0 = reference.nbVentes();
        double articles0 = (double) reference.quantiteServie() / reference.nbVentes();
        double prix0 = (double) reference.caLignesTtc() / reference.quantiteServie();
        double ventes1 = periode.nbVentes();
        double articles1 = (double) periode.quantiteServie() / periode.nbVentes();
        double prix1 = (double) periode.caLignesTtc() / periode.quantiteServie();
        return new EffetsEcart(
            Math.round((ventes1 - ventes0) * articles0 * prix0),
            Math.round(ventes1 * (articles1 - articles0) * prix0),
            Math.round(ventes1 * articles1 * (prix1 - prix0))
        );
    }
}

package com.kobe.warehouse.service.pilotage.calcul;

import java.util.List;

/** CroissanceAnnuelle annuelle moyenne entre la première et la dernière année close ayant des ventes. */
public record CroissanceAnnuelle(Double taux, Integer depuis, Integer jusqua) {
    private static final double CENT = 100.0;

    public static final CroissanceAnnuelle AUCUNE = new CroissanceAnnuelle(null, null, null);

    public static CroissanceAnnuelle calculer(List<Integer> anneesCloses, LectureAnnees lecture) {
        List<Integer> retenues = anneesCloses.stream().filter(annee -> lecture.brut(lecture.total(annee)) > 0).toList();
        if (retenues.size() < 2) {
            return AUCUNE;
        }
        int depuis = retenues.getFirst();
        int jusqua = retenues.getLast();
        double rapport = lecture.brut(lecture.total(jusqua)) / lecture.brut(lecture.total(depuis));
        return new CroissanceAnnuelle((Math.pow(rapport, 1.0 / (jusqua - depuis)) - 1) * CENT, depuis, jusqua);
    }
}

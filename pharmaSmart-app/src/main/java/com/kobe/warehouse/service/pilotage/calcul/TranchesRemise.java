package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.service.dto.pilotage.MembreAnalyseDTO;
import java.util.ArrayList;
import java.util.List;

/**
 * Tranches de remise lues sur le taux de chaque vente, selon les seuils de l'officine ({@code APP_PILOTAGE_TRANCHES_REMISE}) :
 * avec 5, 10, 20 → sans remise, moins de 5 %, 5 à 10 %, 10 à 20 %, 20 % et plus. La clé d'une tranche est {@code min-max}
 * (taux inclus) : le dépôt d'analyse sait la filtrer.
 */
public record TranchesRemise(List<Tranche> tranches) {
    private static final int TAUX_MAX = 100;

    public static TranchesRemise decouper(List<Integer> seuils) {
        List<Tranche> tranches = new ArrayList<>();
        tranches.add(new Tranche(0, 0, "Sans remise"));
        int debut = 1;
        for (int seuil : seuils) {
            if (seuil > debut) {
                String libelle = debut == 1 ? "Moins de " + seuil + " %" : debut + " à " + seuil + " %";
                tranches.add(new Tranche(debut, seuil - 1, libelle));
                debut = seuil;
            }
        }
        tranches.add(new Tranche(debut, TAUX_MAX, debut == 1 ? "Avec remise" : debut + " % et plus"));
        return new TranchesRemise(List.copyOf(tranches));
    }

    public MembreAnalyseDTO classer(int taux) {
        Tranche tranche = tranches.stream().filter(candidate -> taux <= candidate.max()).findFirst().orElse(tranches.getLast());
        return new MembreAnalyseDTO(tranche.min() + "-" + tranche.max(), tranche.libelle());
    }

    public record Tranche(int min, int max, String libelle) {}
}

package com.kobe.warehouse.service.ordonnance;

import com.kobe.warehouse.domain.enumeration.StatutOrdonnance;
import com.kobe.warehouse.service.dto.ordonnance.LigneSuivieDTO;
import java.time.LocalDate;
import java.util.List;

/**
 * Règles du suivi, sans accès aux données. La capacité d'une ligne est
 * {@code quantité prescrite x (renouvellements + 1)} ; le reste à délivrer s'en déduit, et le statut
 * se calcule à chaque lecture : il ne peut pas être périmé.
 */
public final class SuiviOrdonnance {

    private SuiviOrdonnance() {}

    public static int capacite(int quantitePrescrite, int renouvellements) {
        return quantitePrescrite * (renouvellements + 1);
    }

    public static int resteADelivrer(int quantitePrescrite, int renouvellements, int quantiteDelivree) {
        return Math.max(0, capacite(quantitePrescrite, renouvellements) - quantiteDelivree);
    }

    /** Passages complets de l'ordonnance : le plus petit nombre de fois où chaque ligne a été entièrement délivrée. */
    public static int passagesComplets(List<LigneSuivieDTO> lignes) {
        return lignes.stream().mapToInt(l -> l.quantiteDelivree() / l.quantitePrescrite()).min().orElse(0);
    }

    /**
     * Renouvellements encore utilisables. Le premier passage n'en est pas un : tant qu'il n'est pas
     * complet, tous les renouvellements restent ; ensuite, chaque passage en consomme un.
     */
    public static int renouvellementsRestants(int renouvellements, List<LigneSuivieDTO> lignes) {
        int passages = passagesComplets(lignes);
        return passages == 0 ? renouvellements : Math.max(0, renouvellements + 1 - passages);
    }

    public static StatutOrdonnance statut(boolean cloturee, LocalDate dateFinValidite, int renouvellements, List<LigneSuivieDTO> lignes, LocalDate aujourdhui) {
        boolean soldee = !lignes.isEmpty() && lignes.stream().allMatch(l -> resteADelivrer(l.quantitePrescrite(), renouvellements, l.quantiteDelivree()) == 0);
        if (cloturee || soldee) {
            return StatutOrdonnance.TERMINEE;
        }
        if (dateFinValidite != null && dateFinValidite.isBefore(aujourdhui)) {
            return StatutOrdonnance.EXPIREE;
        }
        return StatutOrdonnance.EN_COURS;
    }
}

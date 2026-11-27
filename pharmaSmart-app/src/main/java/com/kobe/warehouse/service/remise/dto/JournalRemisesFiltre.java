package com.kobe.warehouse.service.remise.dto;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import java.time.LocalDate;
import java.util.Set;

/**
 * La demande d'un journal : la période et les filtres. Un ensemble vide ou nul ne filtre rien. Le
 * magasin n'en fait pas partie : c'est toujours celui de l'utilisateur.
 */
public record JournalRemisesFiltre(
        LocalDate du,
        LocalDate au,
        Set<Integer> caissiers,
        Set<Integer> vendeurs,
        Set<NatureVente> naturesVente,
        Set<Long> promotions,
        Set<OrigineRemise> origines,
        Set<Integer> familles) {

    public JournalRemisesFiltre {
        caissiers = caissiers == null ? Set.of() : Set.copyOf(caissiers);
        vendeurs = vendeurs == null ? Set.of() : Set.copyOf(vendeurs);
        naturesVente = naturesVente == null ? Set.of() : Set.copyOf(naturesVente);
        promotions = promotions == null ? Set.of() : Set.copyOf(promotions);
        origines = origines == null ? Set.of() : Set.copyOf(origines);
        familles = familles == null ? Set.of() : Set.copyOf(familles);
    }

    public static JournalRemisesFiltre periode(LocalDate du, LocalDate au) {
        return new JournalRemisesFiltre(du, au, null, null, null, null, null, null);
    }
}

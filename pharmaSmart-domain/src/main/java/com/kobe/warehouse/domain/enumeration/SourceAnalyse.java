package com.kobe.warehouse.domain.enumeration;

import static com.kobe.warehouse.domain.enumeration.IndicateurPilotage.*;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Agrégat sur lequel une analyse est lue. Les lignes connaissent le produit (famille, laboratoire…) et la marge, mais pas le
 * nombre de ventes ; les en-têtes connaissent l'heure, le caissier et le nombre de ventes, mais pas le produit.
 */
public enum SourceAnalyse {
    LIGNES(EnumSet.of(CA_TTC, CA_HT, CA_NET, QUANTITES_VENDUES, PRIX_MOYEN_ARTICLE, MARGE_BRUTE, TAUX_MARGE, REMISES, TAUX_REMISE, POIDS_REMISES_MARGE)),
    ENTETES(EnumSet.of(CA_TTC, CA_HT, CA_NET, NB_VENTES, PANIER_MOYEN, REMISES, TAUX_REMISE, PART_TIERS_PAYANT));

    private final Set<IndicateurPilotage> indicateurs;

    SourceAnalyse(Set<IndicateurPilotage> indicateurs) {
        this.indicateurs = indicateurs;
    }

    public Set<IndicateurPilotage> getIndicateurs() {
        return indicateurs;
    }

    /** La source qui sait ventiler par tous les axes et calcule le plus d'indicateurs demandés ; à égalité, les lignes. */
    public static Optional<SourceAnalyse> choisir(Collection<AxeAnalyse> axes, Collection<IndicateurPilotage> indicateurs) {
        return Stream.of(values())
            .filter(source -> axes.stream().allMatch(axe -> axe.getSources().contains(source)))
            .max(Comparator.comparingLong((SourceAnalyse source) -> indicateurs.stream().filter(source.indicateurs::contains).count()).thenComparing(Comparator.reverseOrder()));
    }
}

package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;

import java.math.BigDecimal;

/**
 * DTO représentant le score de classification Pareto d'un produit.
 * Contient toutes les métriques utilisées pour déterminer la classe de criticité.
 *
 * @param produitId ID du produit
 * @param libelle Libellé du produit
 * @param ca12Mois Chiffre d'affaires sur 12 mois (en centimes)
 * @param vmm12Mois Consommation mensuelle moyenne sur 12 mois (CMM)
 * @param qteVendue12Mois Quantité totale vendue sur 12 mois
 * @param frequenceVenteMois Nombre de mois distincts avec ventes sur les 12 derniers mois
 * @param caCumulePct % CA cumulé dans l'analyse Pareto (issu de v_abc_pareto_analysis) — faible = produit critique
 * @param stockActuel Stock actuel total
 * @param rang Rang dans l'analyse Pareto (1 = produit le plus vendu)
 * @param scoreFrequence Fréquence de vente brute (0–12 mois sur 12)
 * @param scoreTotal Score inversé pour les logs : (int)(100 − caCumulePct), plus élevé = plus critique
 * @param classeSuggeree Classe de criticité suggérée par le Pareto
 * @param classeActuelle Classe de criticité actuelle du produit
 * @param estNouveauProduit Si le produit est considéré comme nouveau (< X mois)
 * @param ancienneteMois Ancienneté du produit en mois
 * @param changementSignificatif Si le changement dépasse le seuil d'hysteresis
 */
public record ClassificationScoreDTO(
    Integer produitId,
    String libelle,
    Long ca12Mois,
    Integer vmm12Mois,
    Integer qteVendue12Mois,
    Integer frequenceVenteMois,
    BigDecimal caCumulePct,
    Integer stockActuel,
    int rang,
    int scoreFrequence,
    int scoreTotal,
    ClasseCriticite classeSuggeree,
    ClasseCriticite classeActuelle,
    boolean estNouveauProduit,
    int ancienneteMois,
    boolean changementSignificatif
) {

    /**
     * Vérifie si un changement de classe est nécessaire.
     */
    public boolean doitChangerClasse() {
        if (estNouveauProduit) {
            return false;
        }
        return classeSuggeree != classeActuelle && changementSignificatif;
    }

    /**
     * Vérifie si le produit est promu (monte en classe).
     */
    public boolean estPromotion() {
        if (classeSuggeree == null || classeActuelle == null) {
            return false;
        }
        return getOrdreClasse(classeSuggeree) > getOrdreClasse(classeActuelle);
    }

    /**
     * Vérifie si le produit est rétrogradé (descend en classe).
     */
    public boolean estRetrogradation() {
        if (classeSuggeree == null || classeActuelle == null) {
            return false;
        }
        return getOrdreClasse(classeSuggeree) < getOrdreClasse(classeActuelle);
    }

    private int getOrdreClasse(ClasseCriticite classe) {
        return switch (classe) {
            case A_PLUS -> 5;
            case A -> 4;
            case B -> 3;
            case C -> 2;
            case D -> 1;
        };
    }

    /**
     * Contribution du CA Pareto au score final (rang parmi les plus critiques).
     */
    public double getContributionPareto() {
        return caCumulePct != null ? 100.0 - caCumulePct.doubleValue() : 0.0;
    }

    /**
     * Contribution de la fréquence au score.
     */
    public double getContributionFrequence() {
        return scoreFrequence * (100.0 / 12.0);
    }

    public String getResume() {
        return String.format(
            "Pareto: rang %d, caCumulé %.1f%% → Classe %s%s",
            rang,
            caCumulePct != null ? caCumulePct.doubleValue() : 100.0,
            classeSuggeree != null ? classeSuggeree.getCode() : "?",
            doitChangerClasse() ? " (changement)" : ""
        );
    }
}

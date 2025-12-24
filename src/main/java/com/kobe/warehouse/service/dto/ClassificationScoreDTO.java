package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;

import java.math.BigDecimal;

/**
 * DTO représentant le score de classification d'un produit.
 * Contient toutes les métriques et scores utilisés pour déterminer la classe de criticité.
 *
 * @param produitId ID du produit
 * @param libelle Libellé du produit
 * @param ca12Mois Chiffre d'affaires sur 12 mois (en centimes)
 * @param vmm12Mois Ventes Mensuelles Moyennes sur 12 mois
 * @param qteVendue12Mois Quantité totale vendue sur 12 mois
 * @param frequenceVenteMois Nombre de mois avec ventes sur les 12 derniers mois
 * @param rotationAnnuelle Rotation annuelle (ventes / stock moyen)
 * @param stockActuel Stock actuel total
 * @param scoreCA Score normalisé du CA (0-100)
 * @param scoreRotation Score normalisé de la rotation (0-100)
 * @param scoreFrequence Score normalisé de la fréquence (0-100)
 * @param scoreTotal Score final pondéré (0-100)
 * @param classeSuggeree Classe de criticité suggérée par le score
 * @param classeActuelle Classe de criticité actuelle du produit
 * @param estNouveauProduit Si le produit est considéré comme nouveau (< X mois)
 * @param ancienneteMois Ancienneté du produit en mois
 * @param changementSignificatif Si le changement de score est significatif (> seuil hysteresis)
 */
public record ClassificationScoreDTO(
    Integer produitId,
    String libelle,
    Long ca12Mois,
    Integer vmm12Mois,
    Integer qteVendue12Mois,
    Integer frequenceVenteMois,
    BigDecimal rotationAnnuelle,
    Integer stockActuel,
    int scoreCA,
    int scoreRotation,
    int scoreFrequence,
    int scoreTotal,
    ClasseCriticite classeSuggeree,
    ClasseCriticite classeActuelle,
    boolean estNouveauProduit,
    int ancienneteMois,
    boolean changementSignificatif
) {

    /**
     * Vérifie si un changement de classe est nécessaire
     *
     * @return true si la classe suggérée est différente de la classe actuelle
     *         et que le changement est significatif
     */
    public boolean doitChangerClasse() {
        if (estNouveauProduit) {
            return false;
        }
        return classeSuggeree != classeActuelle && changementSignificatif;
    }

    /**
     * Vérifie si le produit est promu (monte en classe)
     *
     * @return true si la nouvelle classe est supérieure à l'ancienne
     */
    public boolean estPromotion() {
        if (classeSuggeree == null || classeActuelle == null) {
            return false;
        }
        return getOrdreClasse(classeSuggeree) > getOrdreClasse(classeActuelle);
    }

    /**
     * Vérifie si le produit est rétrogradé (descend en classe)
     *
     * @return true si la nouvelle classe est inférieure à l'ancienne
     */
    public boolean estRetrogradation() {
        if (classeSuggeree == null || classeActuelle == null) {
            return false;
        }
        return getOrdreClasse(classeSuggeree) < getOrdreClasse(classeActuelle);
    }

    /**
     * Obtient l'ordre numérique d'une classe (A+ = 5, A = 4, B = 3, C = 2, D = 1)
     */
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
     * Calcule la contribution du CA au score final
     *
     * @param poidsCa Poids du CA (0.0 à 1.0)
     * @return Contribution au score
     */
    public double getContributionCA(double poidsCa) {
        return scoreCA * poidsCa;
    }

    /**
     * Calcule la contribution de la rotation au score final
     *
     * @param poidsRotation Poids de la rotation (0.0 à 1.0)
     * @return Contribution au score
     */
    public double getContributionRotation(double poidsRotation) {
        return scoreRotation * poidsRotation;
    }

    /**
     * Calcule la contribution de la fréquence au score final
     *
     * @param poidsFrequence Poids de la fréquence (0.0 à 1.0)
     * @return Contribution au score
     */
    public double getContributionFrequence(double poidsFrequence) {
        return scoreFrequence * poidsFrequence;
    }

    /**
     * Retourne un résumé textuel du score
     *
     * @return Description du score
     */
    public String getResume() {
        return String.format(
            "Score: %d (CA: %d, Rotation: %d, Freq: %d) -> Classe %s%s",
            scoreTotal,
            scoreCA,
            scoreRotation,
            scoreFrequence,
            classeSuggeree != null ? classeSuggeree.getCode() : "?",
            doitChangerClasse() ? " (changement)" : ""
        );
    }
}

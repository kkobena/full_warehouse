package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour les suggestions de réapprovisionnement selon la méthode SEMOIS.
 * Contient toutes les informations nécessaires pour décider du réapprovisionnement.
 *
 * @param produitId ID du produit
 * @param libelle Libellé du produit
 * @param codeCip Code CIP du produit
 * @param fournisseurId ID du fournisseur principal
 * @param fournisseurLibelle Libellé du fournisseur principal
 * @param classeCriticite Classe de criticité (A+, A, B, C, D)
 * @param vmm Ventes Mensuelles Moyennes (pondérées)
 * @param margeSecurite Marge de sécurité calculée
 * @param stockObjectif Stock objectif SEMOIS (VMM + marge de sécurité)
 * @param stockActuel Stock actuel total du produit
 * @param quantiteACommander Quantité à commander (Stock Objectif - Stock Actuel, si > 0)
 * @param delaiLivraisonJours Délai de livraison du fournisseur en jours
 * @param coefficientSecurite Coefficient de sécurité appliqué
 * @param facteurSaisonnier Facteur d'ajustement saisonnier actuel
 * @param dateDernierCalcul Date du dernier calcul SEMOIS
 */
/**
 * DTO pour les suggestions de réapprovisionnement selon la méthode SEMOIS.
 *
 * @param couvertParCommandeEnCours true si une commande REQUESTED couvre déjà le besoin
 *        (pendingQty > 0 ET qteVueACommander > 0 ET qteACommander = 0 après ajustement).
 *        Distinct de quantiteACommander=0 qui peut aussi signifier "produit non configuré".
 */
public record SemoisSuggestionDTO(
    Integer produitId,
    String libelle,
    String codeCip,
    Integer fournisseurId,
    String fournisseurLibelle,
    ClasseCriticite classeCriticite,
    Integer vmm,
    Integer margeSecurite,
    Integer stockObjectif,
    Integer stockActuel,
    Integer quantiteACommander,
    Integer delaiLivraisonJours,
    BigDecimal coefficientSecurite,
    BigDecimal facteurSaisonnier,
    LocalDateTime dateDernierCalcul
) {

    /**
     * Calcule le taux de couverture en mois
     *
     * @return Nombre de mois de stock disponible (Stock Actuel / VMM)
     */
    public double getTauxCouvertureMois() {
        if (vmm == null || vmm == 0) {
            return 0.0;
        }
        return (double) stockActuel / vmm;
    }

    /**
     * Calcule la couverture cible en mois (objectif pharmacien).
     * Indique combien de mois de ventes le stock objectif représente.
     * Formule : Stock Objectif / VMM
     *
     * @return Nombre de mois de couverture cible (Stock Objectif / VMM)
     */
    public double getCouvertureCibleMois() {
        if (vmm == null || vmm == 0) {
            return 0.0;
        }
        return stockObjectif != null ? (double) stockObjectif / vmm : 0.0;
    }

    /**
     * Calcule la couverture de la marge de sécurité en mois.
     * Indique combien de mois de ventes la marge de sécurité représente.
     * Formule : Marge de Sécurité / VMM
     *
     * @return Nombre de mois de marge de sécurité (Marge / VMM)
     */
    public double getMargeSecuriteCibleMois() {
        if (vmm == null || vmm == 0) {
            return 0.0;
        }
        return margeSecurite != null ? (double) margeSecurite / vmm : 0.0;
    }

    /**
     * Construit un message comparatif de couverture (actuelle vs cible).
     * Permet un affichage lisible du ratio stock actuel / stock objectif en mois.
     *
     * @return Message "X.X mois / Y.Y mois cible"
     */
    public String getMessageCouvertureComparatif() {
        return String.format("%.1f mois / %.1f mois cible",
            getTauxCouvertureMois(), getCouvertureCibleMois());
    }

    /**
     * Vérifie si le produit est en rupture potentielle
     *
     * @return true si le stock actuel est inférieur à la marge de sécurité
     */
    public boolean estEnRupture() {
        if (stockActuel == null || margeSecurite == null) {
            return false;
        }
        return stockActuel < margeSecurite;
    }

    /**
     * Vérifie si le produit est en surstock
     *
     * @return true si le stock actuel dépasse 150% du stock objectif
     */
    public boolean estEnSurstock() {
        if (stockActuel == null || stockObjectif == null || stockObjectif == 0) {
            return false;
        }
        return stockActuel > (stockObjectif * 1.5);
    }

    /**
     * Calcule l'écart entre stock actuel et stock objectif
     *
     * @return Écart en unités (positif = surstock, négatif = sous-stock)
     */
    public int getEcartStockObjectif() {
        if (stockActuel == null || stockObjectif == null) {
            return 0;
        }
        return stockActuel - stockObjectif;
    }

    /**
     * Calcule l'écart en pourcentage par rapport au stock objectif
     *
     * @return Écart en % (positif = surstock, négatif = sous-stock)
     */
    public double getEcartStockObjectifPourcent() {
        if (stockObjectif == null || stockObjectif == 0) {
            return 0.0;
        }
        return ((double) getEcartStockObjectif() / stockObjectif) * 100.0;
    }

    /**
     * Vérifie si un réapprovisionnement est nécessaire
     *
     * @return true si quantiteACommander > 0
     */
    public boolean necessiteReappro() {
        return quantiteACommander != null && quantiteACommander > 0;
    }

    /**
     * Niveau d'urgence du réapprovisionnement.
     * Cohérent avec le filtre repository : quantiteACommander > 0 toujours garanti.
     * <ul>
     *   <li>URGENT — stock &lt; marge de sécurité (rupture potentielle)</li>
     *   <li>NORMAL — stock entre marge et objectif</li>
     *   <li>OK     — stock ≥ objectif (ne devrait pas apparaître si filtre appliqué)</li>
     * </ul>
     */
    public String getNiveauUrgence() {
        if (estEnRupture()) return "URGENT";
        if (necessiteReappro()) return "NORMAL";
        return "OK";
    }

    /**
     * Nombre de jours de stock restant.
     * Un stock négatif est traité comme 0 (artefact comptable, sans sens physique).
     */
    public int getJoursStockRestant() {
        if (vmm == null || vmm == 0 || stockActuel == null) {
            return 0;
        }
        int stockEffectif = Math.max(0, stockActuel);
        double vmmJour = vmm / 30.0;
        return (int) Math.ceil(stockEffectif / vmmJour);
    }

    /**
     * Vérifie si le calcul est récent (< 24h)
     */
    public boolean estCalculRecent() {
        if (dateDernierCalcul == null) {
            return false;
        }
        return dateDernierCalcul.isAfter(LocalDateTime.now().minusDays(1));
    }

    /**
     * Message de suggestion lisible pour l'utilisateur.
     */
    public String getMessageSuggestion() {
        if (estEnRupture()) {
            if (quantiteACommander == null || quantiteACommander == 0) {
                return String.format(
                    "⏳ Rupture couverte par commande en cours — stock: %d (seuil: %d) — couverture: %.1f/%.1f mois",
                    stockActuel, margeSecurite, getTauxCouvertureMois(), getCouvertureCibleMois());
            }
            return String.format(
                "⚠️ URGENT — Commander %d unité(s) — stock: %d (seuil: %d) — %d jour(s) restant(s)",
                quantiteACommander, stockActuel, margeSecurite, getJoursStockRestant());
        }
        if (necessiteReappro()) {
            return String.format(
                "ℹ️ Commander %d unité(s) — couverture: %.1f/%.1f mois (%d jour(s) restant(s))",
                quantiteACommander, getTauxCouvertureMois(), getCouvertureCibleMois(), getJoursStockRestant());
        }
        return String.format(
            "✅ Stock suffisant — couverture: %.1f mois (cible: %.1f mois)",
            getTauxCouvertureMois(), getCouvertureCibleMois());
    }
}

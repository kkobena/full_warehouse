package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import java.util.List;

/**
 * DTO pour le tableau de bord réapprovisionnement SEMOIS temps réel.
 * Consolide les indicateurs clés de l'état du stock vs. les paramètres SEMOIS calculés.
 *
 * <p><b>Niveaux d'urgence :</b>
 * <ul>
 *   <li><b>RUPTURE</b> : stock_actuel &lt; marge_securite — commande urgente requise</li>
 *   <li><b>SOUS_SEUIL</b> : marge_securite ≤ stock_actuel &lt; stock_objectif — commande prochaine</li>
 *   <li><b>OK</b> : stock_objectif ≤ stock_actuel ≤ 1.5 × stock_objectif — stock optimal</li>
 *   <li><b>SURSTOCK</b> : stock_actuel &gt; 1.5 × stock_objectif — risque péremption</li>
 *   <li><b>NON_CONFIGURE</b> : vmm = 0 ou config absente — paramétrage requis</li>
 * </ul>
 */
public record ReapproDashboardDTO(

    /** Nombre total de produits actifs avec configuration SEMOIS (vmm > 0). */
    long totalProduits,

    /** Produits en rupture critique (stock &lt; marge de sécurité). */
    long nbRupture,

    /** Produits sous seuil : stock entre marge de sécurité et stock objectif. */
    long nbSousSeuil,

    /** Produits à stock optimal : stock entre objectif et 150 % de l'objectif. */
    long nbOk,

    /** Produits en surstock : stock &gt; 150 % du stock objectif. */
    long nbSurstock,

    /** Produits sans données de vente (vmm = 0) — nécessitent un paramétrage initial. */
    long nbSansConfig,

    /** Quantité totale à commander pour tous les produits en rupture (unités). */
    long totalUnitesManquantes,

    /** Répartition par classe de criticité. */
    List<ClasseBreakdown> parClasse,

    /** Top 10 des produits les plus urgents (rupture), triés par taux de couverture croissant. */
    List<TopUrgentDTO> topUrgents

) {

    /**
     * Répartition des produits par classe de criticité SEMOIS.
     */
    public record ClasseBreakdown(
        ClasseCriticite classeCriticite,
        long nbProduits,
        long nbRupture,
        long nbSousSeuil,
        long nbOk,
        long nbSurstock
    ) {}

    /**
     * Produit urgent nécessitant une commande immédiate.
     */
    public record TopUrgentDTO(
        Integer produitId,
        String libelle,
        String codeCip,
        String fournisseurLibelle,
        ClasseCriticite classeCriticite,
        int vmm,
        int margeSecurite,
        int stockObjectif,
        long stockActuel,
        long quantiteACommander,
        /** Taux de couverture en mois = stock_actuel / vmm. */
        double tauxCouvertureMois
    ) {}

    /** Pourcentage de produits OK sur le total configuré. */
    public double tauxOkPourcent() {
        if (totalProduits == 0) return 0.0;
        return (double) nbOk / totalProduits * 100.0;
    }

    /** Pourcentage de produits en rupture ou sous seuil (produits à risque). */
    public double tauxRisquePourcent() {
        if (totalProduits == 0) return 0.0;
        return (double) (nbRupture + nbSousSeuil) / totalProduits * 100.0;
    }
}


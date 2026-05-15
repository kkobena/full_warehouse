package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SemoisSuggestionView;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour la vue ordinaire v_semois_suggestion.
 *
 * LECTURE SEULE: stock_actuel et quantite_a_commander sont calculés en temps réel.
 * vmm, marge_securite, stock_objectif viennent de semois_configuration (batch nocturne).
 * Ne jamais tenter d'écrire dans cette vue.
 */
@Repository
public interface SemoisSuggestionViewRepository extends JpaRepository<SemoisSuggestionView, Integer> {

    /**
     * Recherche paginée de suggestions SEMOIS avec filtres.
     * Lit directement depuis la vue matérialisée (très performant).
     *
     * @param search Recherche dans libellé ou code CIP (optionnel)
     * @param classeCriticite Filtre par classe de criticité (optionnel)
     * @param pageable Pagination et tri
     * @return Page de suggestions depuis la vue matérialisée
     */
    @Query("""
        SELECT s FROM SemoisSuggestionView s
        WHERE (:classeCriticite IS NULL OR s.classeCriticite = :classeCriticite)
          AND (:fournisseurId IS NULL
               OR s.fournisseurId = :fournisseurId
               OR s.fournisseurId = (SELECT f.parent.id FROM Fournisseur f WHERE f.id = :fournisseurId AND f.parent IS NOT NULL))
          AND (:search IS NULL OR :search = '' OR
               LOWER(s.libelle) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.codeCip) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:niveauUrgence IS NULL OR
               (:niveauUrgence = 'URGENT' AND s.vmm > 0 AND s.stockActuel < s.margeSecurite) OR
               (:niveauUrgence = 'NORMAL' AND s.vmm > 0 AND s.stockActuel >= s.margeSecurite
                                           AND s.stockActuel < s.stockObjectif) OR
               (:niveauUrgence = 'OK'     AND (s.vmm = 0 OR s.stockActuel >= s.stockObjectif)))
        """)
    Page<SemoisSuggestionView> findAllWithFilters(
        @Param("search") String search,
        @Param("classeCriticite") ClasseCriticite classeCriticite,
        @Param("fournisseurId") Integer fournisseurId,
        @Param("niveauUrgence") String niveauUrgence,
        Pageable pageable
    );

    /** Tous les produits en rupture (stockActuel < margeSecurite, vmm > 0) sans pagination. */
    @Query("""
        SELECT s FROM SemoisSuggestionView s
        WHERE s.vmm > 0 AND s.stockActuel < s.margeSecurite
        ORDER BY s.stockActuel ASC
        """)
    List<SemoisSuggestionView> findAllUrgents();

    /**
     * Compte les produits urgents (rupture + sous seuil) depuis v_semois_suggestion.
     * Utilisé pour le badge de notification de la navbar et du tableau de bord.
     * Urgents = rupture (stock_actuel < marge_securite) + sous seuil (marge_securite <= stock_actuel < stock_objectif)
     */
    @Query(value = """
        SELECT COUNT(*) FILTER (WHERE vmm > 0 AND stock_actuel < stock_objectif)
        FROM v_semois_suggestion
        """, nativeQuery = true)
    Long countUrgentProducts();

    /** Fournisseurs distincts ayant au moins un produit SEMOIS configuré. */
    @Query(value = """
        SELECT DISTINCT fournisseur_id, fournisseur_libelle
        FROM v_semois_suggestion
        WHERE fournisseur_id IS NOT NULL
        ORDER BY fournisseur_libelle
        """, nativeQuery = true)
    List<Object[]> findDistinctFournisseurs();


    /**
     * Retourne les statistiques globales du dashboard réappro en une seule requête.
     * Résultat : List avec un seul Object[] de 7 colonnes :
     * [0] total_produits, [1] nb_rupture, [2] nb_sous_seuil, [3] nb_ok,
     * [4] nb_surstock, [5] nb_sans_config, [6] total_unites_manquantes
     *
     * <p>Retourné en List&lt;Object[]&gt; car Hibernate 7 ne déroule plus
     * automatiquement le résultat scalaire d'une native query en Object[].</p>
     */
    @Query(value = """
        SELECT
            COUNT(*)                                                                              AS total_produits,
            COUNT(*) FILTER (WHERE vmm > 0 AND stock_actuel < marge_securite)                    AS nb_rupture,
            COUNT(*) FILTER (WHERE vmm > 0 AND stock_actuel >= marge_securite
                                           AND stock_actuel < stock_objectif)                    AS nb_sous_seuil,
            COUNT(*) FILTER (WHERE vmm > 0 AND stock_actuel >= stock_objectif
                                           AND stock_actuel <= stock_objectif * 1.5)             AS nb_ok,
            COUNT(*) FILTER (WHERE vmm > 0 AND stock_objectif > 0
                                           AND stock_actuel > stock_objectif * 1.5)              AS nb_surstock,
            COUNT(*) FILTER (WHERE vmm = 0 OR vmm IS NULL)                                       AS nb_sans_config,
            COALESCE(SUM(quantite_a_commander)
                FILTER (WHERE vmm > 0 AND stock_actuel < marge_securite), 0)                     AS total_unites_manquantes
        FROM v_semois_suggestion
        """, nativeQuery = true)
    List<Object[]> getDashboardGlobalStats();

    /**
     * Répartition par classe de criticité pour le dashboard réappro.
     * Résultat : lignes de Object[] avec :
     * [0] classe_criticite, [1] nb_produits, [2] nb_rupture,
     * [3] nb_sous_seuil, [4] nb_ok, [5] nb_surstock
     */
    @Query(value = """
        SELECT
            classe_criticite,
            COUNT(*)                                                                              AS nb_produits,
            COUNT(*) FILTER (WHERE vmm > 0 AND stock_actuel < marge_securite)                    AS nb_rupture,
            COUNT(*) FILTER (WHERE vmm > 0 AND stock_actuel >= marge_securite
                                           AND stock_actuel < stock_objectif)                    AS nb_sous_seuil,
            COUNT(*) FILTER (WHERE vmm > 0 AND stock_actuel >= stock_objectif
                                           AND stock_actuel <= stock_objectif * 1.5)             AS nb_ok,
            COUNT(*) FILTER (WHERE vmm > 0 AND stock_objectif > 0
                                           AND stock_actuel > stock_objectif * 1.5)              AS nb_surstock
        FROM v_semois_suggestion
        WHERE classe_criticite IS NOT NULL
        GROUP BY classe_criticite
        ORDER BY classe_criticite
        """, nativeQuery = true)
    List<Object[]> getDashboardStatsByClasse();

    /**
     * Top N produits les plus urgents (en rupture ou sous seuil, vmm > 0).
     * Triés par taux de couverture croissant (les plus critiques en premier).
     */
    @Query(value = """
        SELECT
            produit_id,
            libelle,
            code_cip,
            fournisseur_libelle,
            classe_criticite,
            vmm,
            marge_securite,
            stock_objectif,
            stock_actuel,
            quantite_a_commander,
            CASE WHEN vmm > 0 THEN CAST(stock_actuel AS FLOAT) / vmm ELSE 0 END AS taux_couverture_mois
        FROM v_semois_suggestion
        WHERE vmm > 0
          AND stock_actuel < stock_objectif
        ORDER BY taux_couverture_mois ASC, quantite_a_commander DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Object[]> findTopUrgentProducts(@Param("limit") int limit);
}

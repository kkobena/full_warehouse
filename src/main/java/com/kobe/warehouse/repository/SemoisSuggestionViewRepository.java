package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SemoisSuggestionView;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository pour la vue matérialisée mv_semois_suggestion.
 *
 * LECTURE SEULE: Cette vue est rafraîchie automatiquement tous les jours à 3h du matin
 * après le recalcul SEMOIS. Ne jamais tenter d'écrire dans cette vue.
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
          AND (:search IS NULL OR :search = '' OR
               LOWER(s.libelle) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.codeCip) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<SemoisSuggestionView> findAllWithFilters(
        @Param("search") String search,
        @Param("classeCriticite") ClasseCriticite classeCriticite,
        Pageable pageable
    );

    /**
     * Compte le nombre de produits nécessitant un réapprovisionnement urgent
     * (quantite_a_commander > 0 et stock_actuel < marge_securite)
     *
     * @return Nombre de produits en alerte
     */
    @Query("""
        SELECT COUNT(s) FROM SemoisSuggestionView s
        WHERE s.quantiteACommander > 0
          AND s.stockActuel < s.margeSecurite
        """)
    long countUrgentReappro();

    /**
     * Compte le nombre de produits nécessitant un réapprovisionnement normal
     * (quantite_a_commander > 0 et stock_actuel >= marge_securite)
     *
     * @return Nombre de produits en alerte normale
     */
    @Query("""
        SELECT COUNT(s) FROM SemoisSuggestionView s
        WHERE s.quantiteACommander > 0
          AND s.stockActuel >= s.margeSecurite
        """)
    long countNormalReappro();

    /**
     * Compte le nombre de produits avec stock suffisant
     * (quantite_a_commander = 0)
     *
     * @return Nombre de produits OK
     */
    @Query("""
        SELECT COUNT(s) FROM SemoisSuggestionView s
        WHERE s.quantiteACommander = 0
        """)
    long countOkStock();

    /**
     * Récupère toutes les suggestions nécessitant un réapprovisionnement
     * trié par priorité (classe criticité puis quantité à commander)
     *
     * @param pageable Pagination
     * @return Page de suggestions nécessitant réappro
     */
    @Query("""
        SELECT s FROM SemoisSuggestionView s
        WHERE s.quantiteACommander > 0
        ORDER BY
            CASE s.classeCriticite
                WHEN com.kobe.warehouse.domain.enumeration.ClasseCriticite.A_PLUS THEN 1
                WHEN com.kobe.warehouse.domain.enumeration.ClasseCriticite.A THEN 2
                WHEN com.kobe.warehouse.domain.enumeration.ClasseCriticite.B THEN 3
                WHEN com.kobe.warehouse.domain.enumeration.ClasseCriticite.C THEN 4
                WHEN com.kobe.warehouse.domain.enumeration.ClasseCriticite.D THEN 5
                ELSE 6
            END,
            s.quantiteACommander DESC
        """)
    Page<SemoisSuggestionView> findAllNeedingReappro(Pageable pageable);
}

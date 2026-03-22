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
          AND (:search IS NULL OR :search = '' OR
               LOWER(s.libelle) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(s.codeCip) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<SemoisSuggestionView> findAllWithFilters(
        @Param("search") String search,
        @Param("classeCriticite") ClasseCriticite classeCriticite,
        Pageable pageable
    );

}

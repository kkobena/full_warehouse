package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

 import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour SemoisConfiguration.
 * Gère les configurations SEMOIS par produit.
 */
@Repository
public interface SemoisConfigurationRepository extends JpaRepository<SemoisConfiguration, Integer> {

    /**
     * Trouve la configuration SEMOIS pour un produit donné
     *
     * @param produitId ID du produit
     * @return La configuration si elle existe
     */
    Optional<SemoisConfiguration> findByProduitId(Integer produitId);

    @Query("SELECT MAX(c.dateDernierCalcul) FROM SemoisConfiguration c")
    java.time.LocalDateTime findMaxDateDernierCalcul();

    @Query("SELECT COUNT(c) FROM SemoisConfiguration c")
    long countAll();

    /**
     * Vérifie si un produit a une configuration SEMOIS
     *
     * @param produitId ID du produit
     * @return true si le produit a une configuration
     */
    boolean existsByProduitId(Integer produitId);


    /**
     * Charge les configurations SEMOIS pour un lot de produits (évite les N+1 dans suggerer()).
     *
     * @param produitIds IDs des produits
     * @return Liste des configurations existantes (les produits sans config sont absents)
     */
    List<SemoisConfiguration> findByProduitIdIn(java.util.Collection<Integer> produitIds);


    /**
     * Recherche paginée de configurations SEMOIS avec filtres.
     * Charge les produits en eager fetch pour éviter LazyInitializationException.
     *
     * @param search Recherche dans libellé ou code CIP (optionnel)
     * @param classeCriticite Filtre par classe de criticité (optionnel)
     * @param pageable Pagination et tri
     * @return Page de configurations filtrées
     */
    @Query(value = """
        SELECT DISTINCT sc FROM SemoisConfiguration sc
        JOIN FETCH sc.produit p
        WHERE (:classeCriticite IS NULL OR sc.classeCriticite = :classeCriticite)
          AND (:search IS NULL OR :search = '' OR
               LOWER(p.libelle) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(p.codeEanLaboratoire) LIKE LOWER(CONCAT('%', :search, '%'))) ORDER BY p.libelle ASC
        """,
        countQuery = """
        SELECT COUNT(DISTINCT sc) FROM SemoisConfiguration sc
        JOIN sc.produit p
        WHERE (:classeCriticite IS NULL OR sc.classeCriticite = :classeCriticite)
          AND (:search IS NULL OR :search = '' OR
               LOWER(p.libelle) LIKE LOWER(CONCAT('%', :search, '%')) OR
               LOWER(p.codeEanLaboratoire) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<SemoisConfiguration> findAllWithFilters(
        @Param("search") String search,
        @Param("classeCriticite") ClasseCriticite classeCriticite,
        Pageable pageable
    );



    /**
     * Retourne toutes les configurations avec une exclusion encore active (NOW < date_fin).
     */
    @Query("""
        SELECT sc FROM SemoisConfiguration sc
        JOIN FETCH sc.produit p
        WHERE sc.exclusionDate IS NOT NULL
          AND sc.exclusionDate > :horizon
        ORDER BY sc.exclusionDate ASC
        """)
    List<SemoisConfiguration> findExclusionsActives(@Param("horizon") LocalDateTime horizon);

    /**
     * Compte le nombre d'exclusions actuellement actives.
     */
    @Query(value = """
        SELECT COUNT(*) FROM semois_configuration
        WHERE exclusion_date IS NOT NULL
          AND NOW() < exclusion_date + (COALESCE(exclusion_duree_jours, 30) || ' days')::INTERVAL
        """, nativeQuery = true)
    long countExclusionsActives();



    /**
     * Réintègre en masse les exclusions expirées (met exclusion_date à NULL).
     *
     * @return Nombre de configurations réintégrées
     */
    @Modifying
    @Query(value = """
        UPDATE semois_configuration
        SET exclusion_date = NULL,
            exclusion_motif = NULL,
            updated_at = NOW()
        WHERE exclusion_date IS NOT NULL
          AND NOW() >= exclusion_date + (COALESCE(exclusion_duree_jours, 30) || ' days')::INTERVAL
        """, nativeQuery = true)
    int reintegrerExclusionsExpirees();
}

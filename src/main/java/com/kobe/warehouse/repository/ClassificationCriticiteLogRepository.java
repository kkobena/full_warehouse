package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ClassificationCriticiteLog;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ClassificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour ClassificationCriticiteLog.
 * Gère l'historique des changements de classe de criticité des produits.
 */
@Repository
public interface ClassificationCriticiteLogRepository extends JpaRepository<ClassificationCriticiteLog, Integer> {

    /**
     * Trouve les logs d'un produit donné
     *
     * @param produitId ID du produit
     * @return Liste des logs ordonnés par date décroissante
     */
    List<ClassificationCriticiteLog> findByProduitIdOrderByCreatedAtDesc(Integer produitId);

    /**
     * Trouve les logs paginés d'un produit
     *
     * @param produitId ID du produit
     * @param pageable Pagination
     * @return Page de logs
     */
    Page<ClassificationCriticiteLog> findByProduitIdOrderByCreatedAtDesc(Integer produitId, Pageable pageable);

    /**
     * Trouve le dernier log d'un produit
     *
     * @param produitId ID du produit
     * @return Le dernier log si existant
     */
    Optional<ClassificationCriticiteLog> findFirstByProduitIdOrderByCreatedAtDesc(Integer produitId);

    /**
     * Trouve les logs par type de classification
     *
     * @param classificationType Type de classification (AUTO, MANUAL, INITIAL)
     * @param pageable Pagination
     * @return Page de logs
     */
    Page<ClassificationCriticiteLog> findByClassificationTypeOrderByCreatedAtDesc(
        ClassificationType classificationType, Pageable pageable);

    /**
     * Trouve les logs dans une période donnée
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @param pageable Pagination
     * @return Page de logs
     */
    @Query("""
        SELECT l FROM ClassificationCriticiteLog l
        WHERE l.createdAt >= :startDate AND l.createdAt <= :endDate
        ORDER BY l.createdAt DESC
        """)
    Page<ClassificationCriticiteLog> findByPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);

    /**
     * Compte le nombre de changements par nouvelle classe
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Liste de [classe, count]
     */
    @Query("""
        SELECT l.nouvelleClasse, COUNT(l) FROM ClassificationCriticiteLog l
        WHERE l.createdAt >= :startDate AND l.createdAt <= :endDate
        GROUP BY l.nouvelleClasse
        ORDER BY l.nouvelleClasse
        """)
    List<Object[]> countByNouvelleClasse(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Compte le nombre de changements par type de transition (ex: B->A, C->B)
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Liste de [ancienneClasse, nouvelleClasse, count]
     */
    @Query("""
        SELECT l.ancienneClasse, l.nouvelleClasse, COUNT(l) FROM ClassificationCriticiteLog l
        WHERE l.createdAt >= :startDate AND l.createdAt <= :endDate
          AND l.ancienneClasse IS NOT NULL
        GROUP BY l.ancienneClasse, l.nouvelleClasse
        ORDER BY l.ancienneClasse, l.nouvelleClasse
        """)
    List<Object[]> countByTransition(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Trouve les produits ayant changé de classe dans une période (dernière classification uniquement)
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Liste de logs (un par produit)
     */
    @Query("""
        SELECT l FROM ClassificationCriticiteLog l
        WHERE l.createdAt >= :startDate AND l.createdAt <= :endDate
          AND l.ancienneClasse IS NOT NULL
          AND l.ancienneClasse <> l.nouvelleClasse
        ORDER BY l.createdAt DESC
        """)
    List<ClassificationCriticiteLog> findChangementsInPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Trouve les promotions (changement vers une classe supérieure)
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @param pageable Pagination
     * @return Page de logs de promotion
     */
    @Query("""
        SELECT l FROM ClassificationCriticiteLog l
        WHERE l.createdAt >= :startDate AND l.createdAt <= :endDate
          AND l.ancienneClasse IS NOT NULL
          AND (
            (l.ancienneClasse = 'D' AND l.nouvelleClasse IN ('C', 'B', 'A', 'A_PLUS')) OR
            (l.ancienneClasse = 'C' AND l.nouvelleClasse IN ('B', 'A', 'A_PLUS')) OR
            (l.ancienneClasse = 'B' AND l.nouvelleClasse IN ('A', 'A_PLUS')) OR
            (l.ancienneClasse = 'A' AND l.nouvelleClasse = 'A_PLUS')
          )
        ORDER BY l.createdAt DESC
        """)
    Page<ClassificationCriticiteLog> findPromotionsInPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);

    /**
     * Trouve les rétrogradations (changement vers une classe inférieure)
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @param pageable Pagination
     * @return Page de logs de rétrogradation
     */
    @Query("""
        SELECT l FROM ClassificationCriticiteLog l
        WHERE l.createdAt >= :startDate AND l.createdAt <= :endDate
          AND l.ancienneClasse IS NOT NULL
          AND (
            (l.ancienneClasse = 'A_PLUS' AND l.nouvelleClasse IN ('A', 'B', 'C', 'D')) OR
            (l.ancienneClasse = 'A' AND l.nouvelleClasse IN ('B', 'C', 'D')) OR
            (l.ancienneClasse = 'B' AND l.nouvelleClasse IN ('C', 'D')) OR
            (l.ancienneClasse = 'C' AND l.nouvelleClasse = 'D')
          )
        ORDER BY l.createdAt DESC
        """)
    Page<ClassificationCriticiteLog> findRetrogradationsInPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);

    /**
     * Compte le nombre total de changements dans une période
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Nombre de changements
     */
    @Query("""
        SELECT COUNT(l) FROM ClassificationCriticiteLog l
        WHERE l.createdAt >= :startDate AND l.createdAt <= :endDate
          AND l.ancienneClasse IS NOT NULL
          AND l.ancienneClasse <> l.nouvelleClasse
        """)
    long countChangementsInPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Supprime les logs d'un produit (usage admin uniquement)
     *
     * @param produitId ID du produit
     * @return Nombre de logs supprimés
     */
    long deleteByProduitId(Integer produitId);
}

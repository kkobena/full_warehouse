package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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

    /**
     * Vérifie si un produit a une configuration SEMOIS
     *
     * @param produitId ID du produit
     * @return true si le produit a une configuration
     */
    boolean existsByProduitId(Integer produitId);

    /**
     * Liste toutes les configurations d'une classe de criticité
     *
     * @param classeCriticite Classe de criticité (A+, A, B, C, D)
     * @return Liste des configurations
     */
    List<SemoisConfiguration> findByClasseCriticite(ClasseCriticite classeCriticite);

    /**
     * Compte le nombre de produits par classe de criticité
     *
     * @return Map des classes avec leur comptage
     */
    @Query("""
        SELECT sc.classeCriticite, COUNT(sc) FROM SemoisConfiguration sc
        GROUP BY sc.classeCriticite
        ORDER BY sc.classeCriticite ASC
        """)
    List<Object[]> countByClasseCriticite();

    /**
     * Trouve les configurations nécessitant un recalcul
     * (date dernier calcul > 24h ou null)
     *
     * @return Liste des configurations à recalculer
     */
    @Query(value = """
        SELECT * FROM semois_configuration sc
        WHERE sc.date_dernier_calcul IS NULL
           OR sc.date_dernier_calcul < CURRENT_TIMESTAMP - INTERVAL '1 day'
        ORDER BY sc.date_dernier_calcul ASC NULLS FIRST
        """, nativeQuery = true)
    List<SemoisConfiguration> findConfigurationsNeedingRecalculation();

    /**
     * Trouve les produits avec stock objectif calculé < stock actuel (surstock potentiel)
     *
     * @return Liste des configurations en surstock
     */
    @Query("""
        SELECT sc FROM SemoisConfiguration sc
        JOIN sc.produit p
        JOIN p.stockProduits sp
        WHERE sc.stockObjectifCalcule IS NOT NULL
          AND sc.stockObjectifCalcule < (SELECT SUM(sp2.qtyStock + sp2.qtyUG)
                                          FROM StockProduit sp2
                                          WHERE sp2.produit.id = p.id)
        """)
    List<SemoisConfiguration> findSurstockConfigurations();

    /**
     * Trouve les produits avec limite péremption activée
     *
     * @return Liste des configurations avec limite péremption
     */
    List<SemoisConfiguration> findByLimitePeremptionTrue();

    /**
     * Supprime la configuration d'un produit (usage admin uniquement)
     *
     * @param produitId ID du produit
     * @return Nombre de configurations supprimées (0 ou 1)
     */
    long deleteByProduitId(Integer produitId);

    /**
     * Récupère les configurations avec facteur saisonnier actif (≠ 1.0)
     *
     * @return Liste des configurations avec ajustement saisonnier
     */
    @Query("""
        SELECT sc FROM SemoisConfiguration sc
        WHERE sc.facteurSaisonnierActuel IS NOT NULL
          AND sc.facteurSaisonnierActuel <> 1.0
        """)
    List<SemoisConfiguration> findConfigurationsWithSeasonalFactor();

    /**
     * Récupère les configurations par plage de délai de livraison
     *
     * @param delaiMin Délai minimum (inclus)
     * @param delaiMax Délai maximum (inclus)
     * @return Liste des configurations
     */
    @Query("""
        SELECT sc FROM SemoisConfiguration sc
        WHERE sc.delaiLivraisonJours >= :delaiMin
          AND sc.delaiLivraisonJours <= :delaiMax
        ORDER BY sc.delaiLivraisonJours DESC
        """)
    List<SemoisConfiguration> findByDelaiLivraisonBetween(@Param("delaiMin") Integer delaiMin,
                                                           @Param("delaiMax") Integer delaiMax);

    /**
     * Récupère toutes les configurations avec eager fetch du Produit (pour batch processing).
     * Résout le problème "Could not initialize proxy - no Session" en chargeant
     * le Produit dans la même requête via JOIN FETCH.
     *
     * @param pageable Pagination
     * @return Page de configurations avec produits chargés
     */
    @Query(value = """
        SELECT sc FROM SemoisConfiguration sc
        JOIN FETCH sc.produit p
        """,
        countQuery = """
        SELECT COUNT(sc) FROM SemoisConfiguration sc
        """)
    Page<SemoisConfiguration> findAllWithProduit(Pageable pageable);

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


}

package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ProduitMetriquesClassification;
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
 * Repository pour la vue v_produit_metriques_classification.
 *
 * LECTURE SEULE: Cette vue est calculée dynamiquement par PostgreSQL.
 * Ne jamais tenter d'écrire dans cette vue.
 */
@Repository
public interface ProduitMetriquesClassificationRepository extends JpaRepository<ProduitMetriquesClassification, Integer> {

    /**
     * Trouve les métriques d'un produit par son ID
     *
     * @param produitId ID du produit
     * @return Les métriques du produit
     */
    Optional<ProduitMetriquesClassification> findByProduitId(Integer produitId);

    /**
     * Récupère tous les produits paginés pour la reclassification
     *
     * @param pageable Pagination
     * @return Page de métriques produits
     */
    @Query("SELECT p FROM ProduitMetriquesClassification p ORDER BY p.produitId")
    Page<ProduitMetriquesClassification> findAllForClassification(Pageable pageable);

    /**
     * Récupère les produits par classe de criticité actuelle
     *
     * @param classe Classe de criticité
     * @param pageable Pagination
     * @return Page de produits de cette classe
     */
    Page<ProduitMetriquesClassification> findByClasseActuelle(ClasseCriticite classe, Pageable pageable);

    /**
     * Récupère les nouveaux produits (ancienneté < seuil mois)
     *
     * @param seuilMois Seuil d'ancienneté en mois
     * @param pageable Pagination
     * @return Page de nouveaux produits
     */
    @Query("SELECT p FROM ProduitMetriquesClassification p WHERE p.ancienneteMois < :seuilMois ORDER BY p.produitId")
    Page<ProduitMetriquesClassification> findNouveauxProduits(@Param("seuilMois") int seuilMois, Pageable pageable);

    /**
     * Récupère les produits éligibles à la reclassification
     * (ancienneté >= seuil et non nouveaux)
     *
     * @param seuilMois Seuil d'ancienneté en mois
     * @param pageable Pagination
     * @return Page de produits éligibles
     */
    @Query("SELECT p FROM ProduitMetriquesClassification p WHERE p.ancienneteMois >= :seuilMois ORDER BY p.produitId")
    Page<ProduitMetriquesClassification> findProduitsEligibles(@Param("seuilMois") int seuilMois, Pageable pageable);

    /**
     * Compte les produits par classe de criticité
     *
     * @return Liste de [classe, count]
     */
    @Query("SELECT p.classeActuelle, COUNT(p) FROM ProduitMetriquesClassification p GROUP BY p.classeActuelle ORDER BY p.classeActuelle")
    List<Object[]> countByClasse();

    /**
     * Récupère les IDs des produits pour la reclassification par batch
     *
     * @param pageable Pagination
     * @return Liste des IDs de produits
     */
    @Query("SELECT p.produitId FROM ProduitMetriquesClassification p ORDER BY p.produitId")
    List<Integer> findAllProduitIds(Pageable pageable);

    /**
     * Compte le nombre total de produits dans la vue
     *
     * @return Nombre de produits
     */
    @Query("SELECT COUNT(p) FROM ProduitMetriquesClassification p")
    long countAll();

    /**
     * Récupère les produits avec CA > 0 sur 12 mois
     *
     * @param pageable Pagination
     * @return Page de produits avec ventes
     */
    @Query("SELECT p FROM ProduitMetriquesClassification p WHERE p.ca12Mois > 0 ORDER BY p.ca12Mois DESC")
    Page<ProduitMetriquesClassification> findProduitsAvecVentes(Pageable pageable);

    /**
     * Récupère les produits sans ventes sur 12 mois
     *
     * @param pageable Pagination
     * @return Page de produits sans ventes
     */
    @Query("SELECT p FROM ProduitMetriquesClassification p WHERE p.ca12Mois = 0 OR p.ca12Mois IS NULL ORDER BY p.produitId")
    Page<ProduitMetriquesClassification> findProduitsSansVentes(Pageable pageable);

    /**
     * Compte les produits avec un CA inférieur à la valeur donnée
     * Utilisé pour calculer le percentile du CA
     *
     * @param ca Valeur de CA à comparer
     * @return Nombre de produits avec CA inférieur
     */
    @Query("SELECT COUNT(p) FROM ProduitMetriquesClassification p WHERE p.ca12Mois < :ca")
    long countByCa12MoisLessThan(@Param("ca") Long ca);
}

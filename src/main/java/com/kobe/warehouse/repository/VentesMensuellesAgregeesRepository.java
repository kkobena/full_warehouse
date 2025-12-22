package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour VentesMensuellesAgregees.
 * Gère les agrégations mensuelles des ventes pour le calcul SEMOIS.
 */
@Repository
public interface VentesMensuellesAgregeesRepository extends JpaRepository<VentesMensuellesAgregees, Integer> {

    /**
     * Trouve l'agrégation pour un produit et un mois donnés
     *
     * @param produitId ID du produit
     * @param anneeMois Mois au format YYYY-MM
     * @return L'agrégation si trouvée
     */
    Optional<VentesMensuellesAgregees> findByProduitIdAndAnneeMois(Integer produitId, String anneeMois);

    /**
     * Récupère les N derniers mois de ventes pour un produit
     * Trié par mois décroissant (plus récent en premier)
     *
     * @param produitId ID du produit
     * @param nbMois Nombre de mois à récupérer
     * @return Liste des agrégations
     */
    @Query("""
        SELECT vma FROM VentesMensuellesAgregees vma
        WHERE vma.produit.id = :produitId
        ORDER BY vma.anneeMois DESC
        LIMIT :nbMois
        """)
    List<VentesMensuellesAgregees> findLastNMonthsByProduit(@Param("produitId") Integer produitId,
                                                             @Param("nbMois") int nbMois);

    /**
     * Récupère les ventes entre deux mois inclus pour un produit
     *
     * @param produitId ID du produit
     * @param moisDebut Mois de début (inclus)
     * @param moisFin Mois de fin (inclus)
     * @return Liste des agrégations triées par mois croissant
     */
    @Query("""
        SELECT vma FROM VentesMensuellesAgregees vma
        WHERE vma.produit.id = :produitId
          AND vma.anneeMois >= :moisDebut
          AND vma.anneeMois <= :moisFin
        ORDER BY vma.anneeMois ASC
        """)
    List<VentesMensuellesAgregees> findByProduitBetweenMonths(@Param("produitId") Integer produitId,
                                                               @Param("moisDebut") String moisDebut,
                                                               @Param("moisFin") String moisFin);

    /**
     * Liste tous les mois gelés
     *
     * @return Liste des agrégations de mois gelés
     */
    List<VentesMensuellesAgregees> findByIsFrozenTrue();

    /**
     * Liste tous les mois non gelés (modifiables)
     *
     * @return Liste des agrégations de mois non gelés
     */
    List<VentesMensuellesAgregees> findByIsFrozenFalse();

    /**
     * Trouve tous les produits avec des ventes pour un mois donné
     *
     * @param anneeMois Mois au format YYYY-MM
     * @return Liste des agrégations pour ce mois
     */
    List<VentesMensuellesAgregees> findByAnneeMois(String anneeMois);

    /**
     * Vérifie si un mois est gelé pour au moins un produit
     *
     * @param anneeMois Mois au format YYYY-MM
     * @return true si le mois contient au moins une entrée gelée
     */
    @Query("""
        SELECT COUNT(vma) > 0 FROM VentesMensuellesAgregees vma
        WHERE vma.anneeMois = :anneeMois
          AND vma.isFrozen = TRUE
        """)
    boolean isMonthFrozen(@Param("anneeMois") String anneeMois);

    /**
     * Compte le nombre d'agrégations pour un mois donné
     *
     * @param anneeMois Mois au format YYYY-MM
     * @return Nombre d'agrégations
     */
    long countByAnneeMois(String anneeMois);

    /**
     * Supprime toutes les agrégations d'un mois (usage admin uniquement)
     *
     * @param anneeMois Mois au format YYYY-MM
     * @return Nombre de lignes supprimées
     */
    long deleteByAnneeMois(String anneeMois);

    /**
     * Récupère les mois avec données pour un produit
     *
     * @param produitId ID du produit
     * @return Liste des mois (YYYY-MM) ayant des données
     */
    @Query("""
        SELECT DISTINCT vma.anneeMois FROM VentesMensuellesAgregees vma
        WHERE vma.produit.id = :produitId
        ORDER BY vma.anneeMois DESC
        """)
    List<String> findDistinctMonthsByProduit(@Param("produitId") Integer produitId);
}

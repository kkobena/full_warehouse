package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;

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

    /**
     * Dégèle un mois (usage admin uniquement).
     * Remet is_frozen à FALSE et freeze_date à NULL.
     *
     * @param anneeMois Mois au format YYYY-MM
     * @return Nombre de lignes modifiées
     */
    @Modifying
    @Query(value = """
        UPDATE ventes_mensuelles_agregees
        SET is_frozen = FALSE,
            freeze_date = NULL,
            updated_at = NOW()
        WHERE annee_mois = :anneeMois
        """, nativeQuery = true)
    int unfreezeMonth(@Param("anneeMois") String anneeMois);

    /**
     * Agrège ou met à jour les ventes d'un mois.
     * Utilise INSERT ... ON CONFLICT pour gérer les updates.
     * Prend en compte les ventes de produits DETAIL (enfants) en les convertissant en quantité parent.
     *
     * @param anneeMois Mois au format YYYY-MM
     * @param debut Date de début du mois
     * @param fin Date de fin du mois (premier jour du mois suivant)
     * @param freeze Si true, gèle définitivement le mois
     * @return Nombre de lignes affectées
     */
    @Modifying
    @Query(value = """
        WITH sales_base AS (
            SELECT
                sli.produit_id,
                SUM(sli.quantity_requested) AS qty_sold,
                SUM(sli.sales_amount) AS montant_ca,
                COUNT(DISTINCT s.id) AS nombre_ventes
            FROM sales_line sli
            JOIN sales s ON s.id = sli.sales_id
            WHERE s.sale_date >= :debut
              AND s.sale_date < :fin
              AND s.statut = 'CLOSED'
              AND s.canceled = FALSE
            GROUP BY sli.produit_id
        ),
        sales_detail AS (
            SELECT
                pd.parent_id,
                SUM(sli.quantity_requested) AS qty_sold_detail
            FROM sales_line sli
            JOIN sales s ON s.id = sli.sales_id
            JOIN produit pd ON pd.id = sli.produit_id
            WHERE s.sale_date >= :debut
              AND s.sale_date < :fin
              AND s.statut = 'CLOSED'
              AND s.canceled = FALSE
              AND pd.type_produit = 'DETAIL'
            GROUP BY pd.parent_id
        )
        INSERT INTO ventes_mensuelles_agregees (
            produit_id,
            annee_mois,
            quantite_vendue,
            montant_ca,
            nombre_ventes,
            is_frozen,
            freeze_date,
            created_at,
            updated_at
        )
        SELECT
            p.id,
            :anneeMois,
            COALESCE(sb.qty_sold, 0) +
            COALESCE(CEIL(sd.qty_sold_detail::numeric / NULLIF(p.item_qty, 0)), 0) AS quantite_vendue,
            COALESCE(sb.montant_ca, 0) AS montant_ca,
            COALESCE(sb.nombre_ventes, 0) AS nombre_ventes,
            :freeze,
            CASE WHEN :freeze = TRUE THEN NOW() ELSE NULL END,
            NOW(),
            NOW()
        FROM produit p
        LEFT JOIN sales_base sb ON sb.produit_id = p.id
        LEFT JOIN sales_detail sd ON sd.parent_id = p.id
        WHERE p.status = 'ENABLE'
          AND p.type_produit = 'PACKAGE'
          AND (sb.qty_sold IS NOT NULL OR sd.qty_sold_detail IS NOT NULL)
        ON CONFLICT (produit_id, annee_mois)
        DO UPDATE SET
            quantite_vendue = CASE
                WHEN ventes_mensuelles_agregees.is_frozen = TRUE
                THEN ventes_mensuelles_agregees.quantite_vendue
                ELSE EXCLUDED.quantite_vendue
            END,
            montant_ca = CASE
                WHEN ventes_mensuelles_agregees.is_frozen = TRUE
                THEN ventes_mensuelles_agregees.montant_ca
                ELSE EXCLUDED.montant_ca
            END,
            nombre_ventes = CASE
                WHEN ventes_mensuelles_agregees.is_frozen = TRUE
                THEN ventes_mensuelles_agregees.nombre_ventes
                ELSE EXCLUDED.nombre_ventes
            END,
            is_frozen = EXCLUDED.is_frozen,
            freeze_date = EXCLUDED.freeze_date,
            updated_at = NOW()
        """, nativeQuery = true)
    int aggregateOrUpdateMonth(
        @Param("anneeMois") String anneeMois,
        @Param("debut") java.time.LocalDate debut,
        @Param("fin") java.time.LocalDate fin,
        @Param("freeze") boolean freeze
    );
}

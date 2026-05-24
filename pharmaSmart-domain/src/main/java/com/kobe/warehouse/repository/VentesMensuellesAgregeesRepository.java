package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * Axe 4 SEMOIS — Batch-chargement des données saisonnières.
     * Charge les ventes pour une liste de produits et une liste de mois spécifiques
     * (ex: même mois N-1 et N-2) en une seule requête SQL.
     * Utilisé pour calculer le coefficient saisonnier dans {@code SemoisCalculationService}.
     *
     * @param produitIds IDs des produits du batch courant
     * @param months     Mois cibles au format YYYY-MM (ex: ["2025-03", "2024-03"])
     * @return Ventes pour ces produits et ces mois spécifiques
     */
    @Query("""
        SELECT vma FROM VentesMensuellesAgregees vma
        JOIN FETCH vma.produit p
        WHERE p.id IN :produitIds
          AND vma.anneeMois IN :months
        """)
    List<VentesMensuellesAgregees> findByProduitIdInAndAnneeMoisIn(
        @Param("produitIds") Set<Integer> produitIds,
        @Param("months") Set<String> months
    );

    /**
     * Charge en une seule requête toutes les agrégations mensuelles d'un lot de produits.
     * Utilisé par le batch SEMOIS pour calculer la VMM pondérée en mémoire (élimine le N+1).
     *
     * @param produitIds IDs des produits du batch courant
     * @return Toutes les agrégations mensuelles de ces produits
     */
    @Query("SELECT vma FROM VentesMensuellesAgregees vma WHERE vma.produit.id IN :produitIds")
    List<VentesMensuellesAgregees> findAllByProduitIdIn(@Param("produitIds") Collection<Integer> produitIds);

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
     * Récupère les N derniers mois VALIDES (hors rupture fournisseur) pour un produit.
     * Ces mois sont utilisés pour le calcul VMM SEMOIS afin d'exclure les mois biais.
     * <p>
     * Un mois de rupture (est_rupture_fournisseur = TRUE) est exclu car les ventes
     * auraient été plus élevées sans la rupture, et les inclure sous-estime la VMM.
     * </p>
     *
     * @param produitId ID du produit
     * @param nbMois    Nombre de mois valides souhaités
     * @return Liste des agrégations hors rupture, triée par mois décroissant
     */
    @Query("""
        SELECT vma FROM VentesMensuellesAgregees vma
        WHERE vma.produit.id = :produitId
          AND vma.estRuptureFournisseur = FALSE
        ORDER BY vma.anneeMois DESC
        LIMIT :nbMois
        """)
    List<VentesMensuellesAgregees> findLastNValidMonthsByProduit(@Param("produitId") Integer produitId,
                                                                  @Param("nbMois") int nbMois);

    /**
     * Recalcule le statut de rupture fournisseur pour un mois donné.
     * <p>
     * Met à jour {@code est_rupture_fournisseur} dans les deux sens :
     * <ul>
     *   <li><b>TRUE</b> si le produit a une rupture encore active ({@code product_still_out_of_stock = TRUE})
     *       qui avait débuté avant ou pendant ce mois ({@code date_mtv < :fin})</li>
     *   <li><b>FALSE</b> dans tous les autres cas :
     *     <ul>
     *       <li>Aucune rupture pour ce produit</li>
     *       <li>Rupture résolue ({@code product_still_out_of_stock = FALSE}) — données de ventes valides</li>
     *       <li>Rupture démarrée après ce mois — le mois n'était pas impacté</li>
     *     </ul>
     *   </li>
     * </ul>
     * Seuls les mois <b>non gelés</b> ({@code is_frozen = FALSE}) sont mis à jour.
     * Les mois gelés sont immuables : leur statut de rupture est figé définitivement lors du gel.
     * </p>
     * <p>
     * Le retour à FALSE est automatique : dès que {@code productStillOutOfStock} passe à FALSE
     * (stock réapprovisionné), la prochaine agrégation mensuelle réinitialise le flag.
     * </p>
     *
     * @param anneeMois Mois au format YYYY-MM
     * @param debut     Premier jour du mois (inclusif) — non utilisé dans le filtre mais gardé pour cohérence
     * @param fin       Premier jour du mois suivant (exclusif) — borne de début de rupture
     * @return Nombre de lignes mises à jour
     */
    @Modifying
    @Query(value = """
        UPDATE ventes_mensuelles_agregees vma
        SET est_rupture_fournisseur = EXISTS (
                SELECT 1
                FROM   rupture r
                WHERE  r.produit_id                = vma.produit_id
                  AND  r.product_still_out_of_stock = TRUE
                  AND  r.date_mtv                  < :fin
            ),
            updated_at = NOW()
        WHERE vma.annee_mois = :anneeMois
          AND vma.is_frozen  = FALSE
        """, nativeQuery = true)
    int refreshRuptureStatus(
        @Param("anneeMois") String anneeMois,
        @Param("debut") LocalDate debut,
        @Param("fin") LocalDate fin
    );

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

    /**
     * Récupère la date de la dernière mise à jour dans la table
     *
     * @return Date et heure de la dernière mise à jour
     */

    @Query(
        value = "SELECT v.updated_at FROM ventes_mensuelles_agregees v ORDER BY v.updated_at DESC LIMIT 1",
        nativeQuery = true
    )
    LocalDateTime findTop1UpdatedAt();
}

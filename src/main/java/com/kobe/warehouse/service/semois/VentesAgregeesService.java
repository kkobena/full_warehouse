package com.kobe.warehouse.service.semois;

import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Service d'agrégation des ventes mensuelles pour la méthode SEMOIS.
 * Gère l'agrégation quotidienne automatique et le gel progressif des mois.
 * <p>
 * Stratégie:
 * - Mois en cours: Recalcul quotidien (volatile)
 * - Mois-1 (J+0 à J+7): Recalcul quotidien (fenêtre de stabilisation)
 * - Mois-1 (J+8): Gel définitif (immuable)
 * - Mois-2 et antérieurs: Gelés, non modifiés
 */
@Service
@Transactional
public class VentesAgregeesService {

    private static final Logger LOG = LoggerFactory.getLogger(VentesAgregeesService.class);

    private final EntityManager entityManager;
    private final VentesMensuellesAgregeesRepository ventesAgregeesRepository;

    @Value("${pharma-smart.semois.freeze-delay-days:7}")
    private int freezeDelayDays;

    public VentesAgregeesService(
        EntityManager entityManager,
        VentesMensuellesAgregeesRepository ventesAgregeesRepository
    ) {
        this.entityManager = entityManager;
        this.ventesAgregeesRepository = ventesAgregeesRepository;
    }

    /**
     * Agrégation quotidienne automatique.
     * Exécuté selon le cron configuré dans application.yml.
     * Par défaut: tous les jours à 2h du matin (heure creuse).
     */
    @Scheduled(cron = "${pharma-smart.semois.aggregation-cron:0 0 2 * * *}")
    public void aggregateMonthlySalesDaily() {
        LOG.info("🔄 Début agrégation quotidienne des ventes mensuelles SEMOIS");

        YearMonth now = YearMonth.now();

        try {
            // 1. Toujours recalculer le mois en cours (volatile)
            aggregateOrUpdateMonth(now, false);
            LOG.info("✅ Mois en cours {} mis à jour", now);

            // 2. Gérer le mois précédent avec fenêtre de stabilisation
            YearMonth lastMonth = now.minusMonths(1);
            LocalDate endOfLastMonth = lastMonth.atEndOfMonth();
            long daysSinceEnd = ChronoUnit.DAYS.between(endOfLastMonth, LocalDate.now());

            if (daysSinceEnd <= freezeDelayDays) {
                // Encore dans la fenêtre de correction
                aggregateOrUpdateMonth(lastMonth, false);
                LOG.info("⏳ Mois {} mis à jour (fenêtre stabilisation: J+{})",
                         lastMonth, daysSinceEnd);
            } else if (daysSinceEnd == freezeDelayDays + 1) {
                // Dernier recalcul puis gel définitif
                aggregateOrUpdateMonth(lastMonth, true);
                LOG.warn("🔒 GEL DÉFINITIF du mois {}", lastMonth);
            } else {
                LOG.debug("Mois {} déjà gelé, ignoré", lastMonth);
            }

            LOG.info("✅ Agrégation quotidienne SEMOIS terminée avec succès");
        } catch (Exception e) {
            LOG.error("❌ Erreur lors de l'agrégation quotidienne SEMOIS", e);
            throw e;
        }
    }

    /**
     * Agrège ou met à jour les ventes d'un mois.
     * Utilise INSERT ... ON CONFLICT pour gérer les updates.
     * Prend en compte les ventes de produits DETAIL (enfants) en les convertissant en quantité parent.
     *
     * @param mois Le mois à agréger (format YearMonth)
     * @param freeze Si true, gèle définitivement le mois
     */
    public void aggregateOrUpdateMonth(YearMonth mois, boolean freeze) {
        String anneeMois = mois.toString();
        LocalDate debut = mois.atDay(1);
        LocalDate fin = mois.plusMonths(1).atDay(1);

        String sql = """
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
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("anneeMois", anneeMois);
        query.setParameter("debut", debut);
        query.setParameter("fin", fin);
        query.setParameter("freeze", freeze);

        int rowsAffected = query.executeUpdate();

        LOG.info("Mois {} : {} produits agrégés (freeze={})",
                 anneeMois, rowsAffected, freeze);
    }

    /**
     * Dégel exceptionnel d'un mois (usage admin uniquement).
     * Utilisé uniquement en cas de correction exceptionnelle.
     *
     * @param mois Le mois à dégeler
     * @param reason Raison du dégel (pour audit)
     */
    @Transactional
    public void unfreezeMonth(YearMonth mois, String reason) {
        String sql = """
            UPDATE ventes_mensuelles_agregees
            SET is_frozen = FALSE,
                freeze_date = NULL,
                updated_at = NOW()
            WHERE annee_mois = :anneeMois
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("anneeMois", mois.toString());

        int rows = query.executeUpdate();

        LOG.warn("⚠️ DÉGEL EXCEPTIONNEL mois {} : {} produits dégelés. Raison: {}",
                 mois, rows, reason);
    }

    /**
     * Vérifie si un mois est gelé pour au moins un produit.
     *
     * @param mois Le mois à vérifier
     * @return true si le mois contient des entrées gelées
     */
    @Transactional(readOnly = true)
    public boolean isMonthFrozen(YearMonth mois) {
        return ventesAgregeesRepository.isMonthFrozen(mois.toString());
    }

    /**
     * Récupère l'agrégation pour un produit et un mois donnés.
     *
     * @param produitId ID du produit
     * @param mois Le mois
     * @return L'agrégation si elle existe
     */
    @Transactional(readOnly = true)
    public Optional<VentesMensuellesAgregees> getAgregationForMonth(Integer produitId, YearMonth mois) {
        return ventesAgregeesRepository.findByProduitIdAndAnneeMois(produitId, mois.toString());
    }

    /**
     * Import historique des N derniers mois.
     * À exécuter UNE SEULE FOIS lors du déploiement initial.
     *
     * @param nbMois Nombre de mois à importer (recommandé: 12)
     */
    @Transactional
    public void importHistoricalMonths(int nbMois) {
        LOG.info("🔄 Import historique {} derniers mois...", nbMois);

        YearMonth now = YearMonth.now();

        for (int i = 1; i <= nbMois; i++) {
            YearMonth mois = now.minusMonths(i);
            // Geler tous les mois sauf le précédent (qui est dans la fenêtre de stabilisation)
            boolean shouldFreeze = i > 1;

            try {
                aggregateOrUpdateMonth(mois, shouldFreeze);
                LOG.info("✅ Mois {} importé (frozen={})", mois, shouldFreeze);
            } catch (Exception e) {
                LOG.error("❌ Erreur import mois {}", mois, e);
            }
        }

        LOG.info("✅ Import historique terminé - {} mois agrégés", nbMois);
    }

    /**
     * Compte le nombre d'agrégations pour un mois donné.
     *
     * @param mois Le mois
     * @return Nombre d'agrégations
     */
    @Transactional(readOnly = true)
    public long countAgregationsForMonth(YearMonth mois) {
        return ventesAgregeesRepository.countByAnneeMois(mois.toString());
    }

    /**
     * Récupère le délai de gel configuré.
     *
     * @return Nombre de jours de délai avant gel
     */
    public int getFreezeDelayDays() {
        return freezeDelayDays;
    }
}

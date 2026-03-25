package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private final VentesMensuellesAgregeesRepository ventesAgregeesRepository;

    @Value("${pharma-smart.semois.freeze-delay-days:7}")
    private int freezeDelayDays;

    public VentesAgregeesService(
        VentesMensuellesAgregeesRepository ventesAgregeesRepository
    ) {
        this.ventesAgregeesRepository = ventesAgregeesRepository;
    }

    /**
     * Agrégation quotidienne automatique.
     * Exécuté selon le cron configuré dans application.yml.
     * Par défaut: tous les jours à 2h du matin (heure creuse).
     */
    @Scheduled(cron = "${pharma-smart.semois.aggregation-cron:0 0 2 * * *}")
    public void aggregateMonthlySalesDaily() {
        LOG.info(" Début agrégation quotidienne des ventes mensuelles SEMOIS");

        boolean canContinue = getLastUpdateTime().map(lastUpdate ->
            lastUpdate.toLocalDate().isBefore(LocalDate.now())
        ).orElse(true);
        YearMonth now = YearMonth.now();
        if (!canContinue) {
            LOG.warn("Agrégation quotidienne SEMOIS annulée: déjà exécutée aujourd'hui à {}",
                getLastUpdateTime().get());
            return;
        }

        try {
            // 1. Toujours recalculer le mois en cours (volatile)
            aggregateOrUpdateMonth(now, false);
            LOG.info("Mois en cours {} mis à jour", now);

            // 2. Gérer le mois précédent avec fenêtre de stabilisation
            YearMonth lastMonth = now.minusMonths(1);
            LocalDate endOfLastMonth = lastMonth.atEndOfMonth();
            long daysSinceEnd = ChronoUnit.DAYS.between(endOfLastMonth, LocalDate.now());

            if (daysSinceEnd <= freezeDelayDays) {
                // Encore dans la fenêtre de correction
                aggregateOrUpdateMonth(lastMonth, false);
                LOG.info("Mois {} mis à jour (fenêtre stabilisation: J+{})",
                    lastMonth, daysSinceEnd);
            } else if (daysSinceEnd == freezeDelayDays + 1) {
                // Dernier recalcul puis gel définitif
                aggregateOrUpdateMonth(lastMonth, true);
                LOG.warn("GEL DÉFINITIF du mois {}", lastMonth);
            } else {
                LOG.debug("Mois {} déjà gelé, ignoré", lastMonth);
            }

            LOG.info("Agrégation quotidienne SEMOIS terminée avec succès");
        } catch (Exception e) {
            LOG.error("Erreur lors de l'agrégation quotidienne SEMOIS", e);
            throw e;
        }
    }

    /**
     * Agrège ou met à jour les ventes d'un mois.
     * Utilise INSERT ... ON CONFLICT pour gérer les updates.
     * Prend en compte les ventes de produits DETAIL (enfants) en les convertissant en quantité parent.
     * <p>
     * Après l'agrégation, recalcule le statut de rupture fournisseur pour les mois non-gelés :
     * <ul>
     *   <li>Si une rupture existe sur la période → {@code est_rupture_fournisseur = TRUE}</li>
     *   <li>Si plus aucune rupture sur la période (rupture résolue) → {@code est_rupture_fournisseur = FALSE}</li>
     * </ul>
     * Les mois gelés ({@code is_frozen = TRUE}) ne sont jamais modifiés.
     * </p>
     *
     * @param mois   Le mois à agréger (format YearMonth)
     * @param freeze Si true, gèle définitivement le mois
     */
    public void aggregateOrUpdateMonth(YearMonth mois, boolean freeze) {
        String anneeMois = mois.toString();
        LocalDate debut = mois.atDay(1);
        LocalDate fin = mois.plusMonths(1).atDay(1);

        int rowsAffected = ventesAgregeesRepository.aggregateOrUpdateMonth(anneeMois, debut, fin, freeze);

        // Axe 3 — Recalcul bidirectionnel du statut rupture (TRUE ou FALSE selon la réalité).
        // Seuls les mois non-gelés sont mis à jour (les mois gelés sont immuables).
        // Si une rupture a été résolue depuis la dernière agrégation, le flag revient à FALSE.
        ventesAgregeesRepository.refreshRuptureStatus(anneeMois, debut, fin);

        LOG.info("Mois {} : {} produits agrégés (freeze={})", anneeMois, rowsAffected, freeze);
    }

    /**
     * Dégel exceptionnel d'un mois (usage admin uniquement).
     * Utilisé uniquement en cas de correction exceptionnelle.
     *
     * @param mois   Le mois à dégeler
     * @param reason Raison du dégel (pour audit)
     */
    @Transactional
    public void unfreezeMonth(YearMonth mois, String reason) {
        int rows = ventesAgregeesRepository.unfreezeMonth(mois.toString());

        LOG.warn("DÉGEL EXCEPTIONNEL mois {} : {} produits dégelés. Raison: {}",
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
     * @param mois      Le mois
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
                LOG.info(" Mois {} importé (frozen={})", mois, shouldFreeze);
            } catch (Exception e) {
                LOG.error("Erreur import mois {}", mois, e);
            }
        }

        LOG.info("Import historique terminé - {} mois agrégés", nbMois);
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

    private Optional<LocalDateTime> getLastUpdateTime() {
        return Optional.ofNullable(ventesAgregeesRepository.findTop1UpdatedAt());
    }
}

package com.kobe.warehouse.service.classification;

import com.kobe.warehouse.domain.ClassificationConfig;
import com.kobe.warehouse.domain.ClassificationCriticiteLog;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ClassificationType;
import com.kobe.warehouse.repository.ClassificationCriticiteLogRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.service.scheduler.ClassificationCriticiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Traitement par batch de la classification ABC Pareto.
 *
 * <p>Ce bean est extrait de {@link ClassificationCriticiteService} pour résoudre le problème
 * du proxy Spring : {@code @Transactional(REQUIRES_NEW)} sur une méthode appelée en self-invocation
 * (this.xxx()) contourne le proxy AOP et n'ouvre pas une nouvelle transaction.
 * En délégant à ce bean séparé, le proxy fonctionne correctement.
 */
@Service
@Transactional
public class ClassificationBatchProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ClassificationBatchProcessor.class);

    /**
     * Données Pareto préchargées pour un produit.
     *
     * @param ca12Mois      CA sur 12 mois (centimes)
     * @param caCumulePct   % CA cumulé dans le Pareto (0–100, faible = produit critique)
     * @param rang          Rang Pareto (1 = plus vendu)
     * @param frequenceMois Nombre de mois distincts avec ventes sur 12 mois
     * @param qteVendue     Quantité totale vendue sur 12 mois (pour CMM = qteVendue / 12)
     */
    public record ParetoScore(Long ca12Mois, BigDecimal caCumulePct, int rang, int frequenceMois, int qteVendue) {
        static final ParetoScore NO_SALES = new ParetoScore(0L, new BigDecimal("100.00"), Integer.MAX_VALUE, 0, 0);

        /**
         * Consommation Mensuelle Moyenne (entière, arrondie au plus proche).
         */
        public int cmm() {
            return (int) Math.round(qteVendue / 12.0);
        }
    }

    private final ProduitRepository produitRepository;
    private final ClassificationCriticiteLogRepository logRepository;

    public ClassificationBatchProcessor(
        ProduitRepository produitRepository,
        ClassificationCriticiteLogRepository logRepository
    ) {
        this.produitRepository = produitRepository;
        this.logRepository = logRepository;
    }

    /**
     * Traite une page de produits dans une transaction isolée ({@code REQUIRES_NEW}).
     *
     * <p>Isolation {@code REQUIRES_NEW} assure que :
     * <ul>
     *   <li>Chaque page est commitée indépendamment : les erreurs sur une page ne rollbackent pas les autres.</li>
     *   <li>La mémoire JPA est libérée après chaque page.</li>
     * </ul>
     *
     * @param pageable          Pagination (page courante + taille)
     * @param config            Configuration des seuils Pareto / CMM / hysteresis
     * @param paretoMap         Scores Pareto préchargés (produitId → ParetoScore)
     * @param raison            Raison de la reclassification (pour le log)
     * @param nbAnalyses        Compteur de produits analysés (partagé entre pages)
     * @param nbChangements     Compteur de changements effectués
     * @param nbPromotions      Compteur de promotions
     * @param nbRetrogradations Compteur de rétrogradations
     * @param nbNouveaux        Compteur de produits nouveaux ignorés
     * @param nbOverridden      Compteur de produits avec override ignorés
     * @param nbErreurs         Compteur d'erreurs
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPage(
        Pageable pageable,
        ClassificationConfig config,
        Map<Integer, ParetoScore> paretoMap,
        String raison,
        AtomicInteger nbAnalyses,
        AtomicInteger nbChangements,
        AtomicInteger nbPromotions,
        AtomicInteger nbRetrogradations,
        AtomicInteger nbNouveaux,
        AtomicInteger nbOverridden,
        AtomicInteger nbErreurs
    ) {
        Page<Produit> page = produitRepository.findActiveNonDetailWithFournisseur(pageable);

        for (Produit produit : page.getContent()) {
            try {
                nbAnalyses.incrementAndGet();

                // 1. Produits avec override manuel → ne pas reclassifier
                if (Boolean.TRUE.equals(produit.getIsClassificationOverridden())) {
                    nbOverridden.incrementAndGet();
                    continue;
                }

                // 2. Nouveaux produits (< nbMoisMinNouveauProduit) → conserver classe par défaut
                int ancienneteMois = produit.getCreatedAt() != null
                    ? (int) ChronoUnit.MONTHS.between(produit.getCreatedAt().toLocalDate(), LocalDate.now())
                    : Integer.MAX_VALUE;
                if (ancienneteMois < config.getNbMoisMinNouveauProduit()) {
                    nbNouveaux.incrementAndGet();
                    continue;
                }

                // 3. Récupérer le score Pareto préchargé
                ParetoScore pareto = paretoMap.getOrDefault(produit.getId(), ParetoScore.NO_SALES);

                // 4. Déterminer la classe suggérée
                ClasseCriticite classeSuggeree = determinerClasse(pareto, produit, config);
                ClasseCriticite classeActuelle = produit.getEffectiveClasseCriticite();

                if (classeSuggeree == classeActuelle) {
                    continue;
                }

                // 5. Vérifier l'hysteresis
                if (!passeHysteresis(pareto.caCumulePct(), classeActuelle, classeSuggeree, config)) {
                    continue;
                }

                // 6. Appliquer le changement
                produit.setClasseCriticite(classeSuggeree);
                produitRepository.save(produit);

                ClassificationCriticiteLog log = new ClassificationCriticiteLog()
                    .setProduit(produit)
                    .setAncienneClasse(classeActuelle)
                    .setNouvelleClasse(classeSuggeree)
                    .setCa12Mois(pareto.ca12Mois())
                    .setFrequenceVenteMois(pareto.frequenceMois())
                    .setScoreTotal(pareto.caCumulePct())
                    .setRaisonChangement(raison)
                    .setClassificationType(ClassificationType.AUTO);
                logRepository.save(log);

                nbChangements.incrementAndGet();
                if (getOrdreClasse(classeSuggeree) > getOrdreClasse(classeActuelle)) {
                    nbPromotions.incrementAndGet();
                } else {
                    nbRetrogradations.incrementAndGet();
                }

                LOG.debug("Reclassification produit {}: {} → {} (caCumulé: {}%)",
                    produit.getId(), classeActuelle.getCode(), classeSuggeree.getCode(), pareto.caCumulePct());

            } catch (Exception e) {
                LOG.error("Erreur classification produit {}", produit.getId(), e);
                nbErreurs.incrementAndGet();
            }
        }
    }

    // ==================== Logique de classification ====================

    /**
     * Détermine la classe d'un produit selon le Pareto, les overrides médicaux et de garde.
     */
    public ClasseCriticite determinerClasse(ParetoScore pareto, Produit produit, ClassificationConfig config) {
        // Override de garde — toujours A_PLUS
        if (Boolean.TRUE.equals(produit.getEstProduitGarde())) {
            return ClasseCriticite.A_PLUS;
        }

        // Classe Pareto brute
        ClasseCriticite classePareto = classeDepuisPareto(pareto, config);

        // Phase 2.5 : correction saisonnière (no-op tant que activerCorrectionSaisonniere=false)
        classePareto = appliquerCorrectionSaisonniere(classePareto, config);

        // Override médicament essentiel
        if (Boolean.TRUE.equals(produit.getEstMedicamentEssentiel())) {
            if (Boolean.TRUE.equals(config.getActiverClassificationOrdo())) {
                // Classification CMM : prend le max entre Pareto et CMM-class
                ClasseCriticite classeCmm = classeDepuisCmm(pareto, config);
                return maxClasse(classePareto, classeCmm);
            } else {
                // Plancher simple : jamais en dessous de B
                return maxClasse(classePareto, ClasseCriticite.B);
            }
        }

        return classePareto;
    }

    /**
     * Détermine la classe Pareto brute d'après {@code ca_cumule_pct} et la fréquence.
     */
    private ClasseCriticite classeDepuisPareto(ParetoScore pareto, ClassificationConfig config) {
        // Fréquence insuffisante → D (ventes ponctuelles, non représentatives)
        if (pareto.frequenceMois() < config.getSeuilFrequenceMinMois()) {
            return ClasseCriticite.D;
        }
        // Pas de ventes → D
        if (pareto.ca12Mois() == 0) {
            return ClasseCriticite.D;
        }

        double pct = pareto.caCumulePct().doubleValue();
        if (pct <= config.getSeuilParetoAPlus()) return ClasseCriticite.A_PLUS;
        if (pct <= config.getSeuilParetoA()) return ClasseCriticite.A;
        if (pct <= config.getSeuilParetoB()) return ClasseCriticite.B;
        if (pct <= config.getSeuilParetoC()) return ClasseCriticite.C;
        return ClasseCriticite.D;
    }

    /**
     * Détermine la classe d'un médicament essentiel d'après sa CMM réelle.
     * CMM = qteVendue / 12 — alignée avec la méthode SEMOIS (USPO/FSPF).
     *
     * <p>Plancher B garanti : un médicament essentiel ne peut jamais être classé D,
     * même avec une CMM nulle (médicament rarement utilisé mais vital).
     */
    private ClasseCriticite classeDepuisCmm(ParetoScore pareto, ClassificationConfig config) {
        int cmm = pareto.cmm();
        if (cmm >= config.getCmmSeuilAPlus()) return ClasseCriticite.A_PLUS;
        if (cmm >= config.getCmmSeuilA()) return ClasseCriticite.A;
        if (cmm >= config.getCmmSeuilB()) return ClasseCriticite.B;
        if (cmm >= config.getCmmSeuilC()) return ClasseCriticite.C;
        // Plancher B pour médicament essentiel (jamais D)
        return ClasseCriticite.B;
    }

    /**
     * Correction saisonnière — porte activée par {@code activerCorrectionSaisonniere}.
     *
     * <p>Phase 2.5 (non encore implémentée) : lorsque la correction est activée,
     * un produit détecté comme saisonnier (indice_saisonnalite ≥ indiceSaisonnaliteMin)
     * sera reclassifié sur sa fenêtre de pic ({@code nbMoisSaisonAnalyse} meilleurs mois).
     * En attendant, cette méthode retourne la classe Pareto inchangée.
     *
     * @param classePareto Classe Pareto calculée normalement
     * @param config       Configuration avec les paramètres saisonniers
     * @return Classe ajustée (identique à classePareto jusqu'à l'implémentation de la phase 2.5)
     */
    private ClasseCriticite appliquerCorrectionSaisonniere(
        ClasseCriticite classePareto,
        ClassificationConfig config
    ) {
        if (!Boolean.TRUE.equals(config.getActiverCorrectionSaisonniere())) {
            return classePareto;
        }
        // TODO Phase 2.5 : détecter indice_saisonnalite et reclassifier sur le pic
        // Nécessite : chargement de l'indice par produit ou ajout d'une colonne dans ParetoScore
        LOG.debug("Correction saisonnière activée mais non implémentée (phase 2.5) — classe inchangée");
        return classePareto;
    }

    /**
     * Vérifie si le changement de classe dépasse le seuil d'hysteresis.
     *
     * <p>L'hysteresis est calculée comme la distance en points Pareto entre la position
     * du produit ({@code caCumulePct}) et la frontière de la classe actuelle.
     * Si le produit est clairement dans la nouvelle classe (écart > changementMinPourcentage),
     * le reclassement est autorisé.
     */
    public boolean passeHysteresis(
        BigDecimal caCumulePct,
        ClasseCriticite classeActuelle,
        ClasseCriticite classeSuggeree,
        ClassificationConfig config
    ) {
        if (caCumulePct == null) return true;
        double pct = caCumulePct.doubleValue();
        double frontiere = getFrontiereClasse(classeActuelle, config);
        return Math.abs(pct - frontiere) >= config.getChangementMinPourcentage();
    }

    /**
     * Retourne la frontière supérieure (en %) de la classe actuelle.
     * Un produit en classe A a une frontière supérieure = seuilParetoA.
     */
    private double getFrontiereClasse(ClasseCriticite classe, ClassificationConfig config) {
        return switch (classe) {
            case A_PLUS -> config.getSeuilParetoAPlus();
            case A -> config.getSeuilParetoA();
            case B -> config.getSeuilParetoB();
            case C -> config.getSeuilParetoC();
            case D -> 100.0;
        };
    }

    private ClasseCriticite maxClasse(ClasseCriticite a, ClasseCriticite b) {
        return getOrdreClasse(a) >= getOrdreClasse(b) ? a : b;
    }

    static int getOrdreClasse(ClasseCriticite classe) {
        return switch (classe) {
            case A_PLUS -> 5;
            case A -> 4;
            case B -> 3;
            case C -> 2;
            case D -> 1;
        };
    }
}

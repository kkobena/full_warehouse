package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.ClassificationConfig;
import com.kobe.warehouse.domain.ClassificationCriticiteLog;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ClassificationType;
import com.kobe.warehouse.repository.ClassificationConfigRepository;
import com.kobe.warehouse.repository.ClassificationCriticiteLogRepository;
import com.kobe.warehouse.repository.ParetoAnalysisRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.classification.ClassificationBatchProcessor;
import com.kobe.warehouse.service.classification.ClassificationBatchProcessor.ParetoScore;
import com.kobe.warehouse.service.dto.ClassificationConfigDTO;
import com.kobe.warehouse.service.dto.ClassificationLogDTO;
import com.kobe.warehouse.service.dto.ClassificationScoreDTO;
import com.kobe.warehouse.service.dto.ReclassificationResultDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Service de classification dynamique de la criticité des produits.
 *
 * <p>Implémente la méthode <b>ABC Pareto</b> conforme aux logiciels officine de référence
 * (Winpharma, Lgpi, Périscopie) : les classes sont déterminées par le pourcentage de CA cumulé
 * ({@code ca_cumule_pct}) issu de {@code v_abc_pareto_analysis}, et non par un score pondéré.
 *
 * <p>Algorithme de classification (par ordre de priorité) :
 * <ol>
 *   <li>Produit de garde → toujours A_PLUS</li>
 *   <li>Fréquence &lt; seuil → D (ventes ponctuelles non représentatives)</li>
 *   <li>Classe Pareto depuis {@code ca_cumule_pct} et les seuils configurables</li>
 *   <li>Médicament essentiel → plancher B (ou classe CMM si {@code activerClassificationOrdo})</li>
 *   <li>Hysteresis : changement ignoré si écart &lt; {@code changementMinPourcentage} points Pareto</li>
 * </ol>
 */
@Service
@Transactional
public class ClassificationCriticiteService {

    private static final String AUTO_DESCRIPTION = "Reclassification mensuelle automatique";
    private static final String APP_LAST_DAY_CLASSIFICATION = "APP_LAST_DAY_CLASSIFICATION";
    private static final Logger LOG = LoggerFactory.getLogger(ClassificationCriticiteService.class);
    private final ClassificationConfigRepository configRepository;
    private final ClassificationCriticiteLogRepository logRepository;
    private final ProduitRepository produitRepository;
    private final ParetoAnalysisRepository paretoRepository;
    private final UserRepository userRepository;
    private final ClassificationBatchProcessor batchProcessor;
    private final SemoisCalculationService semoisCalculationService;
    private final AppConfigurationService appConfigurationService;
    @Value("${pharma-smart.classification.batch-size:100}")
    private int batchSize;

    public ClassificationCriticiteService(
        ClassificationConfigRepository configRepository,
        ClassificationCriticiteLogRepository logRepository,
        ProduitRepository produitRepository,
        ParetoAnalysisRepository paretoRepository,
        UserRepository userRepository,
        ClassificationBatchProcessor batchProcessor,
        SemoisCalculationService semoisCalculationService,
        AppConfigurationService appConfigurationService
    ) {
        this.configRepository = configRepository;
        this.logRepository = logRepository;
        this.produitRepository = produitRepository;
        this.paretoRepository = paretoRepository;
        this.userRepository = userRepository;
        this.batchProcessor = batchProcessor;
        this.semoisCalculationService = semoisCalculationService;
        this.appConfigurationService = appConfigurationService;
    }

    // ==================== API publique ====================

    private static BigDecimal toBigDecimal(Object obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        if (obj instanceof BigDecimal bd) {
            return bd;
        }
        return new BigDecimal(obj.toString());
    }

    private static int toInt(Object obj) {
        if (obj == null) {
            return 0;
        }
        return ((Number) obj).intValue();
    }

    private static long toLong(Object obj) {
        if (obj == null) {
            return 0L;
        }
        return ((Number) obj).longValue();
    }

    private static Integer toInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        return ((Number) obj).intValue();
    }

    public ClassificationConfig getConfig() {
        return configRepository.findConfiguration()
            .orElseGet(this::createDefaultConfig);
    }

    public ClassificationConfigDTO getConfigDTO() {
        return ClassificationConfigDTO.fromEntity(getConfig());
    }

    /**
     * Met à jour la configuration de classification.
     */
    public ClassificationConfigDTO updateConfig(ClassificationConfigDTO dto) {
        ClassificationConfig config = getConfig();

        if (dto.seuilParetoAPlus() != null) {
            config.setSeuilParetoAPlus(dto.seuilParetoAPlus());
        }
        if (dto.seuilParetoA() != null) {
            config.setSeuilParetoA(dto.seuilParetoA());
        }
        if (dto.seuilParetoB() != null) {
            config.setSeuilParetoB(dto.seuilParetoB());
        }
        if (dto.seuilParetoC() != null) {
            config.setSeuilParetoC(dto.seuilParetoC());
        }
        if (dto.seuilFrequenceMinMois() != null) {
            config.setSeuilFrequenceMinMois(dto.seuilFrequenceMinMois());
        }
        if (dto.cmmSeuilAPlus() != null) {
            config.setCmmSeuilAPlus(dto.cmmSeuilAPlus());
        }
        if (dto.cmmSeuilA() != null) {
            config.setCmmSeuilA(dto.cmmSeuilA());
        }
        if (dto.cmmSeuilB() != null) {
            config.setCmmSeuilB(dto.cmmSeuilB());
        }
        if (dto.cmmSeuilC() != null) {
            config.setCmmSeuilC(dto.cmmSeuilC());
        }
        if (dto.changementMinPourcentage() != null) {
            config.setChangementMinPourcentage(dto.changementMinPourcentage());
        }
        if (dto.activerClassificationOrdo() != null) {
            config.setActiverClassificationOrdo(dto.activerClassificationOrdo());
        }
        if (dto.activerCorrectionSaisonniere() != null) {
            config.setActiverCorrectionSaisonniere(dto.activerCorrectionSaisonniere());
        }
        if (dto.indiceSaisonnaliteMin() != null) {
            config.setIndiceSaisonnaliteMin(dto.indiceSaisonnaliteMin());
        }
        if (dto.nbMoisSaisonAnalyse() != null) {
            config.setNbMoisSaisonAnalyse(dto.nbMoisSaisonAnalyse());
        }
        if (dto.nbMoisMinNouveauProduit() != null) {
            config.setNbMoisMinNouveauProduit(dto.nbMoisMinNouveauProduit());
        }
        if (dto.autoClassificationEnabled() != null) {
            config.setAutoClassificationEnabled(dto.autoClassificationEnabled());
        }

        return ClassificationConfigDTO.fromEntity(configRepository.save(config));
    }

    /**
     * Calcule le score de classification Pareto pour un produit (API publique). Utilisé pour la
     * preview individuelle et les overrides manuels.
     *
     * <p>Source des données : {@code Produit} (libellé, classe actuelle, ancienneté)
     * + {@code v_abc_pareto_analysis} (Pareto) + {@code stock_produit} (stock actuel). N'utilise
     * plus {@code v_produit_metriques_classification}.
     */
    public ClassificationScoreDTO calculerScore(Integer produitId) {
        ClassificationConfig config = getConfig();

        Produit produit = produitRepository.findById(produitId).orElse(null);
        if (produit == null) {
            return null;
        }

        // Scores Pareto + stock depuis v_abc_pareto_analysis (7 colonnes)
        Object[] row = paretoRepository.findByProduitId(produitId).orElse(null);
        BigDecimal caCumulePct = row != null ? toBigDecimal(row[1]) : new BigDecimal("100.00");
        int rang = row != null ? toInt(row[2]) : Integer.MAX_VALUE;
        long ca12Mois = row != null ? toLong(row[3]) : 0L;
        int frequenceMois = row != null ? toInt(row[4]) : 0;
        int qteVendue = row != null ? toInt(row[5]) : 0;
        int stockActuel = row != null ? toInt(row[6]) : 0;

        int cmm = (int) Math.round(qteVendue / 12.0);

        int ancienneteMois = produit.getCreatedAt() != null
            ? (int) java.time.temporal.ChronoUnit.MONTHS.between(
            produit.getCreatedAt().toLocalDate(), LocalDate.now())
            : 0;
        boolean estNouveauProduit = ancienneteMois < config.getNbMoisMinNouveauProduit();

        ParetoScore ps = new ParetoScore(ca12Mois, caCumulePct, rang, frequenceMois, qteVendue);
        ClasseCriticite classeSuggeree = batchProcessor.determinerClasse(ps, produit, config);

        ClasseCriticite classeActuelle = produit.getEffectiveClasseCriticite();

        boolean changementSignificatif = classeSuggeree != classeActuelle
            && batchProcessor.passeHysteresis(caCumulePct, classeActuelle, classeSuggeree, config);

        int scoreTotal = (int) Math.clamp(100.0 - caCumulePct.doubleValue(), 0, 100);

        return new ClassificationScoreDTO(
            produitId,
            produit.getLibelle(),
            ca12Mois,
            cmm,
            qteVendue,
            frequenceMois,
            caCumulePct,
            stockActuel,
            rang,
            frequenceMois,
            scoreTotal,
            classeSuggeree,
            classeActuelle,
            estNouveauProduit,
            ancienneteMois,
            changementSignificatif
        );
    }

    /**
     * Override manuel de la classe d'un produit.
     */
    @Transactional
    public void overrideClasse(Integer produitId, ClasseCriticite nouvelleClasse, String raison) {
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));

        ClassificationScoreDTO score = calculerScore(produitId);
        appliquerChangementClasse(produit, nouvelleClasse, score, raison,
            ClassificationType.MANUAL);
        produit.setIsClassificationOverridden(true);
        produitRepository.save(produit);
    }

    /**
     * Reclassifie tous les produits selon l'analyse Pareto. Exécuté mensuellement (cron horaire =
     * filet de sécurité, garde idempotente mensuelle).
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void reclassifierTousProduits() {
        reclassifierTousProduits(AUTO_DESCRIPTION);
    }

    // ==================== Méthodes privées ====================

    /**
     * Reclassifie tous les produits avec une raison personnalisée.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReclassificationResultDTO reclassifierTousProduits(String raison) {
        LOG.info("Démarrage reclassification: {}", raison);
        long startTime = System.currentTimeMillis();

        ClassificationConfig config = getConfig();

        if (!config.getAutoClassificationEnabled()) {
            LOG.warn("Classification automatique désactivée");
            return ReclassificationResultDTO.builder()
                .raison("Classification automatique désactivée")
                .dateExecution(LocalDateTime.now())
                .build();
        }

        // Garde idempotente mensuelle — marqueur dédié, indépendant de classification_config
        if (raison.equals(AUTO_DESCRIPTION) && dejaReclassifieCeMois()) {
            LOG.warn("Reclassification automatique déjà exécutée ce mois-ci");
            return ReclassificationResultDTO.builder()
                .raison("Reclassification automatique déjà exécutée ce mois-ci")
                .dateExecution(LocalDateTime.now())
                .build();
        }

        Map<String, Long> repartitionAvant = getDistributionClasses();

        // ── Chargement en mémoire des scores Pareto (une seule requête SQL) ──
        Map<Integer, ParetoScore> paretoMap = loadParetoMap();
        LOG.info("Scores Pareto chargés: {} produits", paretoMap.size());

        long totalProduits = produitRepository.countActiveNonDetail();
        int totalPages = (int) Math.ceil((double) totalProduits / batchSize);

        AtomicInteger nbAnalyses = new AtomicInteger(0);
        AtomicInteger nbChangements = new AtomicInteger(0);
        AtomicInteger nbPromotions = new AtomicInteger(0);
        AtomicInteger nbRetrogradations = new AtomicInteger(0);
        AtomicInteger nbNouveaux = new AtomicInteger(0);
        AtomicInteger nbOverridden = new AtomicInteger(0);
        AtomicInteger nbErreurs = new AtomicInteger(0);

        for (int page = 0; page < totalPages; page++) {
            try {
                batchProcessor.processPage(
                    PageRequest.of(page, batchSize),
                    config,
                    paretoMap,
                    raison,
                    nbAnalyses,
                    nbChangements,
                    nbPromotions,
                    nbRetrogradations,
                    nbNouveaux,
                    nbOverridden,
                    nbErreurs
                );
            } catch (Exception e) {
                LOG.error("Erreur lors du traitement du batch {}/{}", page + 1, totalPages, e);
                nbErreurs.incrementAndGet();
            }
        }

        Map<String, Long> repartitionApres = getDistributionClasses();
        long duree = System.currentTimeMillis() - startTime;

        ReclassificationResultDTO result = ReclassificationResultDTO.builder()
            .totalProduitsAnalyses(nbAnalyses.get())
            .totalChangements(nbChangements.get())
            .nbPromotions(nbPromotions.get())
            .nbRetrogradations(nbRetrogradations.get())
            .nbProduitsNouveaux(nbNouveaux.get())
            .nbProduitsOverridden(nbOverridden.get())
            .nbErreurs(nbErreurs.get())
            .repartitionAvant(repartitionAvant)
            .repartitionApres(repartitionApres)
            .dureeExecutionMs(duree)
            .dateExecution(LocalDateTime.now())
            .raison(raison)
            .build();

        LOG.info("Reclassification terminée: {}", result.getResume());

        if (result.hasAnomaliePotentielle()) {
            LOG.warn("ALERTE: Pourcentage de changements anormalement élevé: {}%",
                String.format("%.1f", result.getPourcentageChangements()));
        }

        // Marquer l'exécution du mois (garde d'idempotence mensuelle)
        marquerReclassificationDuMois();

        // Enchaîner le recalcul SEMOIS : les classes viennent d'être mises à jour
        LOG.info("Déclenchement recalcul SEMOIS suite à la reclassification");
        semoisCalculationService.recalculateAfterClassification();

        return result;
    }

    /**
     * Récupère les logs de classification paginés.
     */
    public Page<ClassificationLogDTO> getLogs(org.springframework.data.domain.Pageable pageable) {
        return logRepository.findAll(pageable).map(ClassificationLogDTO::fromEntity);
    }

    /**
     * Récupère les logs d'un produit.
     */
    public Page<ClassificationLogDTO> getLogsProduit(Integer produitId,
        org.springframework.data.domain.Pageable pageable) {
        return logRepository.findByProduitIdOrderByCreatedAtDesc(produitId, pageable)
            .map(ClassificationLogDTO::fromEntity);
    }

    /**
     * Récupère la distribution actuelle des classes.
     */
    public Map<String, Long> getDistributionClasses() {
        List<Object[]> results = produitRepository.countByClasseCriticite();
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (ClasseCriticite classe : ClasseCriticite.values()) {
            distribution.put(classe.getCode(), 0L);
        }
        for (Object[] row : results) {
            String classe = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            if (classe != null) {
                distribution.put(classe, count);
            }
        }
        return distribution;
    }

    private ClassificationConfig createDefaultConfig() {
        return configRepository.save(new ClassificationConfig());
    }

    /**
     * Garde d'idempotence mensuelle : retourne {@code true} si une reclassification a déjà eu lieu
     * durant le mois courant. Le marqueur est stocké dans la clé
     * {@code APP_LAST_DAY_CLASSIFICATION} — et non dans {@code classification_config.updated_at},
     * lequel est modifié à chaque édition des réglages (et posé dès la création de la config), ce
     * qui bloquait à tort l'exécution du batch.
     */
    private boolean dejaReclassifieCeMois() {
        return appConfigurationService.findOneById(APP_LAST_DAY_CLASSIFICATION)
            .map(AppConfiguration::getValue)
            .filter(StringUtils::hasText)
            .map(LocalDate::parse)
            .map(date -> date.withDayOfMonth(1).equals(LocalDate.now().withDayOfMonth(1)))
            .orElse(false);
    }

    // ── Helpers de conversion de colonnes native query ──

    /**
     * Enregistre la date du jour comme date de dernière reclassification (marqueur mensuel).
     * L'échec d'écriture du marqueur ne doit pas faire échouer le batch déjà réalisé : il est
     * journalisé sans être propagé (au pire, la reclassification sera rejouée le mois courant).
     */
    private void marquerReclassificationDuMois() {
        try {
            appConfigurationService.findOneById(APP_LAST_DAY_CLASSIFICATION)
                .ifPresent(cfg -> {
                    cfg.setValue(LocalDate.now().toString());
                    cfg.setUpdated(LocalDateTime.now());
                    appConfigurationService.update(cfg);
                });
        } catch (Exception e) {
            LOG.error("Erreur mise à jour du marqueur de reclassification mensuelle", e);
        }
    }

    /**
     * Charge tous les scores Pareto depuis {@code v_abc_pareto_analysis} en une requête.
     */
    private Map<Integer, ParetoScore> loadParetoMap() {
        List<Object[]> rows = paretoRepository.loadAllParetoScores();
        Map<Integer, ParetoScore> map = new HashMap<>(rows.size() * 2);
        for (Object[] row : rows) {
            Integer produitId = toInteger(row[0]);
            if (produitId == null) {
                continue;
            }
            map.put(produitId, new ParetoScore(
                toLong(row[3]),
                toBigDecimal(row[1]),
                toInt(row[2]),
                toInt(row[4]),
                toInt(row[5])
            ));
        }
        return map;
    }

    /**
     * Classe Pareto sans les overrides médicaux (pour calculerScore sans Produit chargé).
     */
    private ClasseCriticite classeDepuisPareto(BigDecimal caCumulePct, int frequenceMois,
        ClassificationConfig config) {
        if (frequenceMois < config.getSeuilFrequenceMinMois()) {
            return ClasseCriticite.D;
        }
        double pct = caCumulePct.doubleValue();
        if (pct <= config.getSeuilParetoAPlus()) {
            return ClasseCriticite.A_PLUS;
        }
        if (pct <= config.getSeuilParetoA()) {
            return ClasseCriticite.A;
        }
        if (pct <= config.getSeuilParetoB()) {
            return ClasseCriticite.B;
        }
        if (pct <= config.getSeuilParetoC()) {
            return ClasseCriticite.C;
        }
        return ClasseCriticite.D;
    }

    private void appliquerChangementClasse(
        Produit produit,
        ClasseCriticite nouvelleClasse,
        ClassificationScoreDTO score,
        String raison,
        ClassificationType type
    ) {
        ClasseCriticite ancienneClasse = produit.getClasseCriticite();
        produit.setClasseCriticite(nouvelleClasse);
        produitRepository.save(produit);

        ClassificationCriticiteLog log = new ClassificationCriticiteLog()
            .setProduit(produit)
            .setAncienneClasse(ancienneClasse)
            .setNouvelleClasse(nouvelleClasse)
            .setVmm12Mois(score != null ? score.vmm12Mois() : null)
            .setCa12Mois(score != null ? score.ca12Mois() : null)
            .setFrequenceVenteMois(score != null ? score.frequenceVenteMois() : null)
            .setScoreTotal(score != null ? BigDecimal.valueOf(score.scoreTotal()) : null)
            .setRaisonChangement(raison)
            .setClassificationType(type);

        if (type == ClassificationType.MANUAL) {
            SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .ifPresent(log::setUser);
        }

        logRepository.save(log);

        LOG.info("Classification changée pour produit {}: {} → {} (caCumulé: {}, type: {})",
            produit.getId(),
            ancienneClasse != null ? ancienneClasse.getCode() : "null",
            nouvelleClasse.getCode(),
            score != null ? score.caCumulePct() : "N/A",
            type);
    }
}

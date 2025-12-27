package com.kobe.warehouse.service.classification;

import com.kobe.warehouse.domain.ClassificationConfig;
import com.kobe.warehouse.domain.ClassificationCriticiteLog;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ProduitMetriquesClassification;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ClassificationType;
import com.kobe.warehouse.repository.ClassificationConfigRepository;
import com.kobe.warehouse.repository.ClassificationCriticiteLogRepository;
import com.kobe.warehouse.repository.ProduitMetriquesClassificationRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.ClassificationConfigDTO;
import com.kobe.warehouse.service.dto.ClassificationLogDTO;
import com.kobe.warehouse.service.dto.ClassificationScoreDTO;
import com.kobe.warehouse.service.dto.ReclassificationResultDTO;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service de classification dynamique de la criticité des produits.
 * Implémente la méthode multi-critères (CA, rotation, fréquence) pour classifier
 * automatiquement les produits en classes A+, A, B, C, D.
 */
@Service
@Transactional
public class ClassificationCriticiteService {
private static final String AUTO_DESCRIPTION = "Reclassification mensuelle automatique";
    private static final Logger LOG = LoggerFactory.getLogger(ClassificationCriticiteService.class);

    @Value("${pharma-smart.classification.batch-size:100}")
    private int batchSize;

    private final ClassificationConfigRepository configRepository;
    private final ClassificationCriticiteLogRepository logRepository;
    private final ProduitRepository produitRepository;
    private final ProduitMetriquesClassificationRepository metriquesRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    public ClassificationCriticiteService(
        ClassificationConfigRepository configRepository,
        ClassificationCriticiteLogRepository logRepository,
        ProduitRepository produitRepository,
        ProduitMetriquesClassificationRepository metriquesRepository,
        UserRepository userRepository,
        EntityManager entityManager
    ) {
        this.configRepository = configRepository;
        this.logRepository = logRepository;
        this.produitRepository = produitRepository;
        this.metriquesRepository = metriquesRepository;
        this.userRepository = userRepository;
        this.entityManager = entityManager;
    }

    /**
     * Récupère la configuration de classification (singleton).
     *
     * @return La configuration ou une configuration par défaut
     */
    public ClassificationConfig getConfig() {
        return configRepository.findConfiguration()
            .orElseGet(this::createDefaultConfig);
    }

    /**
     * Récupère la configuration sous forme de DTO.
     *
     * @return DTO de configuration
     */
    public ClassificationConfigDTO getConfigDTO() {
        return ClassificationConfigDTO.fromEntity(getConfig());
    }

    /**
     * Met à jour la configuration de classification.
     *
     * @param dto DTO de configuration à appliquer
     * @return La configuration mise à jour
     */
    public ClassificationConfigDTO updateConfig(ClassificationConfigDTO dto) {
        ClassificationConfig config = getConfig();

        if (dto.poidsCa() != null) config.setPoidsCa(dto.poidsCa());
        if (dto.poidsRotation() != null) config.setPoidsRotation(dto.poidsRotation());
        if (dto.poidsFrequence() != null) config.setPoidsFrequence(dto.poidsFrequence());
        if (dto.seuilAPlus() != null) config.setSeuilAPlus(dto.seuilAPlus());
        if (dto.seuilA() != null) config.setSeuilA(dto.seuilA());
        if (dto.seuilB() != null) config.setSeuilB(dto.seuilB());
        if (dto.seuilC() != null) config.setSeuilC(dto.seuilC());
        if (dto.nbMoisAnalyse() != null) config.setNbMoisAnalyse(dto.nbMoisAnalyse());
        if (dto.nbMoisMinNouveauProduit() != null) config.setNbMoisMinNouveauProduit(dto.nbMoisMinNouveauProduit());
        if (dto.changementMinScore() != null) config.setChangementMinScore(dto.changementMinScore());
        if (dto.autoClassificationEnabled() != null)
            config.setAutoClassificationEnabled(dto.autoClassificationEnabled());

        config = configRepository.save(config);
        return ClassificationConfigDTO.fromEntity(config);
    }

    /**
     * Calcule le score de classification pour un produit.
     *
     * @param produitId ID du produit
     * @return Score de classification avec toutes les métriques
     */
    public ClassificationScoreDTO calculerScore(Integer produitId) {
        ClassificationConfig config = getConfig();

        // Récupérer les métriques du produit via l'entité JPA
        ProduitMetriquesClassification metriques = metriquesRepository.findByProduitId(produitId)
            .orElse(null);

        if (metriques == null) {
            return null;
        }

        // Extraire les valeurs depuis l'entité
        String libelle = metriques.getLibelle();
        Long ca12Mois = metriques.getCa12MoisOrZero();
        Integer vmm12Mois = metriques.getVmm12Mois() != null ? metriques.getVmm12Mois() : 0;
        Integer qteVendue12Mois = metriques.getQteVendue12Mois() != null ? metriques.getQteVendue12Mois() : 0;
        int frequenceVenteMois = metriques.getFrequenceVenteMoisOrZero();
        BigDecimal rotationAnnuelle = metriques.getRotationAnnuelleOrZero();
        Integer stockActuel = metriques.getStockActuel() != null ? metriques.getStockActuel() : 0;
        int ancienneteMois = metriques.getAncienneteMois() != null ? metriques.getAncienneteMois() : 0;
        ClasseCriticite classeActuelle = metriques.getClasseActuelle() != null ? metriques.getClasseActuelle() : ClasseCriticite.B;

        // Déterminer si c'est un nouveau produit selon la config
        boolean estNouveauProduit = metriques.isNouveauProduit(config.getNbMoisMinNouveauProduit());

        // Calculer les scores normalisés (percentiles)
        int scoreCA = calculerScoreCA(ca12Mois);
        int scoreRotation = calculerScoreRotation(rotationAnnuelle);
        int scoreFrequence = calculerScoreFrequence(frequenceVenteMois, config.getNbMoisAnalyse());

        // Calculer le score total pondéré
        int scoreTotal = (int) Math.round(
            config.getPoidsCa().doubleValue() * scoreCA +
                config.getPoidsRotation().doubleValue() * scoreRotation +
                config.getPoidsFrequence().doubleValue() * scoreFrequence
        );

        // Déterminer la classe suggérée
        ClasseCriticite classeSuggeree = determinerClasse(scoreTotal, config);

        // Vérifier si le changement est significatif
        boolean changementSignificatif = isChangementSignificatif(
            scoreTotal, classeActuelle, config
        );

        return new ClassificationScoreDTO(
            produitId,
            libelle,
            ca12Mois,
            vmm12Mois,
            qteVendue12Mois,
            frequenceVenteMois,
            rotationAnnuelle,
            stockActuel,
            scoreCA,
            scoreRotation,
            scoreFrequence,
            scoreTotal,
            classeSuggeree,
            classeActuelle,
            estNouveauProduit,
            ancienneteMois,
            changementSignificatif
        );
    }

    /**
     * Détermine la classe de criticité basée sur le score.
     *
     * @param score  Score total (0-100)
     * @param config Configuration des seuils
     * @return Classe de criticité correspondante
     */
    public ClasseCriticite determinerClasse(int score, ClassificationConfig config) {
        if (score >= config.getSeuilAPlus()) {
            return ClasseCriticite.A_PLUS;
        } else if (score >= config.getSeuilA()) {
            return ClasseCriticite.A;
        } else if (score >= config.getSeuilB()) {
            return ClasseCriticite.B;
        } else if (score >= config.getSeuilC()) {
            return ClasseCriticite.C;
        } else {
            return ClasseCriticite.D;
        }
    }

    /**
     * Applique un changement de classe avec logging.
     *
     * @param produit        produit
     * @param nouvelleClasse Nouvelle classe à appliquer
     * @param score          Score de classification
     * @param raison         Raison du changement
     * @param type           Type de classification
     */

    private void appliquerChangementClasse(
        Produit produit,
        ClasseCriticite nouvelleClasse,
        ClassificationScoreDTO score,
        String raison,
        ClassificationType type
    ) {


        ClasseCriticite ancienneClasse = produit.getClasseCriticite();

        // Mettre à jour le produit
        produit.setClasseCriticite(nouvelleClasse);
        produitRepository.save(produit);

        // Créer le log
        ClassificationCriticiteLog log = new ClassificationCriticiteLog()
            .setProduit(produit)
            .setAncienneClasse(ancienneClasse)
            .setNouvelleClasse(nouvelleClasse)
            .setVmm12Mois(score != null ? score.vmm12Mois() : null)
            .setCa12Mois(score != null ? score.ca12Mois() : null)
            .setRotationAnnuelle(score != null ? score.rotationAnnuelle() : null)
            .setFrequenceVenteMois(score != null ? score.frequenceVenteMois() : null)
            .setScoreTotal(score != null ? BigDecimal.valueOf(score.scoreTotal()) : null)
            .setRaisonChangement(raison)
            .setClassificationType(type);

        // Ajouter l'utilisateur si c'est un changement manuel
        if (type == ClassificationType.MANUAL) {
            SecurityUtils.getCurrentUserLogin()
                .flatMap(userRepository::findOneByLogin)
                .ifPresent(log::setUser);
        }

        logRepository.save(log);

        LOG.info("Classification changée pour produit {}: {} -> {} (score: {}, type: {})",
            produit.getId(),
            ancienneClasse != null ? ancienneClasse.getCode() : "null",
            nouvelleClasse.getCode(),
            score != null ? score.scoreTotal() : "N/A",
            type);
    }

    /**
     * Override manuel de la classe d'un produit.
     *
     * @param produitId      ID du produit
     * @param nouvelleClasse Classe à forcer
     * @param raison         Raison de l'override
     */
    @Transactional
    public void overrideClasse(Integer produitId, ClasseCriticite nouvelleClasse, String raison) {
        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));

        ClassificationScoreDTO score = calculerScore(produitId);

        appliquerChangementClasse(produit, nouvelleClasse, score, raison, ClassificationType.MANUAL);

        // Marquer comme overridden pour éviter la reclassification auto
        produit.setIsClassificationOverridden(true);
        produitRepository.save(produit);
    }

    /**
     * Reclassifie tous les produits selon leurs métriques.
     * Exécuté mensuellement après le gel du mois précédent.
     *
     * @return Résultat de la reclassification
     */
    @Scheduled(cron = "${pharma-smart.classification.cron:0 0 * 1-8 * ?}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReclassificationResultDTO reclassifierTousProduits() {
        return reclassifierTousProduits(AUTO_DESCRIPTION);
    }

    /**
     * Reclassifie tous les produits avec une raison personnalisée.
     *
     * @param raison Raison de la reclassification
     * @return Résultat de la reclassification
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ReclassificationResultDTO reclassifierTousProduits(String raison) {
        LOG.info("Démarrage reclassification: {}", raison);
        long startTime = System.currentTimeMillis();

        ClassificationConfig config = getConfig();

        // Vérifier si la classification auto est activée
        if (!config.getAutoClassificationEnabled()) {
            LOG.warn("Classification automatique désactivée");
            return ReclassificationResultDTO.builder()
                .raison("Classification automatique désactivée")
                .dateExecution(LocalDateTime.now())
                .build();
        }
        if (raison.equals(AUTO_DESCRIPTION) && config.getUpdatedAt().getMonth()== LocalDate.now().getMonth()) {

            LOG.warn("Reclassification automatique déjà exécutée ce mois-ci");
            return ReclassificationResultDTO.builder()
                .raison("Reclassification automatique déjà exécutée ce mois-ci")
                .dateExecution(LocalDateTime.now())
                .build();
        }

        // Récupérer la distribution avant
        Map<String, Long> repartitionAvant = getDistributionClasses();

        // Compter les produits éligibles
        long totalProduits = countProduitsEligibles();
        int totalPages = (int) Math.ceil((double) totalProduits / batchSize);

        AtomicInteger nbAnalyses = new AtomicInteger(0);
        AtomicInteger nbChangements = new AtomicInteger(0);
        AtomicInteger nbPromotions = new AtomicInteger(0);
        AtomicInteger nbRetrogradations = new AtomicInteger(0);
        AtomicInteger nbNouveaux = new AtomicInteger(0);
        AtomicInteger nbOverridden = new AtomicInteger(0);
        AtomicInteger nbErreurs = new AtomicInteger(0);

        // Traitement par lots
        for (int page = 0; page < totalPages; page++) {
            try {
                processBatchClassification(
                    PageRequest.of(page, batchSize),
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

        // Récupérer la distribution après
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

        // Alerte si anomalie potentielle
        if (result.hasAnomaliePotentielle()) {
            LOG.warn("ALERTE: Pourcentage de changements anormalement élevé: {:.1f}%",
                result.getPourcentageChangements());
        }
        updateConfig(config);
        return result;
    }

    /**
     * Traite un batch de produits pour la classification.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processBatchClassification(
        Pageable pageable,
        String raison,
        AtomicInteger nbAnalyses,
        AtomicInteger nbChangements,
        AtomicInteger nbPromotions,
        AtomicInteger nbRetrogradations,
        AtomicInteger nbNouveaux,
        AtomicInteger nbOverridden,
        AtomicInteger nbErreurs
    ) {
        List<Integer> produitIds = getProduitsEligiblesIds(pageable);

        for (Integer produitId : produitIds) {
            try {
                Produit produit = produitRepository.findById(produitId).orElse(null);
                if (produit == null) continue;

                nbAnalyses.incrementAndGet();

                // Vérifier si le produit est overridden
                if (Boolean.TRUE.equals(produit.getIsClassificationOverridden())) {
                    nbOverridden.incrementAndGet();
                    continue;
                }

                ClassificationScoreDTO score = calculerScore(produitId);
                if (score == null) continue;

                // Vérifier si c'est un nouveau produit
                if (score.estNouveauProduit()) {
                    nbNouveaux.incrementAndGet();
                    continue;
                }

                // Vérifier si un changement est nécessaire
                if (score.doitChangerClasse()) {
                    appliquerChangementClasse(
                        produit,
                        score.classeSuggeree(),
                        score,
                        raison,
                        ClassificationType.AUTO
                    );
                    nbChangements.incrementAndGet();

                    if (score.estPromotion()) {
                        nbPromotions.incrementAndGet();
                    } else if (score.estRetrogradation()) {
                        nbRetrogradations.incrementAndGet();
                    }
                }

            } catch (Exception e) {
                LOG.error("Erreur classification produit {}", produitId, e);
                nbErreurs.incrementAndGet();
            }
        }

        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Récupère les logs de classification paginés.
     *
     * @param pageable Pagination
     * @return Page de logs
     */
    public Page<ClassificationLogDTO> getLogs(Pageable pageable) {
        return logRepository.findAll(pageable)
            .map(ClassificationLogDTO::fromEntity);
    }

    /**
     * Récupère les logs d'un produit.
     *
     * @param produitId ID du produit
     * @param pageable  Pagination
     * @return Page de logs
     */
    public Page<ClassificationLogDTO> getLogsProduit(Integer produitId, Pageable pageable) {
        return logRepository.findByProduitIdOrderByCreatedAtDesc(produitId, pageable)
            .map(ClassificationLogDTO::fromEntity);
    }

    /**
     * Récupère la distribution actuelle des classes.
     *
     * @return Map classe -> nombre de produits
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

    // ==================== Méthodes privées ====================

    private ClassificationConfig createDefaultConfig() {
        ClassificationConfig config = new ClassificationConfig();
        return configRepository.save(config);
    }

    private int calculerScoreCA(Long ca12Mois) {
        if (ca12Mois == null || ca12Mois == 0) {
            return 0;
        }

        // Calculer le percentile du CA par rapport à tous les produits via la vue
        long countInferieur = metriquesRepository.countByCa12MoisLessThan(ca12Mois);
        long total = metriquesRepository.countAll();
        if (total == 0) return 0;

        return (int) Math.round((double) countInferieur / total * 100);
    }

    private int calculerScoreRotation(BigDecimal rotation) {
        if (rotation == null || rotation.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        ClassificationConfig config = getConfig();

        // Score basé sur les seuils de rotation
        if (rotation.compareTo(config.getRotationAPlus()) >= 0) {
            return 100;
        } else if (rotation.compareTo(config.getRotationA()) >= 0) {
            return 85;
        } else if (rotation.compareTo(config.getRotationB()) >= 0) {
            return 70;
        } else if (rotation.compareTo(config.getRotationC()) >= 0) {
            return 50;
        } else {
            // Score proportionnel pour rotations < 2
            return (int) Math.round(rotation.doubleValue() / config.getRotationC().doubleValue() * 50);
        }
    }

    private int calculerScoreFrequence(int frequence, int nbMoisAnalyse) {
        if (nbMoisAnalyse == 0) return 0;
        return (int) Math.round((double) frequence / nbMoisAnalyse * 100);
    }

    private boolean isChangementSignificatif(int scoreTotal, ClasseCriticite classeActuelle, ClassificationConfig config) {
        // Calculer le score médian de la classe actuelle
        int scoreMedianClasse = getScoreMedianClasse(classeActuelle, config);

        // Le changement est significatif si l'écart est supérieur au seuil
        return Math.abs(scoreTotal - scoreMedianClasse) >= config.getChangementMinScore();
    }

    private int getScoreMedianClasse(ClasseCriticite classe, ClassificationConfig config) {
        if (classe == null) return 50;

        return switch (classe) {
            case A_PLUS -> (config.getSeuilAPlus() + 100) / 2;
            case A -> (config.getSeuilA() + config.getSeuilAPlus()) / 2;
            case B -> (config.getSeuilB() + config.getSeuilA()) / 2;
            case C -> (config.getSeuilC() + config.getSeuilB()) / 2;
            case D -> config.getSeuilC() / 2;
        };
    }

    private long countProduitsEligibles() {
        return metriquesRepository.countAll();
    }

    private List<Integer> getProduitsEligiblesIds(Pageable pageable) {
        return metriquesRepository.findAllProduitIds(pageable);
    }

    private void updateConfig(ClassificationConfig config) {
        config.setUpdatedAt(LocalDateTime.now());
        configRepository.save(config);
    }
}

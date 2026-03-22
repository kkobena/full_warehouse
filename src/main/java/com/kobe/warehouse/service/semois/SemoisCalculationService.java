package com.kobe.warehouse.service.semois;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SemoisClasseConfig;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.SemoisSuggestionView;
import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ModelReapprovisionnement;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SemoisClasseConfigRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.SemoisSuggestionViewRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.dto.SemoisSuggestionDTO;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Transactional
public class SemoisCalculationService {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisCalculationService.class);
    private static final String APP_LAST_DAY_SEMOIS_CALCULATION = "APP_LAST_DAY_SEMOIS_CALCULATION";

    @Value("${pharma-smart.semois.batch-size:100}")
    private int batchSize;

    private final AtomicBoolean calculEnCours = new AtomicBoolean(false);

    private final SemoisConfigurationRepository semoisConfigRepository;
    private final SemoisClasseConfigRepository semoisClasseConfigRepository;
    private final SemoisSuggestionViewRepository semoisSuggestionViewRepository;
    private final VentesMensuellesAgregeesRepository ventesAgregeesRepository;
    private final ProduitRepository produitRepository;
    private final StockProduitRepository stockProduitRepository;
    private final AppConfigurationService appConfigurationService;

    public SemoisCalculationService(
        SemoisConfigurationRepository semoisConfigRepository,
        SemoisClasseConfigRepository semoisClasseConfigRepository,
        SemoisSuggestionViewRepository semoisSuggestionViewRepository,
        VentesMensuellesAgregeesRepository ventesAgregeesRepository,
        ProduitRepository produitRepository,
        StockProduitRepository stockProduitRepository,
        AppConfigurationService appConfigurationService
    ) {
        this.semoisConfigRepository = semoisConfigRepository;
        this.semoisClasseConfigRepository = semoisClasseConfigRepository;
        this.semoisSuggestionViewRepository = semoisSuggestionViewRepository;
        this.ventesAgregeesRepository = ventesAgregeesRepository;
        this.produitRepository = produitRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.appConfigurationService = appConfigurationService;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Scheduler : filet de sécurité horaire (8h–19h)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Filet de sécurité : toutes les heures entre 8h et 19h.
     * Si le recalcul n'a pas été fait aujourd'hui (machine redémarrée, crash…), il s'exécute.
     * L'idempotence est garantie par APP_LAST_DAY_SEMOIS_CALCULATION.
     */
    @Scheduled(cron = "${pharma-smart.semois.recalculation-cron:0 0 8-19 * * *}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void recalculateAllConfigurations() {
        if (!calculEnCours.compareAndSet(false, true)) {
            LOG.warn("Recalcul SEMOIS déjà en cours, skip");
            return;
        }
        try {
            doRecalculateAllConfigurations();
        } finally {
            calculEnCours.set(false);
        }
    }

    /** Retourne true si un recalcul est en cours (scheduler ou API). */
    public boolean isCalculEnCours() {
        return calculEnCours.get();
    }

    /**
     * Recalcul forcé après une reclassification : ignore la garde journalière car les classes
     * viennent d'être modifiées et les valeurs SEMOIS sont obsolètes.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void recalculateAfterClassification() {
        if (!calculEnCours.compareAndSet(false, true)) {
            LOG.warn("Recalcul SEMOIS déjà en cours, skip recalcul post-classification");
            return;
        }
        try {
            // On force en réinitialisant la date de dernier calcul
            appConfigurationService.findOneById(APP_LAST_DAY_SEMOIS_CALCULATION)
                .ifPresent(cfg -> {
                    cfg.setValue("");
                    appConfigurationService.update(cfg);
                });
            doRecalculateAllConfigurations();
        } finally {
            calculEnCours.set(false);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Calcul principal
    // ──────────────────────────────────────────────────────────────────────────

    private void doRecalculateAllConfigurations() {
        Optional<AppConfiguration> semoisConfigOpt = getLastSemoisCalculationDate();
        LocalDate lastCalcDate = semoisConfigOpt
            .map(c -> StringUtils.isNotBlank(c.getValue()) ? LocalDate.parse(c.getValue()) : null)
            .orElse(null);

        if (lastCalcDate != null && lastCalcDate.isEqual(LocalDate.now())) {
            LOG.info("Recalcul SEMOIS déjà effectué aujourd'hui ({}), skip", lastCalcDate);
            return;
        }

        ModelReapprovisionnement model = getConfiguredModel();
        boolean isSemoisModel = model == ModelReapprovisionnement.SEMOIS;

        Optional<AppConfiguration> lastReapproConfigOpt = getLastReapproDate();
        LocalDate lastReapproDate = lastReapproConfigOpt
            .map(c -> StringUtils.isNotBlank(c.getValue()) ? LocalDate.parse(c.getValue()) : null)
            .orElse(null);
        boolean canUpdateProduitReapproInfo = isNull(lastReapproDate)
            || !lastReapproDate.withDayOfMonth(1).equals(LocalDate.now().withDayOfMonth(1));

        // Charger les 5 configs de classe en mémoire (évite un N+1 par produit)
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigMap = semoisClasseConfigRepository.findAll()
            .stream()
            .collect(Collectors.toMap(SemoisClasseConfig::getClasseCriticite, Function.identity()));

        if (classeConfigMap.isEmpty()) {
            LOG.warn("Aucune SemoisClasseConfig trouvée - Recalcul annulé");
            return;
        }

        long totalProduits = produitRepository.countActiveNonDetail();
        int totalPages = (int) Math.ceil((double) totalProduits / batchSize);

        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);
        long startTime = System.currentTimeMillis();

        LOG.info("Recalcul SEMOIS démarré - {} produits à traiter en {} batch(es)", totalProduits, totalPages);

        for (int pageNumber = 0; pageNumber < totalPages; pageNumber++) {
            try {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                Page<Produit> page = produitRepository.findActiveNonDetailWithFournisseur(pageable);

                BatchResult result = processBatch(page.getContent(), canUpdateProduitReapproInfo, classeConfigMap);
                totalSuccess.addAndGet(result.successCount());
                totalErrors.addAndGet(result.errorCount());

                LOG.debug("Batch {}/{} - Succès: {}, Erreurs: {}", pageNumber + 1, totalPages,
                    result.successCount(), result.errorCount());

            } catch (Exception e) {
                LOG.error("Erreur critique - batch {}/{} - Skip", pageNumber + 1, totalPages, e);
                totalErrors.addAndGet(batchSize);
            }
        }

        if (canUpdateProduitReapproInfo && isSemoisModel) {
            updateAppConfigurationDate(lastReapproConfigOpt.orElse(null));
        }
        updateAppConfigurationDate(semoisConfigOpt.orElse(null));

        long duration = System.currentTimeMillis() - startTime;
        LOG.info("Recalcul SEMOIS terminé en {}ms - Succès: {}, Erreurs: {}",
            duration, totalSuccess.get(), totalErrors.get());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected BatchResult processBatch(
        List<Produit> produits,
        boolean canUpdateProduitReapproInfo,
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigMap
    ) {
        if (produits.isEmpty()) {
            return new BatchResult(0, 0);
        }

        // Bulk-load des SemoisConfiguration existantes (évite N+1)
        List<Integer> produitIds = produits.stream().map(Produit::getId).toList();
        Map<Integer, SemoisConfiguration> configsByProduitId = semoisConfigRepository
            .findByProduitIdIn(produitIds)
            .stream()
            .collect(Collectors.toMap(sc -> sc.getProduit().getId(), Function.identity()));

        int successCount = 0;
        int errorCount = 0;
        List<SemoisConfiguration> toSaveConfigs = new ArrayList<>();
        List<Produit> toSaveProduits = new ArrayList<>();

        for (Produit produit : produits) {
            try {
                // Classe depuis Produit (auto-classifié) ; SemoisConfiguration peut surcharger
                ClasseCriticite classe = produit.getEffectiveClasseCriticite();
                SemoisClasseConfig classeConfig = classeConfigMap.get(classe);
                if (classeConfig == null) {
                    // Fallback sur B si la classe n'a pas de config
                    classeConfig = classeConfigMap.get(ClasseCriticite.B);
                }
                if (classeConfig == null) {
                    LOG.warn("SemoisClasseConfig introuvable pour classe {} et fallback B - produit {} ignoré",
                        classe, produit.getId());
                    errorCount++;
                    continue;
                }

                SemoisConfiguration config = configsByProduitId.get(produit.getId());
                if (config == null) {
                    // Créer une configuration par défaut (sera persistée dans ce batch)
                    config = SemoisConfiguration.createDefault(produit, classe);
                }

                AllCalculs c = computeAllCalculs(config, classeConfig, produit.getId());
                config.updateCalculs(c.vmm(), c.margeSecurite(), c.stockObjectif());
                toSaveConfigs.add(config);

                if (canUpdateProduitReapproInfo) {
                    produit.setQtyAppro(c.stockObjectif());
                    produit.setQtySeuilMini(c.margeSecurite());
                    produit.setUpdatedAt(LocalDateTime.now());
                    toSaveProduits.add(produit);
                }
                successCount++;
            } catch (Exception e) {
                LOG.error("Erreur recalcul SEMOIS pour produit {}", produit.getId(), e);
                errorCount++;
            }
        }

        semoisConfigRepository.saveAll(toSaveConfigs);
        if (!toSaveProduits.isEmpty()) {
            produitRepository.saveAll(toSaveProduits);
        }

        return new BatchResult(successCount, errorCount);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Calculs métier
    // ──────────────────────────────────────────────────────────────────────────

    public int calculateVMM(Integer produitId, int nbMois) {
        List<VentesMensuellesAgregees> ventes = ventesAgregeesRepository
            .findLastNMonthsByProduit(produitId, nbMois);

        if (ventes.isEmpty()) {
            return 0;
        }

        BigDecimal sommePonderee = BigDecimal.ZERO;
        BigDecimal sommePoids = BigDecimal.ZERO;

        int poids = nbMois;
        for (VentesMensuellesAgregees vente : ventes) {
            BigDecimal quantite = BigDecimal.valueOf(vente.getQuantiteVendue());
            BigDecimal poidsDecimal = BigDecimal.valueOf(poids);
            sommePonderee = sommePonderee.add(quantite.multiply(poidsDecimal));
            sommePoids = sommePoids.add(poidsDecimal);
            poids--;
        }

        if (sommePoids.compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }

        return sommePonderee.divide(sommePoids, RoundingMode.HALF_UP).intValue();
    }

    private int resolveDelaiLivraisonJours(SemoisConfiguration config) {
        if (config.getDelaiLivraisonJours() != null) {
            return config.getDelaiLivraisonJours();
        }
        var fp = config.getProduit().getFournisseurProduitPrincipal();
        if (fp != null) {
            var fournisseur = fp.getFournisseur();
            if (fournisseur != null && fournisseur.getDelaiLivraisonJours() != null) {
                return fournisseur.getDelaiLivraisonJours();
            }
            if (fournisseur != null
                && fournisseur.getGroupeFournisseur() != null
                && fournisseur.getGroupeFournisseur().getDelaiLivraisonJours() != null) {
                return fournisseur.getGroupeFournisseur().getDelaiLivraisonJours();
            }
        }
        return 7;
    }

    private @NonNull BigDecimal computeMarge(SemoisConfiguration config, SemoisClasseConfig classeConfig, int vmm) {
        int delaiJours = resolveDelaiLivraisonJours(config);
        BigDecimal coefficient = classeConfig.getCoefficientSecurite();
        BigDecimal facteurSaisonnier = Objects.requireNonNullElse(
            config.getFacteurSaisonnierActuel(), BigDecimal.ONE);

        return BigDecimal.valueOf(vmm)
            .multiply(BigDecimal.valueOf(delaiJours))
            .multiply(coefficient)
            .divide(BigDecimal.valueOf(30), RoundingMode.HALF_UP)
            .multiply(facteurSaisonnier);
    }

    private AllCalculs computeAllCalculs(SemoisConfiguration config, SemoisClasseConfig classeConfig, Integer produitId) {
        int vmm = calculateVMM(produitId, classeConfig.getNbMoisHistorique());
        int marge = vmm == 0 ? 0 : computeMarge(config, classeConfig, vmm).intValue();
        int stockObjectif = applyPeremption(config, classeConfig, produitId, vmm, marge);
        int stockActuel = getStockActuel(produitId);
        return new AllCalculs(vmm, marge, stockObjectif, stockActuel, Math.max(0, stockObjectif - stockActuel));
    }

    private int applyPeremption(SemoisConfiguration config, SemoisClasseConfig classeConfig,
                                Integer produitId, int vmm, int marge) {
        int stockObjectif = vmm + marge;
        boolean limitePeremption = config.getLimitePeremption() != null
            ? config.getLimitePeremption()
            : classeConfig.getLimitePeremption();
        if (limitePeremption && vmm > 0) {
            stockObjectif = Math.min(stockObjectif, vmm * 3);
            LOG.debug("Limite péremption appliquée pour produit {}: {}", produitId, stockObjectif);
        }
        return stockObjectif;
    }

    private record AllCalculs(int vmm, int margeSecurite, int stockObjectif, int stockActuel, int quantiteACommander) {}

    private int getStockActuel(Integer produitId) {
        Integer stock = stockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId(
            (int) EntityConstant.DEFAULT_MAGASIN, produitId);
        return stock != null ? stock : 0;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // API publique (lecture)
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public SemoisSuggestionDTO getSuggestionForProduct(Integer produitId) {
        SemoisConfiguration config = semoisConfigRepository
            .findByProduitId(produitId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Configuration SEMOIS non trouvée pour produit " + produitId));

        ClasseCriticite classe = config.getProduit().getEffectiveClasseCriticite();
        SemoisClasseConfig classeConfig = semoisClasseConfigRepository
            .findById(classe)
            .orElseThrow(() -> new IllegalArgumentException(
                "Configuration de classe SEMOIS non trouvée pour " + classe));

        AllCalculs c = computeAllCalculs(config, classeConfig, produitId);
        Produit produit = config.getProduit();
        var fpPrincipal = produit.getFournisseurProduitPrincipal();

        return new SemoisSuggestionDTO(
            produitId,
            produit.getLibelle(),
            fpPrincipal != null ? fpPrincipal.getCodeCip() : null,
            fpPrincipal != null ? fpPrincipal.getFournisseur().getId() : null,
            fpPrincipal != null ? fpPrincipal.getFournisseur().getLibelle() : null,
            classe,
            c.vmm(),
            c.margeSecurite(),
            c.stockObjectif(),
            c.stockActuel(),
            c.quantiteACommander(),
            resolveDelaiLivraisonJours(config),
            classeConfig.getCoefficientSecurite(),
            config.getFacteurSaisonnierActuel(),
            config.getDateDernierCalcul()
        );
    }

    @Transactional(readOnly = true)
    public Page<SemoisSuggestionDTO> getAllSuggestions(
        String search,
        ClasseCriticite classeCriticite,
        Pageable pageable
    ) {
        LOG.debug("Récupération suggestions SEMOIS depuis vue - search: {}, classe: {}, page: {}",
            search, classeCriticite, pageable.getPageNumber());

        Page<SemoisSuggestionView> viewPage = semoisSuggestionViewRepository.findAllWithFilters(
            search, classeCriticite, pageable);

        List<SemoisSuggestionDTO> suggestions = viewPage.getContent().stream()
            .map(this::toDTO)
            .toList();

        return new PageImpl<>(suggestions, pageable, viewPage.getTotalElements());
    }

    private SemoisSuggestionDTO toDTO(SemoisSuggestionView view) {
        return new SemoisSuggestionDTO(
            view.getProduitId(),
            view.getLibelle(),
            view.getCodeCip(),
            view.getFournisseurId(),
            view.getFournisseurLibelle(),
            view.getClasseCriticite(),
            view.getVmm(),
            view.getMargeSecurite(),
            view.getStockObjectif(),
            view.getStockActuel() != null ? view.getStockActuel().intValue() : 0,
            view.getQuantiteACommander() != null ? view.getQuantiteACommander().intValue() : 0,
            view.getDelaiLivraisonJours(),
            view.getCoefficientSecurite() != null
                ? BigDecimal.valueOf(view.getCoefficientSecurite()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ONE,
            BigDecimal.ONE,
            view.getDateDernierCalcul() != null
                ? LocalDateTime.ofInstant(view.getDateDernierCalcul(), java.time.ZoneId.systemDefault())
                : null
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Initialisation / administration
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public SemoisConfiguration initializeConfiguration(Integer produitId) {
        return semoisConfigRepository.findByProduitId(produitId).orElseGet(() -> {
            Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));
            ClasseCriticite classe = produit.getEffectiveClasseCriticite();
            return semoisConfigRepository.save(SemoisConfiguration.createDefault(produit, classe));
        });
    }

    @Transactional
    public SemoisConfiguration initializeConfiguration(Integer produitId, ClasseCriticite classe) {
        return semoisConfigRepository.findByProduitId(produitId).orElseGet(() -> {
            Produit produit = produitRepository.findById(produitId)
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));
            return semoisConfigRepository.save(SemoisConfiguration.createDefault(produit, classe));
        });
    }

    @Transactional
    public SemoisConfiguration create(Produit produit, ClasseCriticite classe) {
        return semoisConfigRepository.save(SemoisConfiguration.createDefault(produit, classe));
    }

    @Transactional
    public int initializeAllMissingConfigurations() {
        LOG.info("Initialisation configurations SEMOIS manquantes...");
        int created = semoisConfigRepository.initializeAllMissingConfigurations();
        LOG.info("{} configurations SEMOIS initialisées", created);
        return created;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers privés
    // ──────────────────────────────────────────────────────────────────────────

    private record BatchResult(int successCount, int errorCount) {}

    private ModelReapprovisionnement getConfiguredModel() {
        return appConfigurationService
            .findOneById(EntityConstant.APP_MODEL_REAPPRO)
            .map(AppConfiguration::getValue)
            .filter(StringUtils::isNotEmpty)
            .map(value -> {
                try {
                    return ModelReapprovisionnement.valueOf(value);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Modèle de réapprovisionnement invalide '{}', utilisation de SEMOIS par défaut", value);
                    return ModelReapprovisionnement.SEMOIS;
                }
            })
            .orElse(ModelReapprovisionnement.SEMOIS);
    }

    private Optional<AppConfiguration> getLastReapproDate() {
        return appConfigurationService.findOneById(EntityConstant.APP_LAST_DAY_REAPPRO);
    }

    private Optional<AppConfiguration> getLastSemoisCalculationDate() {
        return appConfigurationService.findOneById(APP_LAST_DAY_SEMOIS_CALCULATION);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateAppConfigurationDate(AppConfiguration config) {
        try {
            if (nonNull(config)) {
                config.setValue(LocalDate.now().toString());
                config.setUpdated(LocalDateTime.now());
                appConfigurationService.update(config);
            }
            LOG.info("Date dernier recalcul SEMOIS mise à jour: {}", LocalDate.now());
        } catch (Exception e) {
            LOG.error("Erreur mise à jour date dernier recalcul SEMOIS", e);
        }
    }
}

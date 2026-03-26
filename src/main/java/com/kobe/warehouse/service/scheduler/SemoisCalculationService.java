package com.kobe.warehouse.service.scheduler;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SemoisClasseConfig;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.SemoisSuggestionView;
import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ModelReapprovisionnement;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SemoisClasseConfigRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.SemoisSuggestionViewRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import com.kobe.warehouse.service.dto.ReapproDashboardDTO;
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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    /** Stock virtuel : commandes REQUESTED en attente de livraison. */
    private final OrderLineRepository orderLineRepository;

    public SemoisCalculationService(
        SemoisConfigurationRepository semoisConfigRepository,
        SemoisClasseConfigRepository semoisClasseConfigRepository,
        SemoisSuggestionViewRepository semoisSuggestionViewRepository,
        VentesMensuellesAgregeesRepository ventesAgregeesRepository,
        ProduitRepository produitRepository,
        StockProduitRepository stockProduitRepository,
        AppConfigurationService appConfigurationService,
        OrderLineRepository orderLineRepository
    ) {
        this.semoisConfigRepository = semoisConfigRepository;
        this.semoisClasseConfigRepository = semoisClasseConfigRepository;
        this.semoisSuggestionViewRepository = semoisSuggestionViewRepository;
        this.ventesAgregeesRepository = ventesAgregeesRepository;
        this.produitRepository = produitRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.appConfigurationService = appConfigurationService;
        this.orderLineRepository = orderLineRepository;
    }


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


        Set<Integer> produitIds = produits.stream().map(Produit::getId).collect(Collectors.toSet());
        Map<Integer, SemoisConfiguration> configsByProduitId = semoisConfigRepository
            .findByProduitIdIn(produitIds)
            .stream()
            .collect(Collectors.toMap(sc -> sc.getProduit().getId(), Function.identity()));


        Map<Integer, List<StockProduit>> stockProduitsByProduitId = canUpdateProduitReapproInfo
            ? stockProduitRepository
                .findAllByProduitIdInAndMagasinId(produitIds,  EntityConstant.DEFAULT_MAGASIN)
                .stream()
                .collect(Collectors.groupingBy(sp -> sp.getProduit().getId()))
            : Map.of();


        String moisN1 = YearMonth.now().minusYears(1).toString();
        String moisN2 = YearMonth.now().minusYears(2).toString();
        Map<Integer, Map<String, Integer>> saisonByProduitId = canUpdateProduitReapproInfo
            ? ventesAgregeesRepository
                .findByProduitIdInAndAnneeMoisIn(produitIds, Set.of(moisN1, moisN2))
                .stream()
                .collect(Collectors.groupingBy(
                    vma -> vma.getProduit().getId(),
                    Collectors.toMap(
                        VentesMensuellesAgregees::getAnneeMois,
                        VentesMensuellesAgregees::getQuantiteVendue,
                        (v1, _) -> v1  // dédoublonnage défensif
                    )
                ))
            : Map.of();

        int successCount = 0;
        int errorCount = 0;
        List<SemoisConfiguration> toSaveConfigs = new ArrayList<>();
        List<Produit> toSaveProduits = new ArrayList<>();
        List<StockProduit> toSaveStockProduits = new ArrayList<>();

        for (Produit produit : produits) {
            try {
                // Classe depuis Produit (auto-classifié) ; SemoisConfiguration peut surcharger
                ClasseCriticite classe = produit.getEffectiveClasseCriticite();
                SemoisClasseConfig classeConfig = classeConfigMap.get(classe);
                if (classeConfig == null) {
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

                if (canUpdateProduitReapproInfo) {
                    //  Auto-calcul facteur saisonnier AVANT computeAllCalculs()
                    // pour que computeMarge() utilise le nouveau facteur.
                    if (!config.isFacteurSaisonnierManuel()) {
                        Map<String, Integer> saisonData = saisonByProduitId
                            .getOrDefault(produit.getId(), Map.of());
                        BigDecimal newCoeff = computeSeasonalFactor(config, saisonData, moisN1, moisN2);
                        config.setFacteurSaisonnierActuel(newCoeff);
                    }
                }

                AllCalculs c = computeAllCalculs(config, classeConfig, produit.getId());
                config.updateCalculs(c.vmm(), c.margeSecurite(), c.stockObjectif());
                toSaveConfigs.add(config);

                if (canUpdateProduitReapproInfo) {
                    produit.setQtyAppro(c.stockObjectif());
                    produit.setQtySeuilMini(c.margeSecurite());
                    produit.setUpdatedAt(LocalDateTime.now());
                    toSaveProduits.add(produit);

                    // Auto-renseigner les paramètres rayon/réserve si non définis
                    List<StockProduit> spList = stockProduitsByProduitId.getOrDefault(produit.getId(), List.of());
                    for (StockProduit sp : spList) {
                        if (autoFillStockProduitParams(sp, c)) {
                            toSaveStockProduits.add(sp);
                        }
                    }
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

        if (!toSaveStockProduits.isEmpty()) {
            stockProduitRepository.saveAll(toSaveStockProduits);
            LOG.debug("{} StockProduit mis à jour automatiquement (params rayon/réserve)",
                toSaveStockProduits.size());
        }

        return new BatchResult(successCount, errorCount);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Calculs métier
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Calcule la VMM pondérée (Ventes Mensuelles Moyennes) pour un produit.
     * <p>
     * <b>Exclusion des mois de rupture fournisseur :</b><br>
     * Les mois où le produit était en rupture fournisseur documentée
     * ({@code est_rupture_fournisseur = TRUE}) sont exclus du calcul.
     * En effet, les ventes de ces mois sont artificiellement basses et biaisent
     * la VMM à la baisse, ce qui perpétuerait la rupture.
     * <br>
     * Si aucun mois valide n'est disponible (ex: produit toujours en rupture),
     * on utilise tous les mois disponibles comme fallback.
     * </p>
     *
     * @param produitId ID du produit
     * @param nbMois    Nombre de mois d'historique souhaités
     * @return VMM arrondie à l'entier supérieur, 0 si aucune donnée
     */
    public int calculateVMM(Integer produitId, int nbMois) {
        //  utiliser les mois valides (hors rupture fournisseur) en priorité
        List<VentesMensuellesAgregees> ventes = ventesAgregeesRepository
            .findLastNValidMonthsByProduit(produitId, nbMois);

        //si aucun mois valide, utiliser tous les mois (y compris rupture)
        if (ventes.isEmpty()) {
            ventes = ventesAgregeesRepository.findLastNMonthsByProduit(produitId, nbMois);
            if (!ventes.isEmpty()) {
                LOG.debug("VMM produit {} : aucun mois valide, fallback sur {} mois incluant ruptures",
                    produitId, ventes.size());
            }
        }

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
        //Ajouter le stock de rotation (fréquence de commande)
        int rotationStock = computeRotationStock(config, vmm);
        int stockObjectif = vmm + marge + rotationStock;
        boolean limitePeremption = config.getLimitePeremption() != null
            ? config.getLimitePeremption()
            : classeConfig.getLimitePeremption();
        if (limitePeremption && vmm > 0) {
            stockObjectif = Math.min(stockObjectif, vmm * 3);
            LOG.debug("Limite péremption appliquée pour produit {}: {}", produitId, stockObjectif);
        }
        return stockObjectif;
    }

    /**
     * Stock de rotation = VMM × (fréquence_commande_jours / 30).
     * Représente la quantité consommée entre deux commandes successives.
     * Exemple : VMM=30, fréquence=7j → rotation=7 unités (≈ 1 semaine de ventes).
     */
    private int computeRotationStock(SemoisConfiguration config, int vmm) {
        if (vmm == 0) return 0;
        int freqJours = resolveFrequenceCommandeJours(config);
        return (int) (vmm * freqJours / 30.0);
    }

    /**
     * Résout la fréquence de commande en cascade :
     * <ol>
     *   <li>{@code SemoisConfiguration.frequenceCommandeJours} — surcharge par produit</li>
     *   <li>{@code Fournisseur.frequenceCommandeJours} — surcharge par fournisseur</li>
     *   <li>{@code GroupeFournisseur.frequenceCommandeJours} — défaut par groupe grossiste</li>
     *   <li>Défaut code : 7 jours (commande hebdomadaire)</li>
     * </ol>
     */
    private int resolveFrequenceCommandeJours(SemoisConfiguration config) {
        if (config.getFrequenceCommandeJours() != null) {
            return config.getFrequenceCommandeJours();
        }
        var fp = config.getProduit().getFournisseurProduitPrincipal();
        if (fp != null && fp.getFournisseur() != null) {
            var f = fp.getFournisseur();
            if (f.getFrequenceCommandeJours() != null) {
                return f.getFrequenceCommandeJours();
            }
            var gf = f.getGroupeFournisseur();
            if (gf != null && gf.getFrequenceCommandeJours() != null) {
                return gf.getFrequenceCommandeJours();
            }
        }
        return 7; // commande hebdomadaire par défaut
    }

    private record AllCalculs(int vmm, int margeSecurite, int stockObjectif, int stockActuel, int quantiteACommander) {}

    private int getStockActuel(Integer produitId) {
        Integer stock = stockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId(
             EntityConstant.DEFAULT_MAGASIN, produitId);
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

        // Axe 1 — Stock virtuel : déduire les commandes REQUESTED de la quantité à commander
        int pendingQty = getPendingQty(produitId);
        int qteACommander = Math.max(0, c.quantiteACommander() - pendingQty);

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
            qteACommander,
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
        Integer fournisseurId,String niveauUrgence,
        Pageable pageable
    ) {
        LOG.debug("Récupération suggestions SEMOIS depuis vue - search: {}, classe: {}, fournisseur: {}, page: {}",
            search, classeCriticite, fournisseurId, pageable.getPageNumber());

        Page<SemoisSuggestionView> viewPage = semoisSuggestionViewRepository.findAllWithFilters(
            search, classeCriticite, fournisseurId,niveauUrgence, pageable);

        List<Integer> produitIds = viewPage.getContent().stream()
            .map(SemoisSuggestionView::getProduitId)
            .toList();
        Map<Integer, Integer> pendingQtyMap = loadPendingOrderQtyBatch(produitIds);

        List<SemoisSuggestionDTO> suggestions = viewPage.getContent().stream()
            .map(view -> toDTO(view, pendingQtyMap.getOrDefault(view.getProduitId(), 0)))
            // Aligne avec Winpharma/Pharmagest : un produit couvert par commande en cours
            // (qte nette = 0 après soustraction pending) n'apparaît pas dans les suggestions.
            .filter(dto -> dto.quantiteACommander() > 0)
            .toList();

        return new PageImpl<>(suggestions, pageable, viewPage.getTotalElements());
    }

    /** Tous les produits urgents (stockActuel < margeSecurite) sans pagination. */
    @Transactional(readOnly = true)
    public List<SemoisSuggestionDTO> getAllUrgentSuggestions() {
        return semoisSuggestionViewRepository.findAllUrgents().stream()
            .map(this::toDTO)
            .toList();
    }

    /** Fournisseurs distincts ayant des produits SEMOIS configurés. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getDistinctFournisseurs() {
        return semoisSuggestionViewRepository.findDistinctFournisseurs().stream()
            .map(row -> Map.<String, Object>of(
                "fournisseurId", row[0],
                "fournisseurLibelle", row[1] != null ? row[1].toString() : ""
            ))
            .toList();
    }

    /**
     * Convertit une vue SEMOIS en DTO, en ajustant la quantité à commander
     * pour tenir compte des commandes en attente (stock virtuel).
     *
     * @param view       Vue SEMOIS depuis la vue matérialisée
     * @param pendingQty Quantité déjà commandée (REQUESTED) en attente de livraison
     */
    private SemoisSuggestionDTO toDTO(SemoisSuggestionView view, int pendingQty) {
        int qteVueACommander = view.getQuantiteACommander() != null ? view.getQuantiteACommander().intValue() : 0;
        // qteACommander ajustée = MAX(0, stockObjectif - (stockActuel + pendingQty))
        //       = MAX(0, qteVueACommander - pendingQty)
        int qteACommander = Math.max(0, qteVueACommander - pendingQty);

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
            qteACommander,
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

    private SemoisSuggestionDTO toDTO(SemoisSuggestionView view) {
        return toDTO(view, 0);
    }

    // ──────────────────────────────────────────────────────────────────────────
    //  Dashboard réappro temps réel
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Construit le tableau de bord de réapprovisionnement en temps réel.
     * <p>
     * Agrège les indicateurs clés depuis {@code v_semois_suggestion} :
     * compteurs par niveau d'urgence, répartition par classe, top produits urgents.
     * </p>
     *
     * @return DTO consolidé du tableau de bord réappro
     */
    @Transactional(readOnly = true)
    public ReapproDashboardDTO getDashboard() {

        List<Object[]> globalRows = semoisSuggestionViewRepository.getDashboardGlobalStats();
        Object[] globalRow = globalRows.isEmpty() ? new Object[7] : globalRows.getFirst();
        long totalProduits         = toLong(globalRow[0]);
        long nbRupture             = toLong(globalRow[1]);
        long nbSousSeuil           = toLong(globalRow[2]);
        long nbOk                  = toLong(globalRow[3]);
        long nbSurstock            = toLong(globalRow[4]);
        long nbSansConfig          = toLong(globalRow[5]);
        long totalUnitesManquantes = toLong(globalRow[6]);

        //  Répartition par classe
        List<ReapproDashboardDTO.ClasseBreakdown> parClasse = semoisSuggestionViewRepository
            .getDashboardStatsByClasse()
            .stream()
            .map(row -> {
                ClasseCriticite classe = ClasseCriticite.valueOf((String) row[0]);
                return new ReapproDashboardDTO.ClasseBreakdown(
                    classe,
                    toLong(row[1]),
                    toLong(row[2]),
                    toLong(row[3]),
                    toLong(row[4]),
                    toLong(row[5])
                );
            })
            .toList();

        List<ReapproDashboardDTO.TopUrgentDTO> topUrgents = semoisSuggestionViewRepository
            .findTopUrgentProducts(10)
            .stream()
            .map(row -> new ReapproDashboardDTO.TopUrgentDTO(
                toInt(row[0]),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                row[4] != null ? ClasseCriticite.valueOf((String) row[4]) : null,
                toInt(row[5]),
                toInt(row[6]),
                toInt(row[7]),
                toLong(row[8]),
                toLong(row[9]),
                toDouble(row[10])
            ))
            .toList();

        return new ReapproDashboardDTO(
            totalProduits, nbRupture, nbSousSeuil, nbOk, nbSurstock, nbSansConfig,
            totalUnitesManquantes, parClasse, topUrgents
        );
    }

    /**
     * Conversion sécurisée vers long pour les résultats de requêtes natives.
     * Gère les cas Hibernate 7 où un scalaire peut être wrappé dans Object[].
     */
    private static long toLong(Object val) {
        if (val == null) return 0L;

        if (val instanceof Object[] arr) {
            return arr.length > 0 ? toLong(arr[0]) : 0L;
        }
        return ((Number) val).longValue();
    }

    /**
     * Conversion sécurisée vers int pour les résultats de requêtes natives.
     * Gère les cas Hibernate 7 où un scalaire peut être wrappé dans Object[].
     */
    private static int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Object[] arr) {
            return arr.length > 0 ? toInt(arr[0]) : 0;
        }
        return ((Number) val).intValue();
    }

    /**
     * Conversion sécurisée vers double pour les résultats de requêtes natives.
     * Gère les cas Hibernate 7 où un scalaire peut être wrappé dans Object[].
     */
    private static double toDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Object[] arr) {
            return arr.length > 0 ? toDouble(arr[0]) : 0.0;
        }
        return ((Number) val).doubleValue();
    }


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


    // ──────────────────────────────────────────────────────────────────────────
    // Auto-calcul paramètres rayon/réserve
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Auto-renseigne les paramètres rayon/réserve d'un {@link StockProduit} si non définis.
     * <p>
     * <b>Règle de protection :</b> une valeur saisie manuellement (non null et > 0) n'est jamais
     * écrasée. L'auto-calcul ne s'applique qu'aux valeurs à zéro ou null.
     * </p>
     * <b>Paramètres auto-calculés par type de stockage :</b>
     * <ul>
     *   <li><b>PRINCIPAL (rayon)</b> :
     *     <ul>
     *       <li>{@code seuilMini} = VMM/4 ≈ 1 semaine de ventes (point de déclenchement réassort rayon)</li>
     *       <li>{@code stockReassort} = VMM/4 (quantité à transférer depuis la réserve)</li>
     *       <li>{@code stockMaxi} = max(VMM, margeSecurité×2) (évite la surcharge du rayon)</li>
     *     </ul>
     *   </li>
     *   <li><b>SAFETY_STOCK (réserve)</b> :
     *     <ul>
     *       <li>{@code seuilMini} = margeSecurite (point de commande fournisseur)</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param sp StockProduit à mettre à jour
     * @param c  Calculs SEMOIS (VMM, marge sécurité, stock objectif)
     * @return {@code true} si au moins un champ a été modifié
     */
    private boolean autoFillStockProduitParams(StockProduit sp, AllCalculs c) {
        if (c.vmm() == 0 && c.margeSecurite() == 0) {
            return false; // Pas de données de ventes : aucune auto-valeur fiable
        }
        StorageType type = sp.getStorage().getStorageType();
        boolean modified = false;

        if (type == StorageType.PRINCIPAL) {
            // Rayon : seuil mini ≈ 1 semaine de ventes (VMM/4, minimum 1)
            int seuilRayon = Math.max(1, c.vmm() / 4);
            if (isNullOrZero(sp.getSeuilMini())) {
                sp.setSeuilMini(seuilRayon);
                modified = true;
            }
            // Quantité de réassort rayon = 1 semaine de ventes
            if (isNullOrZero(sp.getStockReassort())) {
                sp.setStockReassort(seuilRayon);
                modified = true;
            }
            // Stock maxi rayon = max(VMM, 2 × marge sécurité) — évite la surcharge
            int stockMaxiRayon = Math.max(Math.max(1, c.vmm()), c.margeSecurite() * 2);
            if (isNullOrZero(sp.getStockMaxi())) {
                sp.setStockMaxi(stockMaxiRayon);
                modified = true;
            }
        } else if (type == StorageType.SAFETY_STOCK) {
            // Réserve : seuil mini = marge de sécurité (point de commande fournisseur)
            if (isNullOrZero(sp.getSeuilMini()) && c.margeSecurite() > 0) {
                sp.setSeuilMini(c.margeSecurite());
                modified = true;
            }
        }

        if (modified) {
            sp.setUpdatedAt(LocalDateTime.now());
        }
        return modified;
    }

    /** Retourne true si la valeur est null ou égale à 0 (non saisie manuellement). */
    private boolean isNullOrZero(Integer value) {
        return value == null || value == 0;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Saisonnalité automatique
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Calcule le coefficient saisonnier pour le mois actuel en comparant les ventes
     * du même mois des 2 années précédentes avec la VMM stockée.
     * <p>
     * <b>Formule (inspirée Winpharma/Périscopie sur 24 mois) :</b>
     * <pre>
     * coeff_N1 = ventes(même_mois, N-1) / VMM_stockée
     * coeff_N2 = ventes(même_mois, N-2) / VMM_stockée  [si disponible]
     * coeff_final = moyenne(coeff_N1, coeff_N2) borné [0.50 ; 3.00]
     * </pre>
     * Retourne 1.0 (facteur neutre) si VMM = 0 ou aucune donnée historique.
     * </p>
     *
     * @param config     Configuration SEMOIS (fournit la VMM du dernier calcul)
     * @param saisonData Map anneeMois → quantiteVendue pour ce produit
     * @param moisN1     Même mois, année N-1 (format YYYY-MM)
     * @param moisN2     Même mois, année N-2 (format YYYY-MM)
     * @return Coefficient saisonnier arrondi à 2 décimales, borné [0.50 ; 3.00]
     */
    private BigDecimal computeSeasonalFactor(
        SemoisConfiguration config,
        Map<String, Integer> saisonData,
        String moisN1,
        String moisN2
    ) {
        int vmmBase = config.getVmmCalcule() != null ? config.getVmmCalcule() : 0;
        if (vmmBase == 0 || saisonData.isEmpty()) {
            return BigDecimal.ONE;
        }

        List<Double> coefficients = new ArrayList<>();

        Integer ventesN1 = saisonData.get(moisN1);
        if (ventesN1 != null && ventesN1 > 0) {
            coefficients.add((double) ventesN1 / vmmBase);
        }

        Integer ventesN2 = saisonData.get(moisN2);
        if (ventesN2 != null && ventesN2 > 0) {
            coefficients.add((double) ventesN2 / vmmBase);
        }

        if (coefficients.isEmpty()) {
            return BigDecimal.ONE;
        }

        double avg = coefficients.stream().mapToDouble(Double::doubleValue).average().orElse(1.0);
        // Borne [0.50 ; 3.00] pour éviter les aberrations (ex: rupture fournisseur N-1)
        double bounded = Math.clamp(avg, 0.50, 3.0);
        return BigDecimal.valueOf(bounded).setScale(2, RoundingMode.HALF_UP);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers privés (infrastructure)
    // ──────────────────────────────────────────────────────────────────────────

    private record BatchResult(int successCount, int errorCount) {}

    /**
     * Axe 1 — Retourne la quantité en attente de livraison pour un produit spécifique.
     * Commandes dont le statut est REQUESTED (envoyées, non encore réceptionnées).
     */
    private int getPendingQty(Integer produitId) {
        List<Object[]> rows = orderLineRepository.findPendingQtyByProduitIds(List.of(produitId));
        return rows.stream()
            .filter(row -> ((Number) row[0]).intValue() == produitId)
            .mapToInt(row -> ((Number) row[1]).intValue())
            .findFirst()
            .orElse(0);
    }

    /**
     * Axe 1 — Batch-charge les quantités en attente de livraison pour une liste de produits.
     * Un seul appel SQL pour toute la page SEMOIS.
     *
     * @param produitIds IDs des produits de la page courante
     * @return Map produit_id → quantité en attente (seulement les produits avec qté > 0)
     */
    private Map<Integer, Integer> loadPendingOrderQtyBatch(List<Integer> produitIds) {
        if (produitIds.isEmpty()) return Map.of();
        List<Object[]> rows = orderLineRepository.findPendingQtyByProduitIds(produitIds);
        return rows.stream().collect(Collectors.toMap(
            row -> ((Number) row[0]).intValue(),
            row -> ((Number) row[1]).intValue()
        ));
    }

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

package com.kobe.warehouse.service.semois;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.AppConfiguration;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.SemoisSuggestionView;
import com.kobe.warehouse.domain.VentesMensuellesAgregees;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ModelReapprovisionnement;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.SemoisSuggestionViewRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.VentesMensuellesAgregeesRepository;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.dto.SemoisSuggestionDTO;
import org.apache.commons.lang3.StringUtils;
import jakarta.persistence.EntityManager;
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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Service de calcul SEMOIS (Stock Économique Mensuel d'Objectif Interne de Sécurité).
 * Implémente la méthode SEMOIS pour le calcul des stocks objectifs et suggestions de réapprovisionnement.
 * <p>
 * Formules SEMOIS:
 * - VMM (Ventes Mensuelles Moyennes) = Moyenne pondérée des N derniers mois
 * - Marge de Sécurité = VMM × (délai_livraison × coefficient_sécurité / 30)
 * - Stock Objectif = VMM + Marge de Sécurité
 * - Quantité à Commander = MAX(0, Stock Objectif - Stock Actuel)
 */
@Service
@Transactional
public class SemoisCalculationService {

    private static final Logger LOG = LoggerFactory.getLogger(SemoisCalculationService.class);
    /**
     * Clé de configuration pour la date du dernier calcul SEMOIS.
     */
    private final static String APP_LAST_DAY_SEMOIS_CALCULATION = "APP_LAST_DAY_SEMOIS_CALCULATION";


    @Value("${pharma-smart.semois.batch-size:100}")
    private int batchSize;

    private final SemoisConfigurationRepository semoisConfigRepository;
    private final SemoisSuggestionViewRepository semoisSuggestionViewRepository;
    private final VentesMensuellesAgregeesRepository ventesAgregeesRepository;
    private final ProduitRepository produitRepository;
    private final StockProduitRepository stockProduitRepository;
    private final EntityManager entityManager;
    private final AppConfigurationService appConfigurationService;


    public SemoisCalculationService(
        SemoisConfigurationRepository semoisConfigRepository,
        SemoisSuggestionViewRepository semoisSuggestionViewRepository,
        VentesMensuellesAgregeesRepository ventesAgregeesRepository,
        ProduitRepository produitRepository,
        StockProduitRepository stockProduitRepository,
        EntityManager entityManager,
        AppConfigurationService appConfigurationService
    ) {
        this.semoisConfigRepository = semoisConfigRepository;
        this.semoisSuggestionViewRepository = semoisSuggestionViewRepository;
        this.ventesAgregeesRepository = ventesAgregeesRepository;
        this.produitRepository = produitRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.entityManager = entityManager;
        this.appConfigurationService = appConfigurationService;
    }

    /**
     * Calcule le VMM (Ventes Mensuelles Moyennes) avec pondération.
     * Formule: VMM = Σ(Ventes_mois_i × Poids_i) / Σ(Poids_i)
     * Poids: mois le plus récent = nbMois, le plus ancien = 1
     *
     * @param produitId ID du produit
     * @param nbMois    Nombre de mois d'historique à utiliser
     * @return VMM calculé
     */
    public int calculateVMM(Integer produitId, int nbMois) {
        List<VentesMensuellesAgregees> ventes = ventesAgregeesRepository
            .findLastNMonthsByProduit(produitId, nbMois);

        if (ventes.isEmpty()) {
            return 0;
        }

        // Calcul moyenne mobile pondérée
        // Poids: mois le plus récent = nbMois, le plus ancien = 1
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

        return sommePonderee
            .divide(sommePoids, RoundingMode.HALF_UP)
            .intValue();
    }

    /**
     * Calcule la marge de sécurité SEMOIS.
     * Formule: Marge = VMM × (délai_livraison × coefficient_sécurité / 30) × facteur_saisonnier
     *
     * @param config Configuration SEMOIS du produit
     * @return Marge de sécurité calculée
     */
    public int calculateMargeSecurite(SemoisConfiguration config, Integer produitId) {


        int vmm = calculateVMM(produitId, config.getNbMoisHistorique());
        if (vmm == 0) {
            return 0;
        }

        BigDecimal marge = computeMarge(config, vmm);

        return marge.intValue();
    }

    private static @NonNull BigDecimal computeMarge(SemoisConfiguration config, int vmm) {
        int delaiJours = config.getDelaiLivraisonJours();
        BigDecimal coefficient = config.getCoefficientSecurite();
        BigDecimal facteurSaisonnier = Objects.requireNonNullElse(
            config.getFacteurSaisonnierActuel(),
            BigDecimal.ONE
        );

        // Marge = VMM × (Délai × Coefficient / 30) × Facteur Saisonnier
        return BigDecimal.valueOf(vmm)
            .multiply(BigDecimal.valueOf(delaiJours))
            .multiply(coefficient)
            .divide(BigDecimal.valueOf(30), RoundingMode.HALF_UP)
            .multiply(facteurSaisonnier);
    }

    /**
     * Calcule le stock objectif SEMOIS.
     * Formule: Stock Objectif = VMM + Marge de Sécurité
     * Si limite péremption: Stock Objectif = MIN(Stock Objectif, VMM × 3)
     *
     * @param produitId ID du produit
     * @return Stock objectif calculé
     */
    public int calculateStockObjectif(SemoisConfiguration config, Integer produitId) {
        int vmm = calculateVMM(produitId, config.getNbMoisHistorique());
        int margeSecurite = calculateMargeSecurite(config, produitId);

        int stockObjectif = vmm + margeSecurite;

        // Ajustement péremption: limiter à 3 mois de VMM
        if (Boolean.TRUE.equals(config.getLimitePeremption()) && vmm > 0) {
            int limitePeremption = vmm * 3;
            stockObjectif = Math.min(stockObjectif, limitePeremption);
            LOG.debug("Limite péremption appliquée pour produit {}: {} → {}",
                produitId, stockObjectif, stockObjectif);
        }

        return stockObjectif;
    }

    /**
     * Calcule la quantité à commander.
     * Formule: Quantité = MAX(0, Stock Objectif - Stock Actuel)
     *
     * @param produitId ID du produit
     * @return Quantité à commander
     */
    public int calculateQuantiteACommander(SemoisConfiguration config, Integer produitId) {
        int stockObjectif = calculateStockObjectif(config, produitId);
        int stockActuel = getStockActuel(produitId);

        int quantite = stockObjectif - stockActuel;
        return Math.max(0, quantite);
    }

    /**
     * Récupère le stock actuel total d'un produit (tous stockages)
     *
     * @param produitId ID du produit
     * @return Stock actuel total
     */
    private int getStockActuel(Integer produitId) {
        Integer stock = stockProduitRepository.findTotalQuantityByMagasinIdIdAndProduitId(
            (int) EntityConstant.DEFAULT_MAGASIN, produitId);
        return stock != null ? stock : 0;
    }

    /**
     * Obtient la suggestion SEMOIS complète pour un produit.
     *
     * @param produitId ID du produit
     * @return Suggestion SEMOIS complète
     */
    @Transactional(readOnly = true)
    public SemoisSuggestionDTO getSuggestionForProduct(Integer produitId) {
        SemoisConfiguration config = semoisConfigRepository
            .findByProduitId(produitId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Configuration SEMOIS non trouvée pour produit " + produitId));

        Produit produit = config.getProduit();

        int vmm = calculateVMM(produitId, config.getNbMoisHistorique());
        int margeSecurite = calculateMargeSecurite(config, produitId);
        int stockObjectif = calculateStockObjectif(config, produitId);
        int stockActuel = getStockActuel(produitId);
        int quantiteACommander = calculateQuantiteACommander(config, produitId);

        String codeCip = produit.getFournisseurProduitPrincipal() != null ?
            produit.getFournisseurProduitPrincipal().getCodeCip() : null;

        return new SemoisSuggestionDTO(
            produitId,
            produit.getLibelle(),
            codeCip,
            config.getClasseCriticite(),
            vmm,
            margeSecurite,
            stockObjectif,
            stockActuel,
            quantiteACommander,
            config.getDelaiLivraisonJours(),
            config.getCoefficientSecurite(),
            config.getFacteurSaisonnierActuel(),
            config.getDateDernierCalcul()
        );
    }

    /**
     * Récupère toutes les suggestions SEMOIS depuis la vue matérialisée mv_semois_suggestion.
     * <p>
     * Cette méthode lit directement depuis la vue matérialisée au lieu de recalculer à la volée.
     * BEAUCOUP plus performant: 1 seule requête SQL vs N calculs en temps réel.
     * <p>
     * IMPORTANT: La vue est rafraîchie quotidiennement à 3h du matin après le recalcul SEMOIS.
     * Les données peuvent avoir jusqu'à 24h de retard par rapport au calcul en temps réel.
     *
     * @param search          Recherche texte dans libellé ou code CIP (optionnel)
     * @param classeCriticite Filtre par classe de criticité (optionnel)
     * @param pageable        Pagination et tri
     * @return Page de suggestions depuis la vue matérialisée
     */
    @Transactional(readOnly = true)
    public Page<SemoisSuggestionDTO> getAllSuggestions(
        String search,
        ClasseCriticite classeCriticite,
        Pageable pageable
    ) {
        LOG.debug("Récupération suggestions SEMOIS depuis vue matérialisée - search: {}, classe: {}, page: {}",
            search, classeCriticite, pageable.getPageNumber());

        // Lire directement depuis la vue matérialisée (très performant)
        Page<SemoisSuggestionView> viewPage = semoisSuggestionViewRepository.findAllWithFilters(
            search,
            classeCriticite,
            pageable
        );

        // Mapper vers DTOs
        List<SemoisSuggestionDTO> suggestions = viewPage.getContent().stream()
            .map(this::toDTO)
            .toList();

        return new PageImpl<>(suggestions, pageable, viewPage.getTotalElements());
    }

    /**
     * Convertit SemoisSuggestionView (vue matérialisée) vers SemoisSuggestionDTO.
     * Note: facteurSaisonnier est absent de la vue, défaut à 1.0.
     *
     * @param view Entité de la vue matérialisée
     * @return DTO pour l'API
     */
    private SemoisSuggestionDTO toDTO(SemoisSuggestionView view) {
        return new SemoisSuggestionDTO(
            view.getProduitId(),
            view.getLibelle(),
            view.getCodeCip(),
            view.getClasseCriticite(),
            view.getVmm(),
            view.getMargeSecurite(),
            view.getStockObjectif(),
            view.getStockActuel(),
            view.getQuantiteACommander(),
            view.getDelaiLivraisonJours(),
            view.getCoefficientSecurite() != null
                ? BigDecimal.valueOf(view.getCoefficientSecurite()).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ONE,
            BigDecimal.ONE, // facteurSaisonnier absent de la vue, défaut à 1.0
            view.getDateDernierCalcul() != null
                ? LocalDateTime.ofInstant(view.getDateDernierCalcul(), java.time.ZoneId.systemDefault())
                : null
        );
    }

    /**
     * Recalcule toutes les configurations SEMOIS et met à jour les caches.
     * Traitement par lots (batch) pour éviter les problèmes de mémoire et timeout de transaction.
     * Exécuté quotidiennement uniquement si le modèle SEMOIS est configuré.
     * <p>
     * Stratégie:
     * - Pagination pour traiter par lots de {@link #batchSize} produits
     * - Transaction indépendante par batch (commit après chaque batch)
     * - Logs de progression détaillés
     * - Rafraîchissement de la vue matérialisée à la fin
     */
    @Scheduled(cron = "0 */15 12-14 * * *")
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // Pas de transaction globale
    public void recalculateAllConfigurations() {
        Optional<AppConfiguration> semoisConfig = getLastSemoisCalculationDate();
        LocalDate lastSemoisCalculationDate = semoisConfig
            .map(config -> LocalDate.parse(config.getValue()))
            .orElse(null);
        if (lastSemoisCalculationDate != null && lastSemoisCalculationDate.isEqual(LocalDate.now())) {
            LOG.info("Recalcul SEMOIS déjà effectué aujourd'hui ({}), skip", lastSemoisCalculationDate);
            return;
        }
        ModelReapprovisionnement model = getConfiguredModel();
        boolean isSemoisModel = model == ModelReapprovisionnement.SEMOIS;

        Optional<AppConfiguration> lastReapproConfigOpt = getLastReapproDate();
        LocalDate lastReapproDate = lastReapproConfigOpt
            .map(config -> LocalDate.parse(config.getValue()))
            .orElse(null);

        boolean canUpdateProduitReapproInfo = isNull(lastReapproDate)
            || lastReapproDate.getMonthValue() != LocalDate.now().getMonthValue();

        // Compter le nombre total de configurations
        long totalCount = semoisConfigRepository.count();
        LOG.info("Début recalcul SEMOIS - {} produits à traiter par batches de {}",
            totalCount, batchSize);

        if (totalCount == 0) {
            LOG.warn(" Aucune configuration SEMOIS trouvée - Recalcul annulé");
            return;
        }

        // Compteurs globaux (thread-safe car potentiellement multi-thread)
        AtomicInteger totalSuccess = new AtomicInteger(0);
        AtomicInteger totalErrors = new AtomicInteger(0);

        // Calculer le nombre de pages
        int totalPages = (int) Math.ceil((double) totalCount / batchSize);
        long startTime = System.currentTimeMillis();

        // Traiter page par page
        for (int pageNumber = 0; pageNumber < totalPages; pageNumber++) {
            try {
                Pageable pageable = PageRequest.of(pageNumber, batchSize);
                // Utiliser findAllWithProduit() pour eager fetch et éviter LazyInitializationException
                Page<SemoisConfiguration> page = semoisConfigRepository.findAllWithProduit(pageable);

                LOG.info("Traitement batch {}/{} ({} produits)",
                    pageNumber + 1, totalPages, page.getNumberOfElements());

                // Traiter le batch dans une transaction indépendante
                BatchResult batchResult = processBatch(page.getContent(), canUpdateProduitReapproInfo);

                totalSuccess.addAndGet(batchResult.successCount());
                totalErrors.addAndGet(batchResult.errorCount());

                LOG.info("Batch {}/{} terminé - Succès: {}, Erreurs: {} (Total: {}/{})",
                    pageNumber + 1, totalPages,
                    batchResult.successCount(), batchResult.errorCount(),
                    totalSuccess.get(), totalErrors.get());

            } catch (Exception e) {
                LOG.error(" Erreur critique lors du traitement du batch {}/{} - Skip",
                    pageNumber + 1, totalPages, e);
                totalErrors.addAndGet(batchSize); // Compter tout le batch comme erreur
            }
        }

        // Rafraîchir vue matérialisée si elle existe
        refreshMaterializedView();

        // Mettre à jour la date du dernier recalcul
        if (canUpdateProduitReapproInfo && isSemoisModel) {
            updateAppConfigurationDate(lastReapproConfigOpt.orElse(null));
        }
        updateAppConfigurationDate(semoisConfig.orElse(null));
        long duration = System.currentTimeMillis() - startTime;
        LOG.info("Recalcul SEMOIS terminé en {}ms - Total: {} produits - Succès: {}, Erreurs: {}",
            duration, totalCount, totalSuccess.get(), totalErrors.get());
    }

    /**
     * Traite un batch de configurations SEMOIS dans une transaction indépendante.
     * Permet le commit après chaque batch pour éviter les timeouts de transaction.
     *
     * @param configs                     Liste des configurations à traiter
     * @param canUpdateProduitReapproInfo Si true, met à jour qtyAppro et qtySeuilMini du produit
     * @return Résultat du traitement (nombre de succès et erreurs)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected BatchResult processBatch(List<SemoisConfiguration> configs, boolean canUpdateProduitReapproInfo) {
        int successCount = 0;
        int errorCount = 0;

        for (SemoisConfiguration config : configs) {
            try {
                // Recalculer les valeurs SEMOIS
                ReapproCalculationResult reapproCalculationResult = recalculateProduit(config);

                // Optionnellement mettre à jour le produit (une fois par mois)
                if (canUpdateProduitReapproInfo) {
                    updateProduitQtyApproAndSeuilMini(
                        config.getProduit(),
                        reapproCalculationResult.stockObjectif(),
                        reapproCalculationResult.margeSecurite()
                    );
                }

                successCount++;
            } catch (Exception e) {
                LOG.error("Erreur recalcul SEMOIS pour produit {} - {}",
                    config.getProduit().getId(), e.getMessage());
                errorCount++;
            }
        }

        return new BatchResult(successCount, errorCount);
    }

    /**
     * Rafraîchit la vue matérialisée des suggestions SEMOIS.
     * Non transactionnel car appelé après les commits.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void refreshMaterializedView() {
        try {
            entityManager.createNativeQuery("REFRESH MATERIALIZED VIEW mv_semois_suggestion")
                .executeUpdate();
            LOG.info("Vue matérialisée mv_semois_suggestion rafraîchie");
        } catch (Exception e) {
            LOG.warn("Vue matérialisée mv_semois_suggestion non trouvée ou erreur refresh: {}",
                e.getMessage());
        }
    }

    /**
     * Met à jour la date du dernier recalcul de réapprovisionnement.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateLastReapproDate() {
        try {
            Optional<AppConfiguration> configOpt = getLastReapproDate();
            configOpt.ifPresent(config -> {
                config.setValue(LocalDate.now().toString());
                config.setUpdated(LocalDateTime.now());
                appConfigurationService.update(config);
                LOG.info("Date dernier recalcul mise à jour: {}", LocalDate.now());
            });
        } catch (Exception e) {
            LOG.error(" Erreur mise à jour date dernier recalcul", e);
        }
    }

    /**
     * Résultat du traitement d'un batch.
     *
     * @param successCount Nombre de produits traités avec succès
     * @param errorCount   Nombre de produits en erreur
     */
    private record BatchResult(int successCount, int errorCount) {
    }


    /**
     * Initialise la configuration SEMOIS pour un produit.
     * Utilise la classe de criticité effective du produit (getEffectiveClasseCriticite).
     *
     * @param produitId ID du produit
     * @return La configuration créée
     */
    @Transactional
    public SemoisConfiguration initializeConfiguration(Integer produitId) {
        // Vérifier si existe déjà
        Optional<SemoisConfiguration> existing = semoisConfigRepository.findByProduitId(produitId);
        if (existing.isPresent()) {
            LOG.warn("Configuration SEMOIS existe déjà pour produit {}", produitId);
            return existing.get();
        }

        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));

        // Utiliser la classe de criticité effective (override ou conversion depuis ABC)
        ClasseCriticite classe = produit.getEffectiveClasseCriticite();
        SemoisConfiguration config = SemoisConfiguration.createDefault(produit, classe);
        return semoisConfigRepository.save(config);
    }

    /**
     * Initialise la configuration SEMOIS pour un produit avec une classe spécifique (override).
     *
     * @param produitId ID du produit
     * @param classe    Classe de criticité (override, ignore la valeur du produit)
     * @return La configuration créée
     */
    @Transactional
    public SemoisConfiguration initializeConfiguration(Integer produitId, ClasseCriticite classe) {
        // Vérifier si existe déjà
        Optional<SemoisConfiguration> existing = semoisConfigRepository.findByProduitId(produitId);
        if (existing.isPresent()) {
            LOG.warn("Configuration SEMOIS existe déjà pour produit {}", produitId);
            return existing.get();
        }

        Produit produit = produitRepository.findById(produitId)
            .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + produitId));

        SemoisConfiguration config = SemoisConfiguration.createDefault(produit, classe);
        return semoisConfigRepository.save(config);
    }

    @Transactional
    public SemoisConfiguration create(Produit produit, ClasseCriticite classe) {

        SemoisConfiguration config = SemoisConfiguration.createDefault(produit, classe);
        return semoisConfigRepository.save(config);
    }


    /**
     * Initialise les configurations SEMOIS pour tous les produits actifs sans configuration.
     *
     * @return Nombre de configurations créées
     */
    @Transactional
    public int initializeAllMissingConfigurations() {
        LOG.info("🔄 Initialisation configurations SEMOIS manquantes...");

        int created = semoisConfigRepository.initializeAllMissingConfigurations();

        LOG.info("{} configurations SEMOIS initialisées", created);
        return created;
    }

    /**
     * Récupère le modèle de réapprovisionnement configuré.
     *
     * @return Le modèle configuré, CLASSIQUE par défaut
     */
    private ModelReapprovisionnement getConfiguredModel() {
        return appConfigurationService
            .findOneById(EntityConstant.APP_MODEL_REAPPRO)
            .map(AppConfiguration::getValue)
            .filter(StringUtils::isNotEmpty)
            .map(value -> {
                try {
                    return ModelReapprovisionnement.valueOf(value);
                } catch (IllegalArgumentException e) {
                    LOG.warn("Modèle de réapprovisionnement invalide '{}', utilisation du modèle CLASSIQUE par défaut", value);
                    return ModelReapprovisionnement.SEMOIS;
                }
            })
            .orElse(ModelReapprovisionnement.SEMOIS);
    }


    private ReapproCalculationResult recalculateProduit(SemoisConfiguration config) {

        Produit produit = config.getProduit();
        Integer produitId = produit.getId();
        int vmm = calculateVMM(produitId, config.getNbMoisHistorique());
        int stockObjectif = calculateStockObjectif(config, produitId);
        int margeSecurite = calculateMargeSecurite(config, produitId);

        config.updateCalculs(vmm, stockObjectif);
        semoisConfigRepository.save(config);

        return new ReapproCalculationResult(stockObjectif, margeSecurite);


    }

    private record ReapproCalculationResult(int stockObjectif, int margeSecurite) {
    }


    private void updateProduitQtyApproAndSeuilMini(Produit produit, int stockObjectif, int margeSecurite) {
        produit.setQtyAppro(stockObjectif);
        produit.setQtySeuilMini(margeSecurite);
        produit.setUpdatedAt(LocalDateTime.now());
        produitRepository.save(produit);

    }


    private Optional<AppConfiguration> getLastReapproDate() {
        return appConfigurationService.findOneById(EntityConstant.APP_LAST_DAY_REAPPRO);
    }

    private Optional<AppConfiguration> getLastSemoisCalculationDate() {
        return appConfigurationService.findOneById(APP_LAST_DAY_SEMOIS_CALCULATION);
    }

    private SemoisSuggestionDTO getSuggestionForProduct(SemoisConfiguration config) {
        Produit produit = config.getProduit();
        Integer produitId = produit.getId();
        int vmm = calculateVMM(produitId, config.getNbMoisHistorique());
        int margeSecurite = calculateMargeSecurite(config, produitId);
        int stockObjectif = calculateStockObjectif(config, produitId);
        int stockActuel = getStockActuel(produitId);
        int quantiteACommander = calculateQuantiteACommander(config, produitId);

        String codeCip = produit.getFournisseurProduitPrincipal() != null ?
            produit.getFournisseurProduitPrincipal().getCodeCip() : null;

        return new SemoisSuggestionDTO(
            produitId,
            produit.getLibelle(),
            codeCip,
            config.getClasseCriticite(),
            vmm,
            margeSecurite,
            stockObjectif,
            stockActuel,
            quantiteACommander,
            config.getDelaiLivraisonJours(),
            config.getCoefficientSecurite(),
            config.getFacteurSaisonnierActuel(),
            config.getDateDernierCalcul()
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void updateAppConfigurationDate(AppConfiguration config) {

        try {
            if (nonNull(config)) {
                config.setValue(LocalDate.now().toString());
                config.setUpdated(LocalDateTime.now());
                appConfigurationService.update(config);
            }

            LOG.info("Date dernier recalcul mise à jour: {}", LocalDate.now());
        } catch (Exception e) {
            LOG.error(" Erreur mise à jour date dernier recalcul", e);
        }
    }
}

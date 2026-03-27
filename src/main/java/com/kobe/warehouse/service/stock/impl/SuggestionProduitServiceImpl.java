package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SemoisClasseConfig;
import com.kobe.warehouse.domain.SemoisConfiguration;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.ClasseCriticite;
import com.kobe.warehouse.domain.enumeration.ModelReapprovisionnement;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SemoisClasseConfigRepository;
import com.kobe.warehouse.repository.SemoisConfigurationRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.EtatProduitService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.BudgetCommandeDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SemoisCommanderDTO;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import com.kobe.warehouse.service.dto.enumeration.Mois;
import com.kobe.warehouse.service.dto.records.QuantitySuggestion;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import com.kobe.warehouse.service.report.pdf.SuggestionPdfReportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import com.kobe.warehouse.service.stock.dto.QauntiteProduitVendus;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class SuggestionProduitServiceImpl implements SuggestionProduitService {

    private static final Logger LOG = LoggerFactory.getLogger(SuggestionProduitServiceImpl.class);

    private final SuggestionRepository suggestionRepository;
    private final SuggestionLineRepository suggestionLineRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final StorageService storageService;
    private final ReferenceService referenceService;
    private final AppConfigurationService appConfigurationService;
    private final EtatProduitService etatProduitService;
    private final CommandService commandService;
    private final CsvExportService csvExportService;
    private final SemoisConfigurationRepository semoisConfigurationRepository;
    private final SemoisClasseConfigRepository semoisClasseConfigRepository;
    private final EntityManager em;
    private final SuggestionPdfReportService suggestionPdfReportService;
    private final ProduitRepository produitRepository;
    /** Axe 1 — Stock virtuel : commandes REQUESTED en attente de livraison. */
    private final OrderLineRepository orderLineRepository;


    public SuggestionProduitServiceImpl(
        SuggestionRepository suggestionRepository,
        SuggestionLineRepository suggestionLineRepository,
        FournisseurProduitRepository fournisseurProduitRepository,
        StorageService storageService,
        ReferenceService referenceService,
        AppConfigurationService appConfigurationService,
        EtatProduitService etatProduitService,
        CommandService commandService,
        CsvExportService csvExportService,
        SemoisConfigurationRepository semoisConfigurationRepository,
        SemoisClasseConfigRepository semoisClasseConfigRepository,
        EntityManager em,
        SuggestionPdfReportService suggestionPdfReportService,
        ProduitRepository produitRepository,
        OrderLineRepository orderLineRepository
    ) {
        this.suggestionRepository = suggestionRepository;
        this.suggestionLineRepository = suggestionLineRepository;
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.storageService = storageService;
        this.referenceService = referenceService;
        this.appConfigurationService = appConfigurationService;
        this.etatProduitService = etatProduitService;
        this.commandService = commandService;
        this.csvExportService = csvExportService;
        this.semoisConfigurationRepository = semoisConfigurationRepository;
        this.semoisClasseConfigRepository = semoisClasseConfigRepository;
        this.em = em;
        this.suggestionPdfReportService = suggestionPdfReportService;
        this.produitRepository = produitRepository;
        this.orderLineRepository = orderLineRepository;
    }

    @Async
    @Override
    public void suggerer(List<QuantitySuggestion> quantitySuggestions, Magasin magasin, AppUser user) {
        if (CollectionUtils.isEmpty(quantitySuggestions)) return;

        boolean isSemois = appConfigurationService.getCurrentModelReappro() == ModelReapprovisionnement.SEMOIS;
        int couvertureMois = appConfigurationService.getCouvertureMoisClassique();

        // ── 1. Pré-filtrage : on garde les produits éligibles uniquement ──────────
        List<QuantitySuggestion> eligibles = quantitySuggestions.stream()
            .filter(q -> etatProduitService.canSuggere(q.produit().getId()))
            .toList();
        if (eligibles.isEmpty()) return;

        // ── 2. Batch-load SEMOIS configs (surcharges par produit, optionnel) ───────
        Set<Integer> allProduitIds = eligibles.stream()
            .map(q -> q.produit().getId())
            .collect(Collectors.toSet());
        Map<Integer, SemoisConfiguration> semoisConfigByProduitId = semoisConfigurationRepository
            .findByProduitIdIn(allProduitIds)
            .stream()
            .collect(Collectors.toMap(sc -> sc.getProduit().getId(), Function.identity()));

        // ── 3. Charger les configs de classe SEMOIS (5 lignes max, L2-cached) ─────
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigs = isSemois
            ? semoisClasseConfigRepository.findAll().stream()
                .collect(Collectors.toMap(SemoisClasseConfig::getClasseCriticite, Function.identity()))
            : Map.of();

        // ── 4. Batch-load VMM — par classe si SEMOIS (nbMoisHistorique spécifique),
        //       sinon global nthMois ──────────────────────────────────────────────
        Map<Integer, Integer> vmmByProduitId = isSemois
            ? loadVmmForProduitsBySemoisClass(eligibles, classeConfigs)
            : loadVmmForProduits(allProduitIds, appConfigurationService.getNthMoisConsommation());

        // ── 5. Batch-load lignes existantes (évite N+1 par produit) ───────────────
        Set<Integer> allFpIds = eligibles.stream()
            .map(q -> q.produit().getFournisseurProduitPrincipal().getId())
            .collect(Collectors.toSet());
        Map<Integer, SuggestionLine> existingLineByFpId = suggestionLineRepository
            .findAllByTypeSuggessionAndFournisseurProduitIdIn(TypeSuggession.AUTO, allFpIds)
            .stream()
            .collect(Collectors.toMap(l -> l.getFournisseurProduit().getId(), Function.identity()));

        // ── 5b. Batch-load commandes en attente — STOCK VIRTUEL (Axe 1) ──────────
        // Produits déjà commandés (REQUESTED) mais non encore reçus.
        // On les soustrait du calcul pour ne pas commander en double.
        Map<Integer, Integer> pendingQtyByProduitId = loadPendingOrderQty(allProduitIds);
        if (!pendingQtyByProduitId.isEmpty()) {
            LOG.debug("Stock virtuel chargé : {} produit(s) ont des commandes en attente", pendingQtyByProduitId.size());
        }

        // ── 6. Regroupement par fournisseur ──────────────────────────────────────
        Map<Fournisseur, List<QuantitySuggestion>> byFournisseur = eligibles.stream()
            .collect(Collectors.groupingBy(q -> q.produit().getFournisseurProduitPrincipal().getFournisseur()));

        List<Suggestion> suggestionsToSave = new ArrayList<>();
        List<SuggestionLine> linesToSave = new ArrayList<>();

        for (Map.Entry<Fournisseur, List<QuantitySuggestion>> entry : byFournisseur.entrySet()) {
            Fournisseur fournisseur = entry.getKey();
            List<QuantitySuggestion> values = entry.getValue();

            AtomicBoolean suggestionExist = new AtomicBoolean(false);
            Suggestion suggestion = getSuggestion(fournisseur, suggestionExist, magasin, user);
            boolean alreadyExisted = suggestionExist.get();

            for (QuantitySuggestion qs : values) {
                Produit produit = qs.produit();
                FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
                boolean isDetail = false;
                int quantitySold = qs.quantitySold();

                if (produit.getTypeProduit() == TypeProduit.DETAIL) {
                    isDetail = true;
                    produit = produit.getParent();
                    quantitySold = Math.ceilDiv(quantitySold, produit.getItemQty());
                }

                int produitAllSock = produit.getStockProduits().stream()
                    .filter(s -> s.getStorage().getMagasin().equals(magasin))
                    .mapToInt(StockProduit::getTotalStockQuantity)
                    .sum();
                if (isDetail) {
                    produitAllSock -= quantitySold;
                }

                // Axe 1 — Stock virtuel : stock physique + commandes REQUESTED en attente
                // Évite de commander des produits déjà en cours de livraison
                int qtesEnCommande = pendingQtyByProduitId.getOrDefault(produit.getId(), 0);
                int stockVirtuel = produitAllSock + qtesEnCommande;

                SemoisConfiguration semoisConfig = semoisConfigByProduitId.get(produit.getId());
                int vmm = vmmByProduitId.getOrDefault(produit.getId(), 0);

                // Utiliser stockVirtuel pour le seuil : si le stock virtuel couvre déjà l'objectif, pas de suggestion
                if (stockVirtuel > seuilReappro(produit, semoisConfig, fournisseur, vmm, couvertureMois, isSemois, classeConfigs)) continue;

                // Utiliser stockVirtuel pour le calcul de la quantité : on ne commande que le delta restant
                int qty = computeQtyReappro(produit, stockVirtuel, semoisConfig, fournisseur, vmm, couvertureMois, isSemois, classeConfigs);
                if (qty <= 0) continue; // Sécurité : ne jamais suggérer une quantité nulle ou négative
                SuggestionLine existingLine = existingLineByFpId.get(fournisseurProduit.getId());

                if (existingLine != null) {
                    existingLine.setQuantity(qty);
                    existingLine.setUpdatedAt(LocalDateTime.now());
                    linesToSave.add(existingLine);
                } else {
                    SuggestionLine newLine = new SuggestionLine();
                    newLine.setCreatedAt(LocalDateTime.now());
                    newLine.setUpdatedAt(newLine.getCreatedAt());
                    newLine.setQuantity(qty);
                    newLine.setFournisseurProduit(fournisseurProduit);
                    newLine.setSuggestion(suggestion);
                    suggestion.getSuggestionLines().add(newLine);
                    if (alreadyExisted) {
                        linesToSave.add(newLine);
                    }
                }
            }

            if (!suggestion.getSuggestionLines().isEmpty() || !linesToSave.isEmpty()) {
                suggestionsToSave.add(suggestion);
            }
        }

        // ── 7. Persister en batch ─────────────────────────────────────────────────
        suggestionRepository.saveAll(suggestionsToSave);
        suggestionLineRepository.saveAll(linesToSave);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<SuggestionProjection> getAllSuggestion(
        String search,
        Integer fournisseurId,
        TypeSuggession typeSuggession,
        StatutSuggession statut,
        Pageable pageable
    ) {
        Specification<Suggestion> specification = suggestionRepository.filterByDate(appConfigurationService.findSuggestionRetention());
        if (typeSuggession != null) {
            specification = specification.and(suggestionRepository.filterByType(typeSuggession));
        }
        if (statut != null) {
            specification = specification.and(suggestionRepository.filterByStatut(statut));
        }
        if (fournisseurId != null) {
            specification = specification.and(suggestionRepository.filterByFournisseurId(fournisseurId));
        }
        if (StringUtils.hasLength(search)) {
            specification = specification.and(suggestionRepository.filterByProduit(search));
        }

        return suggestionRepository.getAllSuggestion(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatut(StatutSuggession statut) {
        return suggestionRepository.countByStatut(statut);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SuggestionDTO> getSuggestionById(Integer id) {
        return suggestionRepository
            .findById(id)
            .map(suggestion ->
                new SuggestionDTO(suggestion).setSuggestionAggregator(suggestionLineRepository.getSuggestionData(suggestion.getId()))
            );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur() {
        return suggestionRepository.getParFournisseur(appConfigurationService.findSuggestionRetention());
    }

    @Override
    @Transactional(readOnly = true)
    public List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur(StatutSuggession statut) {
        if (statut == null) {
            return getSuggestionsParFournisseur();
        }
        return suggestionRepository.getParFournisseur(appConfigurationService.findSuggestionRetention(), statut);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SuggestionLineDTO> getSuggestionLinesByIdWithConsommation(Integer suggestionId, String search, String niveauUrgence, Pageable pageable) {
        Integer storageId = storageService.getDefaultMagasinMainStorage().getId();
        LocalDate dateRetention = LocalDate.now().minusDays(appConfigurationService.getNombreJourRetentionCommande());
        int nthMois = appConfigurationService.getNthMoisConsommation();
        return suggestionLineRepository.fetchSuggestionLinesWithConsommation(suggestionId, search, niveauUrgence, storageId, dateRetention, nthMois, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuggestionLineDTO> getAllSuggestionLines(Integer suggestionId, String search, String niveauUrgence) {
        return getSuggestionLinesByIdWithConsommation(suggestionId, search, niveauUrgence, Pageable.unpaged()).getContent();
    }

    @Override
    public void fusionnerSuggestion(Set<Integer> ids) throws GenericError {
        List<Suggestion> suggestions = suggestionRepository.findAllById(ids);
        if (!CollectionUtils.isEmpty(suggestions)) {
            suggestions.sort(Comparator.comparing(Suggestion::getUpdatedAt, Comparator.reverseOrder()));
            Suggestion suggestion = suggestions.getFirst();
            Set<SuggestionLine> suggestionLines = suggestion.getSuggestionLines();
            Fournisseur fournisseur = suggestion.getFournisseur();
            for (int i = 1; i < suggestions.size(); i++) {
                Suggestion suggestionToMerge = suggestions.get(i);
                if (!Objects.equals(fournisseur, suggestionToMerge.getFournisseur())) {
                    throw new GenericError("Vous ne pouvez pas fusionner des suggestions de fournisseurs differents");
                }
                suggestionToMerge
                    .getSuggestionLines()
                    .forEach(suggestionLine -> {
                        if (suggestionLines.contains(suggestionLine)) {
                            suggestionLineRepository.delete(suggestionLine);
                        } else {
                            suggestionLine.setSuggestion(suggestion);
                            suggestionLine.setUpdatedAt(LocalDateTime.now());
                            suggestionLineRepository.save(suggestionLine);
                        }
                    });

                suggestionRepository.delete(suggestionToMerge);
            }
            suggestionRepository.save(suggestion);
        }
    }

    @Override
    public void deleteSuggestion(Set<Integer> ids) {
        suggestionRepository.deleteAllById(ids);
    }

    @Override
    public void deleteSuggestionLine(Set<Integer> ids) {
        suggestionLineRepository.deleteAllById(ids);
    }

    @Override
    public void sanitize(Integer suggestionId) {
        Storage storage = storageService.getDefaultMagasinMainStorage();
        suggestionRepository
            .findById(suggestionId)
            .ifPresent(suggestion -> {
                List<SuggestionLine> linesToDelete = new ArrayList<>();
                suggestion
                    .getSuggestionLines()
                    .forEach(suggestionLine -> {
                        FournisseurProduit fournisseurProduit = suggestionLine.getFournisseurProduit();
                        Produit produit = fournisseurProduit.getProduit();
                        StockProduit stockProduit = produit
                            .getStockProduits()
                            .stream()
                            .filter(stock -> stock.getStorage().getId() == storage.getId())
                            .findFirst()
                            .orElse(new StockProduit());
                        int currentStock = 0;
                        if (stockProduit != null) {
                            currentStock = stockProduit.getTotalStockQuantity();
                        }
                        if ((produit.getStatus() != Status.ENABLE) || (produit.getQtySeuilMini() < currentStock)) {
                            linesToDelete.add(suggestionLine);
                        }
                    });
                suggestionLineRepository.deleteAll(linesToDelete);
                suggestion.setUpdatedAt(LocalDateTime.now());
                suggestionRepository.save(suggestion);
            });
    }

    @Override
    public void commander(Integer suggestionId) {
        Suggestion suggestion = suggestionRepository.findById(suggestionId).orElseThrow();
        commandService.createCommandeFromSuggestion(suggestion);
        suggestionRepository.delete(suggestion);
    }

    @Override
    public void commanderSelection(CommanderSelectionDTO dto) {
        Suggestion suggestion = suggestionRepository.findById(dto.suggestionId()).orElseThrow();
        commandService.createCommandeFromSelection(suggestion, dto.lignes());
        suggestionRepository.delete(suggestion);
    }

    @Override
    public void createCommandesFromSemois(List<SemoisCommanderDTO.LigneSemois> lignes) {
        if (lignes == null || lignes.isEmpty()) {
            LOG.warn("createCommandesFromSemois: aucune ligne fournie");
            return;
        }
        // Grouper les lignes par fournisseur → 1 commande par fournisseur
        Map<Integer, List<SemoisCommanderDTO.LigneSemois>> byFournisseur = lignes.stream()
            .collect(Collectors.groupingBy(SemoisCommanderDTO.LigneSemois::fournisseurId));

        byFournisseur.forEach(commandService::createCommandeFromSemoisLines
        );
        LOG.info("createCommandesFromSemois: {} commandes créées depuis {} lignes SEMOIS",
            byFournisseur.size(), lignes.size());
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetCommandeDTO getBudgetCommande() {
        long budgetMensuel = appConfigurationService
            .findOneById(com.kobe.warehouse.constant.EntityConstant.APP_BUDGET_MENSUEL_COMMANDE)
            .map(c -> {
                try { return Long.parseLong(c.getValue().trim()); } catch (NumberFormatException _) { return 0L; }
            })
            .orElse(0L);

        // Montant déjà commandé ce mois (commandes REQUESTED ou RECEIVED)
        Number montantCommandeRaw = (Number) em.createNativeQuery(
            """
            SELECT COALESCE(SUM(c.gross_amount), 0)
            FROM commande c
            WHERE c.order_status IN ('REQUESTED', 'RECEIVED')
              AND c.order_date >= DATE_TRUNC('month', CURRENT_DATE)
            """
        ).getSingleResult();
        long montantCommande = montantCommandeRaw.longValue();

        // Montant estimé des suggestions actives (lignes × prix_achat fournisseur)
        Number montantEstimeRaw = (Number) em.createNativeQuery(
            """
            SELECT COALESCE(SUM(sl.quantity * fp.prix_achat), 0)
            FROM suggestion_line sl
            JOIN fournisseur_produit fp ON fp.id = sl.fournisseur_produit_id
            JOIN suggestion s ON s.id = sl.suggestion_id
            WHERE s.statut = 'GENEREE'
            """
        ).getSingleResult();
        long montantEstime = montantEstimeRaw.longValue();

        boolean budgetIllimite = budgetMensuel == 0L;
        long budgetRestant = budgetIllimite ? Long.MAX_VALUE : budgetMensuel - montantCommande;
        boolean enDepassement = !budgetIllimite && (montantCommande + montantEstime) > budgetMensuel;

        return new BudgetCommandeDTO(budgetMensuel, montantEstime, montantCommande, budgetRestant, enDepassement, budgetIllimite);
    }

    @Override
    public void addSuggestionLine(Integer suggestionId, SuggestionLineDTO suggestionLine) {
        Suggestion suggestion = suggestionRepository.findById(suggestionId).orElseThrow();
        suggestionLineRepository
            .findBySuggestionIdAndFournisseurProduitProduitId(suggestionId, suggestionLine.produitId())
            .ifPresentOrElse(
                line -> {
                    line.setQuantity(line.getQuantity() + suggestionLine.quantity());
                    line.setUpdatedAt(line.getCreatedAt());
                    suggestionLineRepository.save(line);
                },
                () -> {
                    Fournisseur fournisseur = suggestion.getFournisseur();
                    FournisseurProduit fournisseurProduit = fournisseurProduitRepository
                        .findOneByProduitIdAndFournisseurId(suggestionLine.produitId(), fournisseur.getId())
                        .orElseThrow();
                    SuggestionLine line = new SuggestionLine();
                    line.setCreatedAt(LocalDateTime.now());
                    line.setUpdatedAt(line.getCreatedAt());
                    line.setQuantity(suggestionLine.quantity());
                    line.setFournisseurProduit(fournisseurProduit);
                    line.setSuggestion(suggestion);
                    suggestion.getSuggestionLines().add(line);
                    suggestionLineRepository.save(line);
                }
            );
        suggestion.setUpdatedAt(LocalDateTime.now());
        suggestionRepository.save(suggestion);
    }

    @Override
    public void updateSuggestionLinQuantity(SuggestionLineDTO suggestionLine) {
        SuggestionLine line = suggestionLineRepository.findById(suggestionLine.id()).orElseThrow();
        line.setUpdatedAt(LocalDateTime.now());
        line.setQuantity(suggestionLine.quantity());
        // S1.6 v12 — marquer comme modifiée manuellement : le batch ne touchera plus cette ligne
        line.setQuantiteModifieeManuel(true);
        suggestionLineRepository.save(line);
    }

    @Override
    public void resetQuantiteManuelle(Integer suggestionLineId) {
        suggestionLineRepository.findById(suggestionLineId).ifPresent(line -> {
            line.setQuantiteModifieeManuel(false);
            line.setUpdatedAt(LocalDateTime.now());
            suggestionLineRepository.save(line);
        });
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToCsv(Integer id) throws IOException {
        return exportToCsvBytes(this.suggestionRepository.findById(id).orElseThrow(()-> new GenericError("Suggestion non trouvée")));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToPdf(Integer id) {
        Suggestion suggestion = suggestionRepository.findById(id)
            .orElseThrow(() -> new GenericError("Suggestion non trouvée"));
        Integer storageId = storageService.getDefaultMagasinMainStorage().getId();
        LocalDate dateRetention = LocalDate.now().minusDays(appConfigurationService.getNombreJourRetentionCommande());
        int nthMois = appConfigurationService.getNthMoisConsommation();

        List<SuggestionLineDTO> lignes = suggestionLineRepository
            .fetchSuggestionLinesWithConsommation(
                suggestion.getId(), null, null, storageId, dateRetention, nthMois, Pageable.unpaged()
            )
            .getContent();

        return suggestionPdfReportService.export(
            suggestion.getSuggessionReference(),
            suggestion.getFournisseur().getLibelle(),
            lignes
        );
    }

    @Override
    public void validerSuggestion(Integer id) {
        suggestionRepository.findById(id).ifPresent(suggestion -> {
            suggestion.setStatut(com.kobe.warehouse.domain.enumeration.StatutSuggession.VALIDEE);
            suggestion.setValidePar(storageService.getUser());
            suggestion.setDateValidation(LocalDateTime.now());
            suggestion.setUpdatedAt(LocalDateTime.now());
            suggestionRepository.save(suggestion);
        });
    }

    @Override
    public void rejeterSuggestion(Integer id) {
        suggestionRepository.findById(id).ifPresent(suggestionRepository::delete);
    }

    @Override
    public int suggestionQuantiteProduitVendus(List<QauntiteProduitVendus> produitVendus, Boolean suggerQuantitySold) {
        if (CollectionUtils.isEmpty(produitVendus)) return 0;

        // 1. Filter eligible produits
        List<QauntiteProduitVendus> eligibles = produitVendus.stream()
            .filter(p -> p.produitId() != null && etatProduitService.canSuggere(p.produitId()))
            .toList();
        if (eligibles.isEmpty()) return 0;

        // 2. Batch-load Produit entities
        Set<Integer> produitIds = eligibles.stream().map(QauntiteProduitVendus::produitId).collect(Collectors.toSet());
        Map<Integer, Produit> produitById = produitRepository.findAllById(produitIds).stream()
            .filter(p -> p.getFournisseurProduitPrincipal() != null)
            .collect(Collectors.toMap(Produit::getId, Function.identity()));
        if (produitById.isEmpty()) return 0;

        // 3. Quantité à suggérer par produit
        Map<Integer, Integer> qtyByProduitId = eligibles.stream()
            .filter(q -> produitById.containsKey(q.produitId()))
            .collect(Collectors.toMap(
                QauntiteProduitVendus::produitId,
                q -> Boolean.TRUE.equals(suggerQuantitySold) ? q.quantitySold() : q.quantityReappro()
            ));

        // 4. Batch-load existing AUTO lines
        Set<Integer> allFpIds = produitById.values().stream()
            .map(p -> p.getFournisseurProduitPrincipal().getId())
            .collect(Collectors.toSet());
        Map<Integer, SuggestionLine> existingLineByFpId = suggestionLineRepository
            .findAllByTypeSuggessionAndFournisseurProduitIdIn(TypeSuggession.AUTO, allFpIds)
            .stream()
            .collect(Collectors.toMap(l -> l.getFournisseurProduit().getId(), Function.identity()));

        // 5. Group by fournisseur
        Map<Fournisseur, List<Produit>> byFournisseur = produitById.values().stream()
            .collect(Collectors.groupingBy(p -> p.getFournisseurProduitPrincipal().getFournisseur()));

        List<Suggestion> suggestionsToSave = new ArrayList<>();
        List<SuggestionLine> linesToSave = new ArrayList<>();
        int count = 0;

        for (Map.Entry<Fournisseur, List<Produit>> entry : byFournisseur.entrySet()) {
            Fournisseur fournisseur = entry.getKey();
            AtomicBoolean suggestionExist = new AtomicBoolean(false);
            Suggestion suggestion = getSuggestion(fournisseur, suggestionExist);
            boolean alreadyExisted = suggestionExist.get();

            for (Produit produit : entry.getValue()) {
                FournisseurProduit fp = produit.getFournisseurProduitPrincipal();
                int qty = qtyByProduitId.getOrDefault(produit.getId(), 0);
                if (qty <= 0) continue;

                SuggestionLine existingLine = existingLineByFpId.get(fp.getId());
                if (existingLine != null) {
                    existingLine.setQuantity(qty);
                    existingLine.setUpdatedAt(LocalDateTime.now());
                    linesToSave.add(existingLine);
                } else {
                    SuggestionLine newLine = new SuggestionLine();
                    newLine.setCreatedAt(LocalDateTime.now());
                    newLine.setUpdatedAt(newLine.getCreatedAt());
                    newLine.setQuantity(qty);
                    newLine.setFournisseurProduit(fp);
                    newLine.setSuggestion(suggestion);
                    suggestion.getSuggestionLines().add(newLine);
                    if (alreadyExisted) linesToSave.add(newLine);
                }
                count++;
            }

            if (!suggestion.getSuggestionLines().isEmpty() || !linesToSave.isEmpty()) {
                suggestionsToSave.add(suggestion);
            }
        }

        suggestionRepository.saveAll(suggestionsToSave);
        suggestionLineRepository.saveAll(linesToSave);
        return count;
    }

    private Suggestion getSuggestion(Fournisseur fournisseur, AtomicBoolean suggestionExist) {
        Magasin magasin = storageService.getConnectedUserMagasin();
        return getSuggestion(fournisseur, suggestionExist, magasin, storageService.getUser());
    }

    private Suggestion getSuggestion(Fournisseur fournisseur, AtomicBoolean suggestionExist, Magasin magasin, AppUser user) {
        Suggestion suggestion;
        Optional<Suggestion> suggestionOpt = suggestionRepository.findByTypeSuggessionAndFournisseurIdAndMagasinId(
            TypeSuggession.AUTO,
            fournisseur.getId(),
            magasin.getId()
        );
        if (suggestionOpt.isPresent()) {
            suggestionExist.set(true);
            suggestion = suggestionOpt.get();
        } else {
            suggestionExist.set(false);
            suggestion = new Suggestion()
                .setSuggessionReference(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).concat(this.referenceService.buildSuggestionReference())
                )
                .createdAt(LocalDateTime.now());
        }

        suggestion.setFournisseur(fournisseur);
        suggestion.setUpdatedAt(LocalDateTime.now());
        suggestion.setTypeSuggession(TypeSuggession.AUTO);
        suggestion.setMagasin(magasin);
        suggestion.setLastUserEdit(user);

        return suggestion;
    }

    /**
     * Résout le délai de livraison (jours) en cascade :
     * SemoisConfiguration (surcharge par produit) → Fournisseur → GroupeFournisseur → défaut 7 j
     */
    private int resolveDelaiLivraisonJours(SemoisConfiguration semoisConfig, Fournisseur fournisseur) {
        if (semoisConfig != null && semoisConfig.getDelaiLivraisonJours() != null) {
            return semoisConfig.getDelaiLivraisonJours();
        }
        if (fournisseur != null && fournisseur.getDelaiLivraisonJours() != null) {
            return fournisseur.getDelaiLivraisonJours();
        }
        if (fournisseur != null
            && fournisseur.getGroupeFournisseur() != null
            && fournisseur.getGroupeFournisseur().getDelaiLivraisonJours() != null) {
            return fournisseur.getGroupeFournisseur().getDelaiLivraisonJours();
        }
        return 7;
    }

    /**
     * Calcule le stock objectif SEMOIS à la volée.
     * Utilisé quand {@code stockObjectifCalcule} n'est pas encore disponible (cache miss).
     * <p>
     * Formule : VMM + (VMM × délai × coeff / 30)
     * </p>
     */
    private int computeSemoisStockObjectif(
        Produit produit,
        SemoisConfiguration semoisConfig,
        Fournisseur fournisseur,
        int vmm,
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigs
    ) {
        ClasseCriticite classe = produit.getEffectiveClasseCriticite();
        SemoisClasseConfig classeConfig = classeConfigs.get(classe);
        int delai = resolveDelaiLivraisonJours(semoisConfig, fournisseur);
        if (classeConfig == null) {
            // classe config manquante : formule P2 par défaut
            return (int) Math.round(vmm * (1 + delai / 30.0));
        }
        BigDecimal coeff = classeConfig.getCoefficientSecurite();
        int marge = BigDecimal.valueOf(vmm)
            .multiply(BigDecimal.valueOf(delai))
            .multiply(coeff)
            .divide(BigDecimal.valueOf(30), RoundingMode.HALF_UP)
            .intValue();
        return vmm + marge;
    }

    /**
     * Seuil de déclenchement du réapprovisionnement.
     * <ul>
     *   <li>Cache SEMOIS : {@code stockObjectifCalcule} pré-calculé (nightly batch)</li>
     *   <li>SEMOIS à la volée : VMM × classe criticité via {@link SemoisClasseConfig}</li>
     *   <li>P2 : {@code (couvertureMois + délai/30) × VMM}</li>
     *   <li>Classique : {@code qtySeuilMini}</li>
     * </ul>
     */
    private int seuilReappro(
        Produit produit,
        SemoisConfiguration semoisConfig,
        Fournisseur fournisseur,
        int vmm,
        int couvertureMois,
        boolean isSemois,
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigs
    ) {
        // Cache hit : nightly batch a déjà tout calculé
        if (semoisConfig != null && semoisConfig.getStockObjectifCalcule() != null) {
            return semoisConfig.getStockObjectifCalcule();
        }
        if (vmm > 0) {
            if (isSemois) {
                return computeSemoisStockObjectif(produit, semoisConfig, fournisseur, vmm, classeConfigs);
            }
            // P2
            int delaiJours = resolveDelaiLivraisonJours(semoisConfig, fournisseur);
            return (int) Math.round((couvertureMois + delaiJours / 30.0) * vmm);
        }
        return produit.getQtySeuilMini();
    }

    private int computeQtyReappro(
        Produit produit,
        int produitTotalStockQuantity,
        SemoisConfiguration semoisConfig,
        Fournisseur fournisseur,
        int vmm,
        int couvertureMois,
        boolean isSemois,
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigs
    ) {
        // Cache hit
        if (semoisConfig != null && semoisConfig.getStockObjectifCalcule() != null) {
            return Math.max(1, semoisConfig.getStockObjectifCalcule() - produitTotalStockQuantity);
        }
        if (vmm > 0) {
            int stockObjectif = isSemois
                ? computeSemoisStockObjectif(produit, semoisConfig, fournisseur, vmm, classeConfigs)
                : (int) Math.round((couvertureMois + resolveDelaiLivraisonJours(semoisConfig, fournisseur) / 30.0) * vmm);
            return Math.max(1, stockObjectif - produitTotalStockQuantity);
        }
        // CLASSIQUE : fallback sur les paramètres statiques du produit
        int qtyReappro = Objects.requireNonNullElse(produit.getQtyAppro(), 1);
        return (produit.getQtySeuilMini() - produitTotalStockQuantity) + qtyReappro;
    }

    /**
     * Charge le VMM en une requête SQL par groupe de classe SEMOIS.
     * Chaque classe a son propre {@code nbMoisHistorique} — au plus 5 requêtes SQL.
     */
    private Map<Integer, Integer> loadVmmForProduitsBySemoisClass(
        List<QuantitySuggestion> eligibles,
        Map<ClasseCriticite, SemoisClasseConfig> classeConfigs
    ) {
        Map<ClasseCriticite, List<Integer>> idsByClass = eligibles.stream()
            .collect(Collectors.groupingBy(
                q -> q.produit().getEffectiveClasseCriticite(),
                Collectors.mapping(q -> q.produit().getId(), Collectors.toList())
            ));

        Map<Integer, Integer> result = new HashMap<>();
        for (Map.Entry<ClasseCriticite, List<Integer>> entry : idsByClass.entrySet()) {
            SemoisClasseConfig cc = classeConfigs.get(entry.getKey());
            int nbMois = cc != null ? cc.getNbMoisHistorique() : 6;
            result.putAll(loadVmmForProduits(new HashSet<>(entry.getValue()), nbMois));
        }
        return result;
    }

    /**
     * Axe 1 — Charge en batch les quantités commandées en attente de livraison.
     * <p>
     * Une commande est "en attente" si son statut est {@code REQUESTED} (envoyée au fournisseur,
     * non encore réceptionnée). La quantité en attente = quantité commandée - quantité déjà reçue.
     * </p>
     *
     * @param produitIds IDs des produits à interroger
     * @return Map produit_id → quantité en attente (seulement les produits avec qté > 0)
     */
    private Map<Integer, Integer> loadPendingOrderQty(Set<Integer> produitIds) {
        if (produitIds.isEmpty()) return Map.of();
        List<Object[]> rows = orderLineRepository.findPendingQtyByProduitIds(produitIds);
        return rows.stream().collect(Collectors.toMap(
            row -> ((Number) row[0]).intValue(),
            row -> ((Number) row[1]).intValue()
        ));
    }

    @SuppressWarnings("unchecked")
    private Map<Integer, Integer> loadVmmForProduits(Set<Integer> produitIds, int nthMois) {
        if (produitIds.isEmpty()) return Map.of();
        String sql = """
            SELECT produit_id, ROUND(AVG(qte_vendue))::integer AS vmm
            FROM mv_monthly_top_products
            WHERE produit_id IN (:produitIds)
              AND mois::date >= (DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '%d months')::date
              AND mois::date < DATE_TRUNC('month', CURRENT_DATE)::date
            GROUP BY produit_id
            """.formatted(nthMois);
        List<Object[]> rows = em.createNativeQuery(sql)
            .setParameter("produitIds", produitIds)
            .getResultList();
        return rows.stream().collect(Collectors.toMap(
            row -> ((Number) row[0]).intValue(),
            row -> ((Number) row[1]).intValue()
        ));
    }

    private byte[] exportToCsvBytes(Suggestion suggestion) throws IOException {
        Integer storageId = storageService.getDefaultMagasinMainStorage().getId();
        LocalDate dateRetention = LocalDate.now().minusDays(appConfigurationService.getNombreJourRetentionCommande());
        int nthMois = appConfigurationService.getNthMoisConsommation();

        List<SuggestionLineDTO> lines = suggestionLineRepository
            .fetchSuggestionLinesWithConsommation(
                suggestion.getId(), null, null, storageId, dateRetention, nthMois, Pageable.unpaged()
            )
            .getContent();

        // Colonnes de mois présentes sur la première ligne ayant des données de conso
        List<Mois> moisColonnes = lines.stream()
            .map(SuggestionLineDTO::consommationMensuelle)
            .filter(Objects::nonNull)
            .findFirst()
            .map(m -> m.keySet().stream().toList())
            .orElse(List.of());

        // Headers : colonnes fixes + une colonne par mois
        List<String> headerList = new ArrayList<>(Arrays.asList(
            "Code CIP", "Code EAN", "Désignation", "Stock", "Qté suggérée", "Prix achat", "Prix vente"
        ));
        moisColonnes.forEach(m -> headerList.add("Conso. " + m.getLibelle()));
        String[] headers = headerList.toArray(new String[0]);

        // Titre
        String title = "Suggestion " + suggestion.getSuggessionReference()
            + " - " + suggestion.getFournisseur().getLibelle()
            + " - " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        // Lignes de données
        List<String[]> rows = lines.stream()
            .map(line -> {
                List<String> row = new ArrayList<>(Arrays.asList(
                    Objects.toString(line.fournisseurProduitCip(), ""),
                    Objects.toString(line.fournisseurProduitCodeEan(), ""),
                    Objects.toString(line.fournisseurProduitLibelle(), ""),
                    String.valueOf(line.currentStock()),
                    String.valueOf(line.quantity()),
                    String.valueOf(line.prixAchat()),
                    String.valueOf(line.prixVente())
                ));
                Map<Mois, Integer> conso = line.consommationMensuelle();
                moisColonnes.forEach(m ->
                    row.add(conso != null ? String.valueOf(conso.getOrDefault(m, 0)) : "0")
                );
                return row.toArray(new String[0]);
            })
            .toList();

        return csvExportService.addUtf8Bom(
            csvExportService.createSimpleCsvReport(title, headers, rows)
        );
    }
}

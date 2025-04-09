package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.ProductStateEnum;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.*;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.projection.SuggestionProjection;
import com.kobe.warehouse.service.dto.records.QuantitySuggestion;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class SuggestionProduitServiceImpl implements SuggestionProduitService {

    private static final Logger LOG = LoggerFactory.getLogger(SuggestionProduitServiceImpl.class);
    private final ProductStateService productStateService;
    private final SuggestionRepository suggestionRepository;
    private final SuggestionLineRepository suggestionLineRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final StorageService storageService;
    private final ReferenceService referenceService;
    private final AppConfigurationService appConfigurationService;
    private final EtatProduitService etatProduitService;

    public SuggestionProduitServiceImpl(
        ProductStateService productStateService,
        SuggestionRepository suggestionRepository,
        SuggestionLineRepository suggestionLineRepository,
        FournisseurProduitRepository fournisseurProduitRepository,
        StorageService storageService,
        ReferenceService referenceService,
        AppConfigurationService appConfigurationService,
        EtatProduitService etatProduitService
    ) {
        this.productStateService = productStateService;
        this.suggestionRepository = suggestionRepository;
        this.suggestionLineRepository = suggestionLineRepository;
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.storageService = storageService;
        this.referenceService = referenceService;
        this.appConfigurationService = appConfigurationService;
        this.etatProduitService = etatProduitService;
    }

    // @Async
    @Override
    public void suggerer(List<QuantitySuggestion> quantitySuggestions) {
        if (!CollectionUtils.isEmpty(quantitySuggestions)) {
            quantitySuggestions
                .stream()
                .collect(Collectors.groupingBy(e -> e.produit().getFournisseurProduitPrincipal().getFournisseur()))
                .forEach((four, values) -> {
                    Suggestion suggestion = getSuggestion(four);
                    values.forEach(quantitySuggestion -> {
                        StockProduit stockProduit = quantitySuggestion.stockProduit();
                        Produit produit = quantitySuggestion.produit();
                        Set<ProductStateEnum> productStates = productStateService.getProductStateByProduitId(produit.getId());
                        if (
                            !productStates.containsAll(
                                Set.of(ProductStateEnum.COMMANDE_EN_COURS, ProductStateEnum.ENTREE, ProductStateEnum.COMMANDE_PASSE)
                            )
                        ) {
                            FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();

                            int quantitySold = quantitySuggestion.quantitySold();
                            if (produit.getTypeProduit() == TypeProduit.DETAIL) {
                                produit = produit.getParent();
                                quantitySold = Math.ceilDiv(quantitySold, produit.getItemQty());
                            }
                            int currentStock = stockProduit.getTotalStockQuantity() - quantitySold;
                            if (currentStock <= produit.getQtySeuilMini()) {
                                if (!productStateService.existsByStateAndProduitId(ProductStateEnum.SUGGESTION, produit.getId())) {
                                    productStateService.addState(produit, ProductStateEnum.SUGGESTION);
                                }
                                saveSuggestionLine(produit, stockProduit, fournisseurProduit, suggestion);
                            }
                        }
                    });
                    if (!suggestion.getSuggestionLines().isEmpty()) {
                        this.suggestionRepository.save(suggestion);
                    }
                });
        }
    }

    @Override
    public int suggererListProduits(List<Produit> produits) {
        return 0;
    }

    @Override
    public void suggerer(Produit produit) {}

    @Override
    @Transactional(readOnly = true)
    public Page<SuggestionProjection> getAllSuggestion(
        String search,
        Long fournisseurId,
        TypeSuggession typeSuggession,
        Pageable pageable
    ) {
        Specification<Suggestion> specification = Specification.where(
            suggestionRepository.filterByDate(appConfigurationService.findSuggestionRetention())
        );
        if (typeSuggession != null) {
            specification = specification.and(suggestionRepository.filterByType(typeSuggession));
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
    public Optional<SuggestionDTO> getSuggestionById(long id) {
        return suggestionRepository.findById(id).map(SuggestionDTO::new);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SuggestionLineDTO> getSuggestionLinesById(long suggestionId, String search, Pageable pageable) {
        Storage storage = storageService.getDefaultMagasinMainStorage();
        Specification<SuggestionLine> specification = Specification.where(suggestionLineRepository.filterBySuggestionId(suggestionId));
        if (StringUtils.hasLength(search)) {
            specification = specification.and(suggestionLineRepository.filterByProduit(search));
        }
        return suggestionLineRepository
            .findAll(specification, pageable)
            .map(e -> {
                FournisseurProduit fournisseurProduit = e.getFournisseurProduit();
                Produit produit = fournisseurProduit.getProduit();

                StockProduit stockProduit = produit
                    .getStockProduits()
                    .stream()
                    .filter(stock -> stock.getStorage().getId() == storage.getId())
                    .findFirst()
                    .orElse(new StockProduit());
                int currentstock = 0;
                if (stockProduit != null) {
                    currentstock = stockProduit.getTotalStockQuantity();
                }

                return new SuggestionLineDTO(
                    e.getId(),
                    e.getQuantity(),
                    e.getCreatedAt(),
                    e.getUpdatedAt(),
                    produit.getLibelle(),
                    fournisseurProduit.getCodeCip(),
                    produit.getCodeEan(),
                    produit.getId(),
                    fournisseurProduit.getId(),
                    currentstock,
                    this.etatProduitService.getEtatProduit(produit.getId(), currentstock)
                );
            });
    }

    @Override
    public void fusionnerSuggestion(Set<Long> ids) throws GenericError {
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
    public void deleteSuggestion(Set<Long> ids) {
        suggestionRepository.deleteAllById(ids);
    }

    @Override
    public void deleteSuggestionLine(Set<Long> ids) {
        suggestionLineRepository.deleteAllById(ids);
    }

    @Override
    public void sanitize(long suggestionId) {
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

    private Suggestion getSuggestion(Fournisseur fournisseur) {
        Magasin magasin = storageService.getConnectedUserMagasin();
        Suggestion suggestion = suggestionRepository
            .findByTypeSuggessionAndFournisseurIdAndMagasinId(TypeSuggession.AUTO, fournisseur.getId(), magasin.getId())
            .orElse(
                new Suggestion().setSuggessionReference(this.referenceService.buildSuggestionReference()).createdAt(LocalDateTime.now())
            );
        suggestion.setFournisseur(fournisseur);
        suggestion.setUpdatedAt(LocalDateTime.now());
        suggestion.setTypeSuggession(TypeSuggession.AUTO);
        suggestion.setMagasin(magasin);
        suggestion.setLastUserEdit(storageService.getUser());

        return suggestion;
    }

    private Optional<FournisseurProduit> getFournisseurProduit(Produit produit) {
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        if (Objects.isNull(fournisseurProduit)) {
            List<FournisseurProduit> fournisseurProduits = fournisseurProduitRepository.findAllByProduitId(produit.getId());
            if (!CollectionUtils.isEmpty(fournisseurProduits)) {
                fournisseurProduit = fournisseurProduits.getFirst();
            }
            LOG.info("********** Ce produit n'a pas de fournisseur principal {} *****************", produit.getLibelle());
        }
        return Optional.ofNullable(fournisseurProduit);
    }

    private void saveSuggestionLine(
        Produit produit,
        StockProduit stockProduit,
        FournisseurProduit fournisseurProduit,
        Suggestion suggestion
    ) {
        this.suggestionLineRepository.findBySuggestionTypeSuggessionAndFournisseurProduitId(
                TypeSuggession.AUTO,
                fournisseurProduit.getId()
            ).ifPresentOrElse(
                line -> updateLine(produit, stockProduit, line),
                () -> buildLine(produit, stockProduit, fournisseurProduit, suggestion)
            );
    }

    private void buildLine(Produit produit, StockProduit stockProduit, FournisseurProduit fournisseurProduit, Suggestion suggestion) {
        SuggestionLine line = new SuggestionLine();
        line.setCreatedAt(LocalDateTime.now());
        line.setUpdatedAt(line.getCreatedAt());
        line.setQuantity(computeQtyReappro(produit, stockProduit));
        line.setFournisseurProduit(fournisseurProduit);
        line.setSuggestion(suggestion);
        suggestion.getSuggestionLines().add(line);
    }

    private void updateLine(Produit produit, StockProduit stockProduit, SuggestionLine line) {
        line.setUpdatedAt(LocalDateTime.now());
        line.setQuantity(computeQtyReappro(produit, stockProduit));
        this.suggestionLineRepository.save(line);
    }

    private int computeQtyReappro(Produit produit, StockProduit stockProduit) {
        int qtyReappro = Objects.requireNonNullElse(produit.getQtyAppro(), 1);
        return (produit.getQtySeuilMini() - stockProduit.getTotalStockQuantity()) + qtyReappro;
    }
}

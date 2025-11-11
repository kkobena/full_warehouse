package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.EtatProduitService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import com.kobe.warehouse.service.dto.records.QuantitySuggestion;
import com.kobe.warehouse.service.errors.FileStorageException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

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
    private final Path fileStorageLocation;

    public SuggestionProduitServiceImpl(
        SuggestionRepository suggestionRepository,
        SuggestionLineRepository suggestionLineRepository,
        FournisseurProduitRepository fournisseurProduitRepository,
        StorageService storageService,
        ReferenceService referenceService,
        AppConfigurationService appConfigurationService,
        EtatProduitService etatProduitService,
        CommandService commandService,
        FileStorageProperties fileStorageProperties
    ) {
        this.suggestionRepository = suggestionRepository;
        this.suggestionLineRepository = suggestionLineRepository;
        this.fournisseurProduitRepository = fournisseurProduitRepository;
        this.storageService = storageService;
        this.referenceService = referenceService;
        this.appConfigurationService = appConfigurationService;
        this.etatProduitService = etatProduitService;
        this.commandService = commandService;
        this.fileStorageLocation = Paths.get(fileStorageProperties.getReportsDir()).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    // @Async
    @Override
    public void suggerer(List<QuantitySuggestion> quantitySuggestions) {
        if (!CollectionUtils.isEmpty(quantitySuggestions)) {
            AtomicBoolean suggestionExist = new AtomicBoolean(false);
            quantitySuggestions
                .stream()
                .collect(Collectors.groupingBy(e -> e.produit().getFournisseurProduitPrincipal().getFournisseur()))
                .forEach((four, values) -> {
                    Suggestion suggestion = getSuggestion(four, suggestionExist);
                    values.forEach(quantitySuggestion -> {
                        StockProduit stockProduit = quantitySuggestion.stockProduit();
                        Produit produit = quantitySuggestion.produit();

                        if (etatProduitService.canSuggere(produit.getId())) {
                            FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();

                            int quantitySold = quantitySuggestion.quantitySold();
                            if (produit.getTypeProduit() == TypeProduit.DETAIL) {
                                produit = produit.getParent();
                                quantitySold = Math.ceilDiv(quantitySold, produit.getItemQty());
                            }
                            int currentStock = stockProduit.getTotalStockQuantity() - quantitySold;
                            if (currentStock <= produit.getQtySeuilMini()) {
                                saveSuggestionLine(produit, stockProduit, fournisseurProduit, suggestion, suggestionExist.get());
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
    public void suggerer(Produit produit) {
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SuggestionProjection> getAllSuggestion(
        String search,
        Integer fournisseurId,
        TypeSuggession typeSuggession,
        Pageable pageable
    ) {
        Specification<Suggestion> specification = suggestionRepository.filterByDate(appConfigurationService.findSuggestionRetention());
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
    public Optional<SuggestionDTO> getSuggestionById(Integer id) {
        return suggestionRepository
            .findById(id)
            .map(suggestion ->
                new SuggestionDTO(suggestion).setSuggestionAggregator(suggestionLineRepository.getSuggestionData(suggestion.getId()))
            );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SuggestionLineDTO> getSuggestionLinesById(Integer suggestionId, String search, Pageable pageable) {
        Storage storage = storageService.getDefaultMagasinMainStorage();
        Specification<SuggestionLine> specification = suggestionLineRepository.filterBySuggestionId(suggestionId);
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
                    .filter(stock -> Objects.equals(stock.getStorage().getId(), storage.getId()))
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
                    fournisseurProduit.getCodeEan(),
                    produit.getId(),
                    fournisseurProduit.getId(),
                    currentstock,
                    this.etatProduitService.getEtatProduit(produit.getId(), currentstock),
                    fournisseurProduit.getPrixAchat(),
                    fournisseurProduit.getPrixUni()
                );
            });
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
        commandService.createCommandeFromSuggestion(suggestionRepository.findById(suggestionId).orElseThrow());
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
        suggestionLineRepository.save(line);
    }

    @Override
    public Resource exportToCsv(Integer id) throws IOException {
        return new UrlResource(Paths.get(exportToCsv(this.suggestionRepository.getReferenceById(id))).toUri());
    }

    private Suggestion getSuggestion(Fournisseur fournisseur, AtomicBoolean suggestionExist) {
        Magasin magasin = storageService.getConnectedUserMagasin();
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
                .setSuggessionReference(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).concat(this.referenceService.buildSuggestionReference()))
                .createdAt(LocalDateTime.now());
        }

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
        Suggestion suggestion,
        boolean suggestionExist
    ) {
        this.suggestionLineRepository.findBySuggestionTypeSuggessionAndFournisseurProduitId(
            TypeSuggession.AUTO,
            fournisseurProduit.getId()
        ).ifPresentOrElse(
            line -> updateLine(produit, stockProduit, line),
            () -> buildLine(produit, stockProduit, fournisseurProduit, suggestion, suggestionExist)
        );
    }

    private void buildLine(
        Produit produit,
        StockProduit stockProduit,
        FournisseurProduit fournisseurProduit,
        Suggestion suggestion,
        boolean suggestionExist
    ) {
        SuggestionLine line = new SuggestionLine();
        line.setCreatedAt(LocalDateTime.now());
        line.setQuantity(computeQtyReappro(produit, stockProduit));
        line.setFournisseurProduit(fournisseurProduit);
        line.setSuggestion(suggestion);
        if (suggestionExist) {
            this.suggestionLineRepository.save(line);
        }

        suggestion.getSuggestionLines().add(line);
    }

    private void updateLine(Produit produit, StockProduit stockProduit, SuggestionLine line) {
        line.setQuantity(computeQtyReappro(produit, stockProduit));
        this.suggestionLineRepository.save(line);
    }

    private int computeQtyReappro(Produit produit, StockProduit stockProduit) {
        int qtyReappro = Objects.requireNonNullElse(produit.getQtyAppro(), 1);
        return (produit.getQtySeuilMini() - stockProduit.getTotalStockQuantity()) + qtyReappro;
    }

    private String exportToCsv(Suggestion suggestion) {
        String filename =
            this.fileStorageLocation.resolve(
                    "suggestion_" +
                        suggestion.getSuggessionReference() +
                        "_" +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd_MM_yyyy_H_mm_ss")) +
                        ".csv"
                )
                .toFile()
                .getAbsolutePath();
        try (final FileWriter writer = new FileWriter(filename); final CSVPrinter printer = new CSVPrinter(writer, CSVFormat.EXCEL)) {
            suggestion
                .getSuggestionLines()
                .forEach(item -> {
                    FournisseurProduit fournisseurProduit = item.getFournisseurProduit();
                    Produit produit = fournisseurProduit.getProduit();
                    try {
                        printer.printRecord(
                            org.apache.commons.lang3.StringUtils.isNotEmpty(produit.getCodeEanLaboratoire())
                                ? produit.getCodeEanLaboratoire()
                                : fournisseurProduit.getCodeCip(),
                            item.getQuantity()
                        );
                    } catch (IOException e) {
                        LOG.error("Error writing data to the csv printer", e);
                    }
                });

            printer.flush();
        } catch (final IOException e) {
            throw new RuntimeException("Csv writing error: " + e.getMessage());
        }
        return filename;
    }
}

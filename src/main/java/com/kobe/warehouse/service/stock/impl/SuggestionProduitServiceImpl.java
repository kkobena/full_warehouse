package com.kobe.warehouse.service.stock.impl;

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
import com.kobe.warehouse.service.dto.enumeration.Mois;
import com.kobe.warehouse.service.dto.records.QuantitySuggestion;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import com.kobe.warehouse.service.stock.dto.QauntiteProduitVendus;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final SuggestionRepository suggestionRepository;
    private final SuggestionLineRepository suggestionLineRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final StorageService storageService;
    private final ReferenceService referenceService;
    private final AppConfigurationService appConfigurationService;
    private final EtatProduitService etatProduitService;
    private final CommandService commandService;
    private final CsvExportService csvExportService;


    public SuggestionProduitServiceImpl(
        SuggestionRepository suggestionRepository,
        SuggestionLineRepository suggestionLineRepository,
        FournisseurProduitRepository fournisseurProduitRepository,
        StorageService storageService,
        ReferenceService referenceService,
        AppConfigurationService appConfigurationService,
        EtatProduitService etatProduitService,
        CommandService commandService,
        CsvExportService csvExportService

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

    }

    // @Async
    @Override
    public void suggerer(List<QuantitySuggestion> quantitySuggestions) {
        if (!CollectionUtils.isEmpty(quantitySuggestions)) {

            Magasin magasin = storageService.getConnectedUserMagasin();
            AtomicBoolean suggestionExist = new AtomicBoolean(false);
            quantitySuggestions
                .stream()
                .collect(Collectors.groupingBy(e -> e.produit().getFournisseurProduitPrincipal().getFournisseur()))
                .forEach((four, values) -> {
                    Suggestion suggestion = getSuggestion(four, suggestionExist);
                    values.forEach(quantitySuggestion -> {
                        // StockProduit stockProduit = quantitySuggestion.stockProduit();
                        Produit produit = quantitySuggestion.produit();
                        if (etatProduitService.canSuggere(produit.getId())) {
                            FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
                            boolean isDetail = false;
                            int quantitySold = quantitySuggestion.quantitySold();
                            if (produit.getTypeProduit() == TypeProduit.DETAIL) {
                                isDetail = true;
                                produit = produit.getParent();
                                quantitySold = Math.ceilDiv(quantitySold, produit.getItemQty());
                            }
                            int produitAllSock = produit.getStockProduits().stream().filter(stock -> stock.getStorage().getMagasin().equals(magasin)).mapToInt(StockProduit::getTotalStockQuantity).sum();
                            LOG.info("********** produitAllSock {} *****************", produitAllSock);
                            if (isDetail) {
                                produitAllSock = produitAllSock - quantitySold;
                                LOG.info("********** produitAllSock after detail {} *****************", produitAllSock);
                            }

                            //   int currentStock = produitAllSock - quantitySold; on aura deja mis a jour le stock avant de lancer la suggestion
                            if (produitAllSock <= produit.getQtySeuilMini()) {
                                saveSuggestionLine(produit, produitAllSock, fournisseurProduit, suggestion, suggestionExist.get());
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
    public Page<SuggestionLineDTO> getSuggestionLinesByIdWithConsommation(Integer suggestionId, String search, Pageable pageable) {
        Integer storageId = storageService.getDefaultMagasinMainStorage().getId();
        LocalDate dateRetention = LocalDate.now().minusDays(appConfigurationService.getNombreJourRetentionCommande());
        int nthMois = appConfigurationService.getNthMoisConsommation();
        return suggestionLineRepository.fetchSuggestionLinesWithConsommation(suggestionId, search, storageId, dateRetention, nthMois, pageable);
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
    @Transactional(readOnly = true)
    public byte[] exportToCsv(Integer id) throws IOException {
        return exportToCsvBytes(this.suggestionRepository.findById(id).orElseThrow(()-> new GenericError("Suggestion non trouvée")));
    }

    @Override
    public int suggestionQuantiteProduitVendus(List<QauntiteProduitVendus> produitVendus, Boolean suggerQuantitySold) {
        Magasin magasin = storageService.getConnectedUserMagasin();
        return 0;
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
                .setSuggessionReference(
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")).concat(this.referenceService.buildSuggestionReference())
                )
                .createdAt(LocalDateTime.now());
        }

        suggestion.setFournisseur(fournisseur);
        suggestion.setUpdatedAt(LocalDateTime.now());
        suggestion.setTypeSuggession(TypeSuggession.AUTO);
        suggestion.setMagasin(magasin);
        suggestion.setLastUserEdit(storageService.getUser());

        return suggestion;
    }

    private void saveSuggestionLine(
        Produit produit,
        int produitTotalStockQuantity,
        FournisseurProduit fournisseurProduit,
        Suggestion suggestion,
        boolean suggestionExist
    ) {
        this.suggestionLineRepository.findBySuggestionTypeSuggessionAndFournisseurProduitId(
            TypeSuggession.AUTO,
            fournisseurProduit.getId()
        ).ifPresentOrElse(
            line -> updateLine(produit, produitTotalStockQuantity, line),
            () -> buildLine(produit, produitTotalStockQuantity, fournisseurProduit, suggestion, suggestionExist)
        );
    }

    private void buildLine(
        Produit produit,
        int produitTotalStockQuantity,
        FournisseurProduit fournisseurProduit,
        Suggestion suggestion,
        boolean suggestionExist
    ) {
        SuggestionLine line = new SuggestionLine();
        line.setCreatedAt(LocalDateTime.now());
        line.setQuantity(computeQtyReappro(produit, produitTotalStockQuantity));
        line.setFournisseurProduit(fournisseurProduit);
        line.setSuggestion(suggestion);
        if (suggestionExist) {
            this.suggestionLineRepository.save(line);
        }
        suggestion.getSuggestionLines().add(line);
    }

    private void updateLine(Produit produit, int produitTotalStockQuantity, SuggestionLine line) {
        line.setQuantity(computeQtyReappro(produit, produitTotalStockQuantity));
        this.suggestionLineRepository.save(line);
    }

    private int computeQtyReappro(Produit produit, int produitTotalStockQuantity) {
        int qtyReappro = Objects.requireNonNullElse(produit.getQtyAppro(), 1);
        return (produit.getQtySeuilMini() - produitTotalStockQuantity) + qtyReappro;
    }

    private byte[] exportToCsvBytes(Suggestion suggestion) throws IOException {
        Integer storageId = storageService.getDefaultMagasinMainStorage().getId();
        LocalDate dateRetention = LocalDate.now().minusDays(appConfigurationService.getNombreJourRetentionCommande());
        int nthMois = appConfigurationService.getNthMoisConsommation();

        List<SuggestionLineDTO> lines = suggestionLineRepository
            .fetchSuggestionLinesWithConsommation(
                suggestion.getId(), null, storageId, dateRetention, nthMois, Pageable.unpaged()
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

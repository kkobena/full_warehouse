package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Suggestion;
import com.kobe.warehouse.domain.SuggestionLine;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SuggestionLineRepository;
import com.kobe.warehouse.repository.SuggestionRepository;
import com.kobe.warehouse.service.ColisageService;
import com.kobe.warehouse.service.EtatProduitService;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.BudgetCommandeDTO;
import com.kobe.warehouse.service.dto.CommanderSelectionDTO;
import com.kobe.warehouse.service.dto.FournisseurSuggestionSummaryDTO;
import com.kobe.warehouse.service.dto.SuggestionDTO;
import com.kobe.warehouse.service.dto.SuggestionLineDTO;
import com.kobe.warehouse.service.dto.SuggestionProjection;
import com.kobe.warehouse.service.dto.enumeration.Mois;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.report.excel.CsvExportService;
import com.kobe.warehouse.service.report.pdf.SuggestionPdfReportService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.CommandService;
import com.kobe.warehouse.service.stock.SuggestionProduitService;
import com.kobe.warehouse.service.stock.dto.QauntiteProduitVendus;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class SuggestionProduitServiceImpl implements SuggestionProduitService {

    private final SuggestionRepository suggestionRepository;
    private final SuggestionLineRepository suggestionLineRepository;
    private final FournisseurProduitRepository fournisseurProduitRepository;
    private final StorageService storageService;
    private final ReferenceService referenceService;
    private final AppConfigurationService appConfigurationService;
    private final EtatProduitService etatProduitService;
    private final CommandService commandService;
    private final CsvExportService csvExportService;
    private final EntityManager em;
    private final SuggestionPdfReportService suggestionPdfReportService;
    private final ProduitRepository produitRepository;
    /**
     * Axe 1 — Stock virtuel : commandes REQUESTED en attente de livraison.
     */
    private final ColisageService colisageService;


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
        EntityManager em,
        SuggestionPdfReportService suggestionPdfReportService,
        ProduitRepository produitRepository,
        ColisageService colisageService
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
        this.em = em;
        this.suggestionPdfReportService = suggestionPdfReportService;
        this.produitRepository = produitRepository;
        this.colisageService = colisageService;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<SuggestionProjection> getAllSuggestion(
        String search,
        Set<Integer> fournisseurIds,
        TypeSuggession typeSuggession,
        Set<StatutSuggession> statut,
        Pageable pageable
    ) {
        Specification<Suggestion> specification = suggestionRepository.filterByDate(
            appConfigurationService.findSuggestionRetention());
        if (typeSuggession != null) {
            specification = specification.and(suggestionRepository.filterByType(typeSuggession));
        }
        if (!CollectionUtils.isEmpty(statut)) {
            specification = specification.and(
                suggestionRepository.filterByStatut(EnumSet.copyOf(statut)));
        }
        if (!CollectionUtils.isEmpty(fournisseurIds)) {
            specification = specification.and(
                suggestionRepository.filterByFournisseurIds(fournisseurIds));
        }

        return suggestionRepository.getAllSuggestion(specification, pageable, search);
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
                new SuggestionDTO(suggestion).setSuggestionAggregator(
                    suggestionLineRepository.getSuggestionData(suggestion.getId()))
            );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FournisseurSuggestionSummaryDTO> getSuggestionsParFournisseur(
        Set<StatutSuggession> statut, Set<Integer> fournisseurIds, String searchTerm) {
        return suggestionRepository.getSuggestionsParFournisseur(statut, fournisseurIds,
            searchTerm);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SuggestionLineDTO> getSuggestionLinesByIdWithConsommation(Integer suggestionId,
        String search, String niveauUrgence, Pageable pageable) {
        Integer storageId = storageService.getDefaultMagasinMainStorage().getId();
        LocalDate dateRetention = LocalDate.now()
            .minusDays(appConfigurationService.getNombreJourRetentionCommande());
        int nthMois = appConfigurationService.getNthMoisConsommation();
        return suggestionLineRepository.fetchSuggestionLinesWithConsommation(suggestionId, search,
            niveauUrgence, storageId, dateRetention, nthMois, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuggestionLineDTO> getAllSuggestionLines(Integer suggestionId, String search,
        String niveauUrgence) {
        return getSuggestionLinesByIdWithConsommation(suggestionId, search, niveauUrgence,
            Pageable.unpaged()).getContent();
    }

    @Override
    public void fusionnerSuggestion(Set<Integer> ids) throws GenericError {
        List<Suggestion> suggestions = suggestionRepository.findAllById(ids);
        if (!CollectionUtils.isEmpty(suggestions)) {
            suggestions.sort(
                Comparator.comparing(Suggestion::getUpdatedAt, Comparator.reverseOrder()));
            Suggestion suggestion = suggestions.getFirst();
            Set<SuggestionLine> suggestionLines = suggestion.getSuggestionLines();
            Fournisseur fournisseur = suggestion.getFournisseur();
            for (int i = 1; i < suggestions.size(); i++) {
                Suggestion suggestionToMerge = suggestions.get(i);
                if (!Objects.equals(fournisseur, suggestionToMerge.getFournisseur())) {
                    throw new GenericError(
                        "Vous ne pouvez pas fusionner des suggestions de fournisseurs differents");
                }
                Set<SuggestionLine> lignesAFusionner = new HashSet<>(suggestionToMerge.getSuggestionLines());
                lignesAFusionner.forEach(suggestionLine -> {
                    if (suggestionLines.contains(suggestionLine)) {
                        suggestionLineRepository.delete(suggestionLine);
                    } else {
                        suggestionLine.setSuggestion(suggestion);
                        suggestionLine.setUpdatedAt(LocalDateTime.now());
                        suggestionLineRepository.save(suggestionLine);
                        suggestionLines.add(suggestionLine);
                    }
                });
                // La collection de la suggestion absorbee doit etre videe avant sa suppression :
                // elle cascade REMOVE, et supprimerait les lignes qu on vient de rattacher ailleurs.
                suggestionToMerge.getSuggestionLines().clear();
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
                        if ((produit.getStatus() != Status.ENABLE) || (produit.getQtySeuilMini()
                            < currentStock)) {
                            linesToDelete.add(suggestionLine);
                        }
                    });
                // Retirer les lignes de la collection avant de sauvegarder la suggestion : celle-ci
                // cascade PERSIST, et reinsererait les lignes qu on vient de supprimer.
                suggestion.getSuggestionLines().removeAll(linesToDelete);
                suggestionLineRepository.deleteAll(linesToDelete);
                suggestion.setUpdatedAt(LocalDateTime.now());
                suggestionRepository.save(suggestion);
            });
    }

    @Override
    public CommandeId commander(Integer suggestionId, Integer fournisseurId) {
        Suggestion suggestion = suggestionRepository.findById(suggestionId).orElseThrow();
        CommandeId commandeId = commandService.createCommandeFromSuggestion(suggestion,
            fournisseurId);
        suggestionRepository.delete(suggestion);
        return commandeId;
    }

    @Override
    public CommandeId commanderSelection(CommanderSelectionDTO dto) {
        Suggestion suggestion = suggestionRepository.getReferenceById(dto.suggestionId());
        CommandeId commandeId = commandService.createCommandeFromSelection(suggestion, dto.lignes(),
            dto.fournisseurId());
        // Si toutes les lignes ont été commandées, supprimer la suggestion elle-même
        long restantes = suggestion.getSuggestionLines().size() - dto.lignes().size();
        if (restantes <= 0) {
            suggestionRepository.delete(suggestion);
        } else {
            suggestion.setUpdatedAt(LocalDateTime.now());
            suggestionRepository.save(suggestion);
        }
        return commandeId;
    }


    @Override
    @Transactional(readOnly = true)
    public BudgetCommandeDTO getBudgetCommande() {
        long budgetMensuel = appConfigurationService
            .findOneById(com.kobe.warehouse.constant.EntityConstant.APP_BUDGET_MENSUEL_COMMANDE)
            .map(c -> {
                try {
                    return Long.parseLong(c.getValue().trim());
                } catch (NumberFormatException _) {
                    return 0L;
                }
            })
            .orElse(0L);

        // Montant déjà engagé ce mois. Un brouillon n'engage rien : il peut être complété,
        // vidé ou supprimé sans que le grossiste en sache quoi que ce soit.
        Number montantCommandeRaw = (Number) em.createNativeQuery(
            """
                SELECT COALESCE(SUM(c.gross_amount), 0)
                FROM commande c
                WHERE (c.order_status = 'RECEIVED'
                       OR (c.order_status = 'REQUESTED' AND c.date_envoi IS NOT NULL))
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
        boolean enDepassement =
            !budgetIllimite && (montantCommande + montantEstime) > budgetMensuel;

        return new BudgetCommandeDTO(budgetMensuel, montantEstime, montantCommande, budgetRestant,
            enDepassement, budgetIllimite);
    }

    @Override
    public void addSuggestionLine(Integer suggestionId, SuggestionLineDTO suggestionLine) {
        Suggestion suggestion = suggestionRepository.findById(suggestionId).orElseThrow();
        suggestionLineRepository
            .findBySuggestionIdAndFournisseurProduitProduitId(suggestionId,
                suggestionLine.produitId())
            .ifPresentOrElse(
                line -> {
                    line.setQuantity(line.getQuantity() + suggestionLine.quantity());
                    line.setUpdatedAt(line.getCreatedAt());
                    suggestionLineRepository.save(line);
                },
                () -> {
                    Fournisseur fournisseur = suggestion.getFournisseur();
                    FournisseurProduit fournisseurProduit = fournisseurProduitRepository
                        .findOneByProduitIdAndFournisseurId(suggestionLine.produitId(),
                            fournisseur.getId())
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
        return exportToCsvBytes(this.suggestionRepository.findById(id)
            .orElseThrow(() -> new GenericError("Suggestion non trouvée")));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToPdf(Integer id) {
        Suggestion suggestion = suggestionRepository.findById(id)
            .orElseThrow(() -> new GenericError("Suggestion non trouvée"));
        Integer storageId = storageService.getDefaultMagasinMainStorage().getId();
        LocalDate dateRetention = LocalDate.now()
            .minusDays(appConfigurationService.getNombreJourRetentionCommande());
        int nthMois = appConfigurationService.getNthMoisConsommation();

        List<SuggestionLineDTO> lignes = suggestionLineRepository
            .fetchSuggestionLinesWithConsommation(
                suggestion.getId(), null, null, storageId, dateRetention, nthMois,
                Pageable.unpaged()
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
    public int suggestionQuantiteProduitVendus(List<QauntiteProduitVendus> produitVendus,
        Boolean suggerQuantitySold) {
        if (CollectionUtils.isEmpty(produitVendus)) {
            return 0;
        }

        // 1. Filter eligible produits
        List<QauntiteProduitVendus> eligibles = produitVendus.stream()
            .filter(p -> p.produitId() != null && etatProduitService.canSuggere(p.produitId()))
            .toList();
        if (eligibles.isEmpty()) {
            return 0;
        }

        // 2. Batch-load Produit entities
        Set<Integer> produitIds = eligibles.stream().map(QauntiteProduitVendus::produitId)
            .collect(Collectors.toSet());
        Map<Integer, Produit> produitById = produitRepository.findAllById(produitIds).stream()
            .filter(p -> p.getFournisseurProduitPrincipal() != null)
            .collect(Collectors.toMap(Produit::getId, Function.identity()));
        if (produitById.isEmpty()) {
            return 0;
        }

        // 3. Quantité à suggérer par produit
        Map<Integer, Integer> qtyByProduitId = eligibles.stream()
            .filter(q -> produitById.containsKey(q.produitId()))
            .collect(Collectors.toMap(
                QauntiteProduitVendus::produitId,
                q -> Boolean.TRUE.equals(suggerQuantitySold) ? q.quantitySold()
                    : q.quantityReappro()
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
            .collect(
                Collectors.groupingBy(p -> p.getFournisseurProduitPrincipal().getFournisseur()));

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
                if (qty <= 0) {
                    continue;
                }
                // Proposer d'emblee une quantite commandable : sinon l'ecran affiche 7 et la
                // commande part a 12, et l'ecart ressemble a un bug.
                qty = colisageService.ajusterQuantite(fp, qty);

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
                    if (alreadyExisted) {
                        linesToSave.add(newLine);
                    }
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

    private Suggestion getSuggestion(Fournisseur fournisseur, AtomicBoolean suggestionExist,
        Magasin magasin, AppUser user) {
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
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                        .concat(this.referenceService.buildSuggestionReference())
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

    private byte[] exportToCsvBytes(Suggestion suggestion) throws IOException {
        Integer storageId = storageService.getDefaultMagasinMainStorage().getId();
        LocalDate dateRetention = LocalDate.now()
            .minusDays(appConfigurationService.getNombreJourRetentionCommande());
        int nthMois = appConfigurationService.getNthMoisConsommation();

        List<SuggestionLineDTO> lines = suggestionLineRepository
            .fetchSuggestionLinesWithConsommation(
                suggestion.getId(), null, null, storageId, dateRetention, nthMois,
                Pageable.unpaged()
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
            "Code CIP", "Code EAN", "Désignation", "Stock", "Qté suggérée", "Prix achat",
            "Prix vente"
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

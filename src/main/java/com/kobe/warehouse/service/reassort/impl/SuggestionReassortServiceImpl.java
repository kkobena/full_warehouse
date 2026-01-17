package com.kobe.warehouse.service.reassort.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.LigneReassort;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.SuggestionReassort;
import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.repository.LigneReassortRepository;
import com.kobe.warehouse.repository.SuggestionReassortRepository;
import com.kobe.warehouse.service.ReferenceService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.reassort.dto.LigneReassortDto;
import com.kobe.warehouse.service.reassort.dto.ReassortRecord;
import com.kobe.warehouse.service.reassort.dto.SuggestionReassortDto;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;

@Service
@Transactional
public class SuggestionReassortServiceImpl implements SuggestionReassortService {

    private static final Logger LOG = LoggerFactory.getLogger(SuggestionReassortServiceImpl.class);
    private final SuggestionReassortRepository suggestionReassortRepository;
    private final LigneReassortRepository ligneReassortRepository;
    private final RepartitionStockService repartitionStockService;
    private final StorageService storageService;
    private final ReferenceService referenceService;

    public SuggestionReassortServiceImpl(SuggestionReassortRepository suggestionReassortRepository, LigneReassortRepository ligneReassortRepository, RepartitionStockService repartitionStockService, StorageService storageService, ReferenceService referenceService) {
        this.suggestionReassortRepository = suggestionReassortRepository;
        this.ligneReassortRepository = ligneReassortRepository;
        this.repartitionStockService = repartitionStockService;
        this.storageService = storageService;
        this.referenceService = referenceService;
    }

    @Override
    public void createLigneReassort(Produit p, int totalQty, int newQty) {

    }

    /**
     * orderLines doit etre filtrer avant l'appel de cette methode
     * Uniquement les produits avec un stock reserve defini
     *
     * @param reassortRecords
     */
    @Override
    public void createLigneReassort(List<ReassortRecord> reassortRecords) {
        if (CollectionUtils.isEmpty(reassortRecords)) {
            return;
        }
        AppUser user = getCurrentUser();
        Magasin magasin = user.getMagasin();

        SuggestionReassortContext context = findOrCreateSuggestionReassort(magasin, user, TypeReassort.RESERVE);
        SuggestionReassort suggestionReassort = context.suggestionReassort();
        boolean isNewSuggestion = context.isNew();

        Set<LigneReassort> ligneReassorts = suggestionReassort.getLigneReassorts();
        for (ReassortRecord reassortRecord : reassortRecords) {
            Optional<LigneReassort> oldLigneReassort = isNewSuggestion ?
                findExistingLigneReassort(ligneReassorts, reassortRecord.stockProduit().getId()) :
                Optional.empty();
            createLigneReassortEntity(isNewSuggestion, suggestionReassort, reassortRecord, oldLigneReassort);
        }
        suggestionReassortRepository.save(suggestionReassort);
    }

    private void createLigneReassortEntity(boolean isNewSuggestion, SuggestionReassort suggestionReassort,
                                           ReassortRecord reassortRecord, Optional<LigneReassort> oldLigneReassort) {
        StockProduit stockProduit = reassortRecord.stockProduit();
        if (isNull(stockProduit.getSeuilMini())) {
            LOG.debug("Le produit {} n'a pas de seuil mini defini, pas de creation de ligne reassort", stockProduit.getId());
            return;
        }
        if (Objects.requireNonNullElse(stockProduit.getQtyStock(), 0) >= stockProduit.getSeuilMini()) {
            LOG.debug("Le produit {} a un stock suffisant en reserve ({}), pas de creation de ligne reassort",
                stockProduit.getId(), stockProduit.getQtyStock());
            return;
        }
        int qtyToReassort = stockProduit.getSeuilMini() - stockProduit.getTotalStockQuantity();
        if (qtyToReassort > reassortRecord.availableQuantity()) {
            qtyToReassort = reassortRecord.availableQuantity();
        }

        updateOrCreateLigneReassort(isNewSuggestion, suggestionReassort, stockProduit, qtyToReassort, oldLigneReassort);
    }

    @Override
    public void deleteLigneReassort(Integer id) {
        ligneReassortRepository.deleteById(id);
    }

    @Override
    public void updateLigneReassort(Integer id, int quantity) {
        LigneReassort ligneReassort = ligneReassortRepository.getReferenceById(id);
        ligneReassort.setQuantity(quantity);
        ligneReassort.setUpdatedAt(LocalDateTime.now());
        ligneReassortRepository.save(ligneReassort);
    }

    @Override
    public void validateSuggestionReassort(Integer suggestionId) {
        SuggestionReassort suggestionReassort = suggestionReassortRepository.getReferenceById(suggestionId);
        if (suggestionReassort.getStatut() != StatutReassort.OPEN) {
            LOG.debug("Impossible de valider une suggestion de reassort qui n'est pas en statut OPEN");
            return;
        }
        suggestionReassort.setUpdatedAt(LocalDateTime.now());
        suggestionReassort.setLastUserEdit(getCurrentUser());
        repartitionStockService.process(suggestionReassort);
        suggestionReassort.setStatut(StatutReassort.CLOSED);
        suggestionReassortRepository.save(suggestionReassort);
    }

    @Override
    public void deleteSuggestionReassort(Integer suggestionId) {
        suggestionReassortRepository.deleteById(suggestionId);

    }

    @Override
    public List<SuggestionReassortDto> getOpenningSuggestions(TypeReassort typeReassort) {
        AppUser user = getCurrentUser();
        Magasin magasin = user.getMagasin();

        List<SuggestionReassort> suggestions;
        if (typeReassort != null) {
            suggestions = suggestionReassortRepository.findAllByStatutAndMagasinIdAndTypeReassort(
                StatutReassort.OPEN, magasin.getId(), typeReassort
            );
        } else {
            suggestions = suggestionReassortRepository.findAllByStatutAndMagasinId(
                StatutReassort.OPEN, magasin.getId()
            );
        }

        return suggestions.stream()
            .map(this::mapToDto)
            .toList();
    }

    private SuggestionReassortDto mapToDto(SuggestionReassort suggestion) {
        SuggestionReassortDto dto = new SuggestionReassortDto();
        dto.setId(suggestion.getId());
        dto.setReference(suggestion.getReference());
        dto.setCreated(suggestion.getCreatedAt());
        dto.setUpdated(suggestion.getUpdatedAt());
        dto.setStatut(suggestion.getStatut());
        dto.setTypeReassort(suggestion.getTypeReassort());

        if (suggestion.getLastUserEdit() != null) {
            dto.setUserFullName(suggestion.getLastUserEdit().getFirstName() + " " + suggestion.getLastUserEdit().getLastName());
        }

        if (suggestion.getLigneReassorts() != null) {
            dto.setLigneReassorts(
                suggestion.getLigneReassorts().stream()
                    .map(this::mapLigneToDto)
                    .toList()
            );
        }

        return dto;
    }

    private LigneReassortDto mapLigneToDto(LigneReassort ligne) {
        LigneReassortDto dto = new LigneReassortDto();
        dto.setId(ligne.getId());
        dto.setQuantity(ligne.getQuantity());

        StockProduit stockProduit = ligne.getStockProduit();
        if (stockProduit != null) {
            dto.setStockProduitId(stockProduit.getId());
            dto.setStockAvailable(stockProduit.getTotalStockQuantity());
            dto.setSeuilMini(stockProduit.getSeuilMini());
            dto.setStockActuel(stockProduit.getQtyStock());

            if (stockProduit.getStorage() != null) {
                dto.setStorageName(stockProduit.getStorage().getName());
            }

            Produit produit = stockProduit.getProduit();
            if (produit != null) {
                FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
                dto.setProduitLibelle(produit.getLibelle());
                dto.setProduitName(produit.getLibelle());
                if (fournisseurProduit != null) {
                    dto.setProduitCode(fournisseurProduit.getCodeCip());
                    dto.setCodeEanFabricant(fournisseurProduit.getCodeEan());
                }

            }
        }

        return dto;
    }

    /*
    Crée une suggestion de type rayon
     */
    @Override
    public void createRayonSuggestionReassort(@NotNull StockProduit stockProduitDest) {
        int qtyToReassort = Objects.requireNonNullElse(stockProduitDest.getStockReassort(), 0);
        if (qtyToReassort <= 0) {
            LOG.debug("Le produit {} n'a pas de qty a reassortir en rayon definie", stockProduitDest.getId());
            return;
        }
        Produit produit = stockProduitDest.getProduit();
        Set<StockProduit> stockProduits = produit.getStockProduits();
        if (stockProduits.size() == 1) {
            LOG.debug("Le produit {} n'est stocke que dans un seul stockage, pas de suggestion de reassort possible", produit.getId());
            return;
        }
        Storage storage = stockProduitDest.getStorage();
        if (storage.getStorageType() != StorageType.PRINCIPAL) {
            LOG.debug("Le produit {} n'est pas dans un stockage principal, pas de suggestion de reassort possible", produit.getId());
            return;
        }
        StockProduit stockSrc = getStockReserve(stockProduits);
        if (isNull(stockSrc) || Objects.requireNonNullElse(stockSrc.getTotalStockQuantity(), 0) <= 0) {
            LOG.debug("Le produit {} n'a pas de stock en reserve , pas de suggestion de reassort possible", produit.getId());
            return;
        }

        AppUser user = getCurrentUser();
        Magasin magasin = user.getMagasin();
        TypeReassort typeReassort = TypeReassort.RAYON;

        SuggestionReassortContext context = findOrCreateSuggestionReassort(magasin, user, typeReassort);
        SuggestionReassort suggestionReassort = context.suggestionReassort();
        boolean isNewSuggestion = context.isNew();

        Set<LigneReassort> ligneReassorts = suggestionReassort.getLigneReassorts();
        Optional<LigneReassort> oldLigneReassort = findExistingLigneReassort(ligneReassorts, stockProduitDest.getId());
        createLigneReassortForStockRayon(isNewSuggestion, suggestionReassort, stockProduitDest, stockSrc, oldLigneReassort);

        suggestionReassortRepository.save(suggestionReassort);


    }

    @Override
    public void createReserveSuggestionReassort(@NotNull StockProduit stockProduitSrc) {
        Produit produit = stockProduitSrc.getProduit();

        if (stockProduitSrc.getTotalStockQuantity() <= Objects.requireNonNullElse(stockProduitSrc.getSeuilMini(), 0)) {
            return;
        }
        Set<StockProduit> stockProduits = produit.getStockProduits();
        if (stockProduits.size() == 1 || isNull(stockProduitSrc.getStockMaxi()) || stockProduitSrc.getStockMaxi() == 0 || stockProduitSrc.getStockMaxi() > stockProduitSrc.getTotalStockQuantity()) {
            LOG.debug("Le produit {} n'est stocke que dans un seul stockage, pas de suggestion de reassort possible", produit.getId());
            return;
        }
        StockProduit stockDest = getStockReserve(stockProduits);
        if (isNull(stockDest) || stockDest.getTotalStockQuantity() >= Objects.requireNonNullElse(stockDest.getSeuilMini(), 0)) {
            LOG.debug("Le produit {} n'a pas de stock en reserve ou le stock reserve est suffisant, pas de suggestion de reassort possible", produit.getId());
            return;
        }
        AppUser user = getCurrentUser();
        Magasin magasin = user.getMagasin();
        TypeReassort typeReassort = TypeReassort.RESERVE;


        SuggestionReassortContext context = findOrCreateSuggestionReassort(magasin, user, typeReassort);
        SuggestionReassort suggestionReassort = context.suggestionReassort();
        boolean isNewSuggestion = context.isNew();

        Set<LigneReassort> ligneReassorts = suggestionReassort.getLigneReassorts();
        Optional<LigneReassort> oldLigneReassort = findExistingLigneReassort(ligneReassorts, stockDest.getId());

        createLigneReassortForStockReserve(isNewSuggestion, suggestionReassort, stockDest, stockProduitSrc, oldLigneReassort);
        suggestionReassortRepository.save(suggestionReassort);


    }

    private AppUser getCurrentUser() {
        return storageService.getUser();
    }

    /**
     * Finds or creates a SuggestionReassort entity
     *
     * @param magasin      the magasin
     * @param user         the user making the change
     * @param typeReassort the type of reassortment
     * @return a pair of SuggestionReassort and boolean indicating if it's new
     */
    private SuggestionReassortContext findOrCreateSuggestionReassort(Magasin magasin, AppUser user, TypeReassort typeReassort) {
        boolean isNewSuggestion = false;
        SuggestionReassort suggestionReassort = suggestionReassortRepository
            .findOneByStatutAndMagasinIdAndTypeReassort(StatutReassort.OPEN, magasin.getId(), typeReassort)
            .orElse(null);

        if (isNull(suggestionReassort)) {
            suggestionReassort = new SuggestionReassort();
            suggestionReassort.setCreatedAt(LocalDateTime.now());
            suggestionReassort.setReference(referenceService.buildNumReassort());
            isNewSuggestion = true;
        }

        suggestionReassort.setMagasin(magasin);
        suggestionReassort.setLastUserEdit(user);
        suggestionReassort.setTypeReassort(typeReassort);
        suggestionReassort.setUpdatedAt(LocalDateTime.now());

        return new SuggestionReassortContext(suggestionReassort, isNewSuggestion);
    }

    /**
     * Finds an existing LigneReassort for a given StockProduit
     *
     * @param ligneReassorts the set of existing ligne reassorts
     * @param stockProduitId the stock produit ID to search for
     * @return Optional containing the found LigneReassort
     */
    private Optional<LigneReassort> findExistingLigneReassort(Set<LigneReassort> ligneReassorts, Integer stockProduitId) {
        return ligneReassorts.stream()
            .filter(lr -> lr.getStockProduit().getId().equals(stockProduitId))
            .findFirst();
    }

    /**
     * Updates an existing LigneReassort or creates a new one
     *
     * @param isNewSuggestion    whether the parent SuggestionReassort is new
     * @param suggestionReassort the parent SuggestionReassort
     * @param stockProduit       the StockProduit for this ligne
     * @param qtyToReassort      the quantity to reassort
     * @param oldLigneReassort   optional existing LigneReassort to update
     */
    private void updateOrCreateLigneReassort(boolean isNewSuggestion, SuggestionReassort suggestionReassort,
                                             StockProduit stockProduit, int qtyToReassort,
                                             Optional<LigneReassort> oldLigneReassort) {
        oldLigneReassort.ifPresentOrElse(
            lr -> {
                lr.setQuantity(qtyToReassort);
                lr.setUpdatedAt(LocalDateTime.now());
                ligneReassortRepository.save(lr);
            },
            () -> {
                LigneReassort ligneReassort = new LigneReassort();
                ligneReassort.setStockProduit(stockProduit);
                ligneReassort.setQuantity(qtyToReassort);
                ligneReassort.setReassort(suggestionReassort);
                ligneReassort.setUpdatedAt(LocalDateTime.now());
                suggestionReassort.getLigneReassorts().add(ligneReassort);
                if (!isNewSuggestion) {
                    ligneReassortRepository.save(ligneReassort);
                }
            }
        );
    }

    private void createLigneReassortForStockRayon(boolean isNewSuggestion, SuggestionReassort suggestionReassort,
                                                  StockProduit stockProduitDesc, StockProduit reserveStockProduit,
                                                  Optional<LigneReassort> oldLigneReassort) {
        int qtyToReassort = stockProduitDesc.getStockReassort();
        if (reserveStockProduit.getTotalStockQuantity() <= qtyToReassort) {
            qtyToReassort = reserveStockProduit.getTotalStockQuantity();
        }

        updateOrCreateLigneReassort(isNewSuggestion, suggestionReassort, stockProduitDesc, qtyToReassort, oldLigneReassort);
    }

    private void createLigneReassortForStockReserve(boolean isNewSuggestion, SuggestionReassort suggestionReassort,
                                                    StockProduit stockProduitDest, StockProduit stockProduitSrc, Optional<LigneReassort> oldLigneReassort) {

        int qtyMaxiSrc = Objects.requireNonNullElse(stockProduitSrc.getStockMaxi(), 0);
        int availableToReassort = stockProduitSrc.getTotalStockQuantity() - qtyMaxiSrc;
        updateOrCreateLigneReassort(isNewSuggestion, suggestionReassort, stockProduitDest, availableToReassort, oldLigneReassort);
    }

    /**
     * Context holder for SuggestionReassort creation
     */
    private record SuggestionReassortContext(SuggestionReassort suggestionReassort, boolean isNew) {
    }

    private StockProduit getStockReserve(Set<StockProduit> stockProduits) {
        if (CollectionUtils.isEmpty(stockProduits)) {
            return null;
        }
        Storage storageReserve = storageService.getDefaultConnectedUserReserveStorage();
        return stockProduits.stream()
            .filter(sp -> Objects.equals(sp.getStorage(), storageReserve))
            .findFirst()
            .orElse(null);
    }

    private StockProduit getStockRayon(Set<StockProduit> stockProduits) {
        if (CollectionUtils.isEmpty(stockProduits)) {
            return null;
        }
        Storage storageRayon = storageService.getDefaultConnectedUserMainStorage();
        return stockProduits.stream()
            .filter(sp -> Objects.equals(sp.getStorage(), storageRayon))
            .findFirst()
            .orElse(null);
    }
}

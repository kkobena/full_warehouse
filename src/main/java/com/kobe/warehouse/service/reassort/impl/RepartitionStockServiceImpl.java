package com.kobe.warehouse.service.reassort.impl;

import static java.util.Objects.isNull;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.LigneReassort;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RepartitionStockProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.SuggestionReassort;
import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.domain.enumeration.TypeRepartition;
import com.kobe.warehouse.repository.RepartitionStockProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.projection.RepartitionStockProduitProjection;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.stock.LotStockLocationService;
import com.kobe.warehouse.service.reassort.dto.RepartionQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartitionStockProduitDto;
import com.kobe.warehouse.service.report.pdf.RepartitionStockPdfReportService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class RepartitionStockServiceImpl implements RepartitionStockService {

    private final StorageService storageService;
    private final RepartitionStockProduitRepository repartitionStockProduitRepository;
    private final StockProduitRepository stockProduitRepository;
    private final RepartitionStockPdfReportService repartitionStockPdfReportService;
    private final InventoryTransactionService inventoryTransactionService;
    private final LotStockLocationService lotStockLocationService;

    public RepartitionStockServiceImpl(
        StorageService storageService,
        RepartitionStockProduitRepository repartitionStockProduitRepository,
        StockProduitRepository stockProduitRepository,
        RepartitionStockPdfReportService repartitionStockPdfReportService,
        InventoryTransactionService inventoryTransactionService,
        LotStockLocationService lotStockLocationService
    ) {
        this.storageService = storageService;
        this.repartitionStockProduitRepository = repartitionStockProduitRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.repartitionStockPdfReportService = repartitionStockPdfReportService;
        this.inventoryTransactionService = inventoryTransactionService;
        this.lotStockLocationService = lotStockLocationService;
    }

    @Override
    public void process(SuggestionReassort suggestionReassort) {
        if (isNull(suggestionReassort) || suggestionReassort.getStatut() != StatutReassort.OPEN) {
            return;
        }

        Set<LigneReassort> ligneReassorts = suggestionReassort.getLigneReassorts();
        if (CollectionUtils.isEmpty(ligneReassorts)) {
            return;
        }
        AppUser user = suggestionReassort.getLastUserEdit();

        TypeReassort typeReassort = suggestionReassort.getTypeReassort();
        Storage srcStorage = typeReassort == TypeReassort.RESERVE
            ? storageService.getDefaultConnectedUserMainStorage()
            : storageService.getDefaultConnectedUserReserveStorage();
        for (LigneReassort reassort : ligneReassorts) {
            StockProduit stockProduitDest = reassort.getStockProduit();
            StockProduit stockProduitSrc = reassort.getStockProduitSrc();
            int stockDesFinal = stockProduitDest.getTotalStockQuantity() + reassort.getQuantity();

            RepartitionStockProduit repartitionStockProduit = createBaseRepartitionStockProduit(
                user);
            setDestinationStockInfo(repartitionStockProduit, stockProduitDest,
                reassort.getQuantity(), stockProduitDest.getTotalStockQuantity(), stockDesFinal);
            stockProduitSrc =
                isNull(stockProduitSrc) ? stockProduitDest.getProduit().getStockProduits().stream()
                    .filter(sp -> sp.getStorage().getId().equals(srcStorage.getId()))
                    .findFirst()
                    .orElseThrow(() -> new GenericError("Stock source inconnu")) : stockProduitSrc;

            int stockSrcFinal = stockProduitSrc.getTotalStockQuantity() - reassort.getQuantity();
            setSourceStockInfo(repartitionStockProduit, stockProduitSrc,
                stockProduitSrc.getTotalStockQuantity(), stockSrcFinal);
            updateStockQuantity(stockProduitSrc, stockSrcFinal);

            updateStockQuantity(stockProduitDest, stockDesFinal);

            repartitionStockProduitRepository.save(repartitionStockProduit);
            inventoryTransactionService.saveRepartition(repartitionStockProduit);

            // FEFO : transfert lots dans le sens du mouvement
            StockProduit finalStockProduitSrc = stockProduitSrc;
            lotStockLocationService.transferFefo(
                stockProduitDest.getProduit(),
                finalStockProduitSrc.getStorage(),
                stockProduitDest.getStorage(),
                reassort.getQuantity()
            );
        }
    }

    @Override
    public void processReassortStockRayon(Set<LigneReassort> ligneReassorts) {
        if (CollectionUtils.isEmpty(ligneReassorts)) {
            return;
        }
        Storage storageReserve = storageService.getDefaultConnectedUserReserveStorage();

        AppUser user = getCurrentUser();
        for (LigneReassort reassort : ligneReassorts) {
            StockProduit stockProduitDest = reassort.getStockProduit();
            StockProduit stockProduitSrc = reassort.getStockProduitSrc();
            Produit produit = stockProduitDest.getProduit();
            stockProduitSrc = isNull(stockProduitSrc) ? produit.getStockProduits().stream()
                .filter(sp -> sp.getStorage().getId().equals(storageReserve.getId()))
                .findFirst()
                .orElse(null) : stockProduitSrc;
            if (stockProduitSrc == null) {
                continue;
            }

            int stockDesFinal = stockProduitDest.getTotalStockQuantity() + reassort.getQuantity();
            int stockSrcFinal = stockProduitSrc.getTotalStockQuantity() - reassort.getQuantity();

            RepartitionStockProduit repartitionStockProduit = createBaseRepartitionStockProduit(
                user);
            setSourceStockInfo(repartitionStockProduit, stockProduitSrc,
                stockProduitSrc.getTotalStockQuantity(), stockSrcFinal);
            setDestinationStockInfo(repartitionStockProduit, stockProduitDest,
                reassort.getQuantity(), stockProduitDest.getTotalStockQuantity(), stockDesFinal);

            updateStockQuantity(stockProduitDest,
                stockProduitDest.getTotalStockQuantity() + reassort.getQuantity());
            updateStockQuantity(stockProduitSrc, stockSrcFinal);

            repartitionStockProduitRepository.save(repartitionStockProduit);
            inventoryTransactionService.saveRepartition(repartitionStockProduit);

            // FEFO : réserve → rayon lors du réassort batch
            lotStockLocationService.transferFefo(
                produit,
                stockProduitSrc.getStorage(),
                stockProduitDest.getStorage(),
                reassort.getQuantity()
            );
        }
    }

    @Override
    public void process(List<RepartionQueryDto> datas) {
        Storage storageReserve = storageService.getDefaultConnectedUserReserveStorage();
        Storage storagePrincipal = storageService.getDefaultConnectedUserMainStorage();
        if (CollectionUtils.isEmpty(datas)) {
            return;
        }
        for (RepartionQueryDto queryDto : datas) {
            StockProduit stockProduitSrc = stockProduitRepository.getReferenceById(
                queryDto.stockSourceId());
            Produit produit = stockProduitSrc.getProduit();

            boolean destIsReserveStorage =
                stockProduitSrc.getStorage().getStorageType().isVendable();
            Storage destStorage = destIsReserveStorage ? storageReserve : storagePrincipal;

            StockProduit stockProduitDest =
                getStockByStorage(produit.getStockProduits(), destStorage);

            // Création automatique du StockProduit destination si demandé
            if (isNull(stockProduitDest)) {
                if (!queryDto.createNewDestination()) {
                    throw new GenericError(
                        "Aucun emplacement réserve trouvé pour ce produit. Cochez 'Créer la réserve' pour le créer automatiquement.");
                }
                stockProduitDest = new StockProduit();
                stockProduitDest.setProduit(produit);
                stockProduitDest.setStorage(destStorage);
                stockProduitDest.setQtyStock(0);
                stockProduitDest.setQtyVirtual(0);
                stockProduitDest.setQtyUG(0);
                stockProduitDest.setCreatedAt(LocalDateTime.now());
                stockProduitDest.setUpdatedAt(stockProduitDest.getCreatedAt());
                stockProduitDest = stockProduitRepository.save(stockProduitDest);
            }

            int stockSrc = stockProduitSrc.getTotalStockQuantity();
            if (stockSrc <= queryDto.quantity()) {
                continue;
            }
            int stockSrcFinal = stockSrc - queryDto.quantity();

            int destInitStock = stockProduitDest.getTotalStockQuantity();
            int stockDestFinal = destInitStock + queryDto.quantity();

            RepartitionStockProduit repartitionStockProduit = createBaseRepartitionStockProduit(
                getCurrentUser());
            setSourceStockInfo(repartitionStockProduit, stockProduitSrc, stockSrc, stockSrcFinal);
            setDestinationStockInfo(repartitionStockProduit, stockProduitDest, queryDto.quantity(),
                destInitStock, stockDestFinal);
            repartitionStockProduitRepository.save(repartitionStockProduit);
            inventoryTransactionService.saveRepartition(repartitionStockProduit);

            updateStockQuantity(stockProduitSrc, stockSrcFinal);
            updateStockQuantity(stockProduitDest, stockDestFinal);

            // FEFO : déplacer les lots du plus proche en expiry en premier
            lotStockLocationService.transferFefo(
                produit,
                stockProduitSrc.getStorage(),
                stockProduitDest.getStorage(),
                queryDto.quantity()
            );
        }
    }


    @Override
    public Page<RepartitionStockProduitDto> fetchRepartitionStockProduits(
        RepartionSearchQueryDto searchQueryDto, Pageable pageable) {
        Integer typeRepartitionOrdinal = searchQueryDto.typeRepartition() != null
            ? searchQueryDto.typeRepartition().ordinal()
            : null;

        return repartitionStockProduitRepository.findRepartitionStockProduits(
            searchQueryDto.userId(),
            typeRepartitionOrdinal,
            searchQueryDto.storageId(),
            searchQueryDto.stockProduitId(),
            searchQueryDto.searchTerm(),
            searchQueryDto.dateDebut(),
            searchQueryDto.dateFin(),
            pageable
        ).map(this::mapProjectionToDto);
    }

    @Override
    public void transferStockBetweenStorages(StockProduit stockProduitDest) {
        if (stockProduitDest.getQtyStock() <= 0) {
            return;
        }
        Storage stockageRayon = storageService.getDefaultConnectedUserMainStorage();
        Produit produit = stockProduitDest.getProduit();
        Set<StockProduit> stockProduits = produit.getStockProduits();
        StockProduit stockProduitSrc = getStockByStorage(stockProduits, stockageRayon);
        if (isNull(stockProduitSrc)) {
            return;
        }
        int stockSrcInitial = stockProduitSrc.getTotalStockQuantity();
        int stockSrcFinal =
            stockProduitSrc.getTotalStockQuantity() - stockProduitDest.getQtyStock();
        RepartitionStockProduit repartitionStockProduit = createBaseRepartitionStockProduit(
            getCurrentUser());
        setDestinationStockInfo(repartitionStockProduit, stockProduitDest,
            stockProduitDest.getQtyStock(), 0, stockProduitDest.getQtyStock());
        setSourceStockInfo(repartitionStockProduit, stockProduitSrc, stockSrcInitial,
            stockSrcFinal);
        repartitionStockProduitRepository.save(repartitionStockProduit);
        inventoryTransactionService.saveRepartition(repartitionStockProduit);
        updateStockQuantity(stockProduitSrc, stockSrcFinal);
        // FEFO : rayon → réserve lors de la création d'un stock réserve
        lotStockLocationService.transferFefo(
            produit,
            stockProduitSrc.getStorage(),
            stockProduitDest.getStorage(),
            stockProduitDest.getQtyStock()
        );
    }

    @Override
    public void transfertImpliciteReserveVersRayon(
        Integer produitId, Integer rayonStorageId, Integer reserveStorageId, int quantity) {

        StockProduit stockRayon = stockProduitRepository.findOneByProduitIdAndStockageId(produitId,
            rayonStorageId);
        StockProduit stockReserve = stockProduitRepository.findOneByProduitIdAndStockageId(
            produitId, reserveStorageId);

        int reserveInit = stockReserve.getTotalStockQuantity();
        int reserveFinal = reserveInit - quantity;
        int rayonInit = stockRayon.getTotalStockQuantity();
        int rayonFinal = rayonInit + quantity;

        RepartitionStockProduit repartition = createBaseRepartitionStockProduit(getCurrentUser());
        repartition.setTypeRepartition(TypeRepartition.AUTO);
        setSourceStockInfo(repartition, stockReserve, reserveInit, reserveFinal);
        setDestinationStockInfo(repartition, stockRayon, quantity, rayonInit, rayonFinal);

        updateStockQuantity(stockReserve, reserveFinal);
        updateStockQuantity(stockRayon, rayonFinal);

        repartitionStockProduitRepository.save(repartition);
        inventoryTransactionService.saveRepartition(repartition);

        // FEFO : réserve → rayon lors d'un réassort implicite (vente urgente)
        lotStockLocationService.transferFefo(
            stockReserve.getProduit(),
            stockReserve.getStorage(),
            stockRayon.getStorage(),
            quantity
        );
    }

    @Override
    public void autoPutawayRayonToReserve(StockProduit rayonSp, StockProduit reserveSp, int qty) {
        if (qty <= 0) return;

        int rayonInit = rayonSp.getTotalStockQuantity();
        int rayonFinal = rayonInit - qty;
        int reserveInit = reserveSp.getTotalStockQuantity();
        int reserveFinal = reserveInit + qty;

        RepartitionStockProduit repartition = createBaseRepartitionStockProduit(getCurrentUser());
        repartition.setTypeRepartition(TypeRepartition.AUTO);
        setSourceStockInfo(repartition, rayonSp, rayonInit, rayonFinal);
        setDestinationStockInfo(repartition, reserveSp, qty, reserveInit, reserveFinal);

        updateStockQuantity(rayonSp, rayonFinal);
        updateStockQuantity(reserveSp, reserveFinal);

        repartitionStockProduitRepository.save(repartition);
        inventoryTransactionService.saveRepartition(repartition);

        // FEFO : rayon → réserve (auto-putaway à la réception)
        lotStockLocationService.transferFefo(
            rayonSp.getProduit(),
            rayonSp.getStorage(),
            reserveSp.getStorage(),
            qty
        );
    }

    @Override
    public byte[] exportRepartitionStockProduits(RepartionSearchQueryDto searchQueryDto) {
        Integer typeRepartitionOrdinal = searchQueryDto.typeRepartition() != null
            ? searchQueryDto.typeRepartition().ordinal()
            : null;

        List<RepartitionStockProduitDto> repartitions = repartitionStockProduitRepository.findRepartitionStockProduits(
            searchQueryDto.userId(),
            typeRepartitionOrdinal,
            searchQueryDto.storageId(),
            searchQueryDto.stockProduitId(),
            searchQueryDto.searchTerm(),
            searchQueryDto.dateDebut(),
            searchQueryDto.dateFin(),
            Pageable.unpaged()
        ).getContent().stream().map(this::mapProjectionToDto).toList();

        return repartitionStockPdfReportService.export(repartitions, searchQueryDto);
    }

    /**
     * Creates a base RepartitionStockProduit with common fields
     *
     * @param user the user performing the operation
     * @return a new RepartitionStockProduit with base fields set
     */
    private RepartitionStockProduit createBaseRepartitionStockProduit(AppUser user) {
        RepartitionStockProduit repartitionStockProduit = new RepartitionStockProduit();
        repartitionStockProduit.setCreated(LocalDateTime.now());
        repartitionStockProduit.setTypeRepartition(TypeRepartition.MANUEL);
        repartitionStockProduit.setUser(user);
        return repartitionStockProduit;
    }

    /**
     * Sets destination stock information on RepartitionStockProduit
     *
     * @param repartition  the RepartitionStockProduit entity
     * @param stockProduit the destination stock product
     * @param qtyMvt       the quantity to move
     * @param finalStock   the final stock after movement
     */
    private void setDestinationStockInfo(RepartitionStockProduit repartition,
        StockProduit stockProduit,
        int qtyMvt, int initSock, int finalStock) {
        repartition.setDestInitStock(initSock);
        repartition.setStockProduitDestination(stockProduit);
        repartition.setQtyMvt(qtyMvt);
        repartition.setDestFinalStock(finalStock);
    }

    /**
     * Sets source stock information on RepartitionStockProduit
     *
     * @param repartition  the RepartitionStockProduit entity
     * @param stockProduit the source stock product
     * @param finalStock   the final stock after movement
     */
    private void setSourceStockInfo(RepartitionStockProduit repartition, StockProduit stockProduit,
        int initSock, int finalStock) {
        repartition.setSourceInitStock(initSock);
        repartition.setStockProduitSource(stockProduit);
        repartition.setSourceFinalStock(finalStock);
    }

    /**
     * Updates stock quantity and saves the StockProduit
     *
     * @param stockProduit the stock product to update
     * @param newQty       the new quantity
     */
    private void updateStockQuantity(StockProduit stockProduit, int newQty) {

        stockProduit.setQtyVirtual(newQty);
        if (stockProduit.getQtyUG() > 0) {
            if (stockProduit.getQtyUG() >= newQty) {
                stockProduit.setQtyUG(newQty);
                stockProduit.setQtyStock(0);
            } else {
                stockProduit.setQtyStock(newQty - stockProduit.getQtyUG());
            }
        } else {
            stockProduit.setQtyUG(0);
            stockProduit.setQtyStock(newQty);
        }

        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);
    }

    /**
     * Gets the current user from the storage service
     *
     * @return the current AppUser
     */
    private AppUser getCurrentUser() {
        return storageService.getUser();
    }

    /**
     * Maps Spring Data projection to RepartitionStockProduitDto
     *
     * @param projection the Spring Data projection
     * @return mapped RepartitionStockProduitDto
     */
    private RepartitionStockProduitDto mapProjectionToDto(
        RepartitionStockProduitProjection projection) {
        RepartitionStockProduitDto dto = new RepartitionStockProduitDto();

        // Basic fields
        dto.setId(projection.getId());
        dto.setCreated(projection.getCreatedAt());
        dto.setMvtQty(projection.getQtyMvt());
        dto.setSourceInitStock(projection.getSourceInitStock());
        dto.setSourceFinalStock(projection.getSourceFinalStock());
        dto.setDestInitStock(projection.getDestInitStock());
        dto.setDestFinalStock(projection.getDestFinalStock());

        // User full name
        dto.setUserFullName(
            formatUserFullName(projection.getFirstName(), projection.getLastName()));

        // Product information
        dto.setProduitName(projection.getProduitName());
        dto.setProduitCode(projection.getProduitCodeEanLabo());
        dto.setCodeCip(projection.getCodeCip());

        // Source stock produit (optional)
        if (projection.getSrcStockId() != null) {
            dto.setStockProduitSrc(createStockProduitDTO(
                projection.getSrcStockId(),
                projection.getSrcStorageId(),
                projection.getSrcStorageName()
            ));
        }

        // Destination stock produit (required)
        dto.setStockProduitDest(createStockProduitDTO(
            projection.getDestStockId(),
            projection.getDestStorageId(),
            projection.getDestStorageName()
        ));

        return dto;
    }

    /**
     * Formats user full name from first and last name
     */
    private String formatUserFullName(String firstName, String lastName) {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return firstName != null ? firstName : lastName;
    }

    /**
     * Creates a StockProduitDTO from basic information
     */
    private StockProduitDTO createStockProduitDTO(Integer id, Integer storageId,
        String storageName) {
        StockProduitDTO dto = new StockProduitDTO();
        dto.setId(id);
        dto.setStorageId(storageId);
        dto.setStorageName(storageName);
        return dto;
    }

    private StockProduit getStockByStorage(Set<StockProduit> stockProduits, Storage storage) {
        if (CollectionUtils.isEmpty(stockProduits)) {
            return null;
        }
        return stockProduits.stream()
            .filter(sp -> Objects.equals(sp.getStorage(), storage))
            .findFirst()
            .orElse(null);
    }
}

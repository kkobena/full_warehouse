package com.kobe.warehouse.service.reassort.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.LigneReassort;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RepartitionStockProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeRepartition;
import com.kobe.warehouse.repository.RepartitionStockProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.projection.RepartitionStockProduitProjection;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.reassort.dto.RepartionQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartionSearchQueryDto;
import com.kobe.warehouse.service.reassort.dto.RepartitionStockProduitDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Transactional
public class RepartitionStockServiceImpl implements RepartitionStockService {
    private final StorageService storageService;
    private final RepartitionStockProduitRepository repartitionStockProduitRepository;
    private final StockProduitRepository stockProduitRepository;

    public RepartitionStockServiceImpl(StorageService storageService, RepartitionStockProduitRepository repartitionStockProduitRepository, StockProduitRepository stockProduitRepository) {
        this.storageService = storageService;
        this.repartitionStockProduitRepository = repartitionStockProduitRepository;
        this.stockProduitRepository = stockProduitRepository;
    }

    @Override
    public void process(Set<LigneReassort> ligneReassorts) {
        if (CollectionUtils.isEmpty(ligneReassorts)) {
            return;
        }
        AppUser user = getCurrentUser();
        for (LigneReassort reassort : ligneReassorts) {
            StockProduit stockProduitDest = reassort.getStockProduit();
            StockProduit stockProduitSrc = reassort.getStockProduitSrc();
            int stockDesFinal = stockProduitDest.getTotalStockQuantity() + reassort.getQuantity();

            RepartitionStockProduit repartitionStockProduit = createBaseRepartitionStockProduit(user);
            setDestinationStockInfo(repartitionStockProduit, stockProduitDest, reassort.getQuantity(), stockProduitDest.getTotalStockQuantity(), stockDesFinal);
            if (nonNull(stockProduitSrc)) {
                setSourceStockInfo(repartitionStockProduit, stockProduitSrc, stockProduitSrc.getTotalStockQuantity(), stockProduitSrc.getTotalStockQuantity() - reassort.getQuantity());
                updateStockQuantity(stockProduitSrc, stockProduitSrc.getTotalStockQuantity() - reassort.getQuantity());
            }

            updateStockQuantity(stockProduitDest, stockProduitDest.getTotalStockQuantity() + reassort.getQuantity());

            repartitionStockProduitRepository.save(repartitionStockProduit);
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

            RepartitionStockProduit repartitionStockProduit = createBaseRepartitionStockProduit(user);
            setSourceStockInfo(repartitionStockProduit, stockProduitSrc, stockProduitSrc.getTotalStockQuantity(), stockSrcFinal);
            setDestinationStockInfo(repartitionStockProduit, stockProduitDest, reassort.getQuantity(), stockProduitDest.getTotalStockQuantity(), stockDesFinal);

            updateStockQuantity(stockProduitDest, stockProduitDest.getTotalStockQuantity() + reassort.getQuantity());
            updateStockQuantity(stockProduitSrc, stockSrcFinal);

            repartitionStockProduitRepository.save(repartitionStockProduit);
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
            StockProduit stockProduitSrc = stockProduitRepository.getReferenceById(queryDto.stockSourceId());
            Produit produit = stockProduitSrc.getProduit();

            boolean destIsReserveStorage = stockProduitSrc.getStorage().getStorageType() == StorageType.PRINCIPAL;


            boolean isNewStockDest = isNull(queryDto.stockDestinationId());
            StockProduit stockProduitDest = destIsReserveStorage ? produit.getStockProduits().stream().filter(stockProduit -> stockProduit.getStorage().getId().equals(storageReserve.getId())).findFirst().orElse(null) : produit.getStockProduits().stream().filter(stockProduit -> stockProduit.getStorage().getId().equals(storagePrincipal.getId())).findFirst().orElse(null);

            int stockSrc = stockProduitSrc.getTotalStockQuantity();
            if (stockSrc <= queryDto.quantity()) {
                continue;
            }
            int stockSrcFinal = stockSrc - queryDto.quantity();
            int stockDestFinal;
            int destInitStock = 0;

            if (nonNull(stockProduitDest)) {
                destInitStock = stockProduitDest.getTotalStockQuantity();
                stockDestFinal = stockProduitDest.getTotalStockQuantity() + queryDto.quantity();

            } else {
                if (!destIsReserveStorage) {
                    throw new GenericError("Le produit " + produit.getLibelle() + " n'a pas de stock dans le stockage principal");
                }
                stockProduitDest = createReserveStockProduit(queryDto, stockProduitSrc, storageReserve);
                if (nonNull(stockProduitDest)) {
                    stockDestFinal = stockProduitDest.getQtyStock();
                } else {
                    throw new GenericError("Le produit " + produit.getLibelle() + " n'a pas de stock dans le stockage de réserve");
                }

            }

            RepartitionStockProduit repartitionStockProduit = createBaseRepartitionStockProduit(getCurrentUser());
            setSourceStockInfo(repartitionStockProduit, stockProduitSrc, stockSrc, stockSrcFinal);
            setDestinationStockInfo(repartitionStockProduit, stockProduitDest, queryDto.quantity(), destInitStock, stockDestFinal);
            repartitionStockProduitRepository.save(repartitionStockProduit);

            updateStockQuantity(stockProduitSrc, stockSrcFinal);
            if (!isNewStockDest) {
                updateStockQuantity(stockProduitDest, stockDestFinal);
            }

        }

    }

    private StockProduit createReserveStockProduit(RepartionQueryDto queryDto, StockProduit stockProduitSrc, Storage reserveStorage) {
        Storage sourceStorage = stockProduitSrc.getStorage();
        if (sourceStorage.getStorageType() == StorageType.SAFETY_STOCK) {
            return null;
        }
        Produit produit = stockProduitSrc.getProduit();
        StockProduit stockProduit = new StockProduit();
        stockProduit.setProduit(produit);
        stockProduit.setStorage(reserveStorage);
        stockProduit.setQtyStock(queryDto.quantity());
        stockProduit.setQtyUG(0);
        stockProduit.setQtyVirtual(queryDto.quantity());
        stockProduit.setSeuilMini(queryDto.seuilMini());
        stockProduit.setCreatedAt(LocalDateTime.now());
        stockProduit.setUpdatedAt(stockProduit.getCreatedAt());
        return stockProduitRepository.save(stockProduit);
    }


    @Override
    public Page<RepartitionStockProduitDto> fetchRepartitionStockProduits(RepartionSearchQueryDto searchQueryDto, Pageable pageable) {
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
        StockProduit stockProduitSrc = getStockRayon(stockProduits, stockageRayon);
        if (isNull(stockProduitSrc)) {
            return;
        }
        int stockSrcInitial = stockProduitSrc.getTotalStockQuantity();
        int stockSrcFinal = stockProduitSrc.getTotalStockQuantity() - stockProduitDest.getQtyStock();
        RepartitionStockProduit repartitionStockProduit = createBaseRepartitionStockProduit(getCurrentUser());
        setDestinationStockInfo(repartitionStockProduit, stockProduitDest,
            stockProduitDest.getQtyStock(), 0, stockProduitDest.getQtyStock());
        setSourceStockInfo(repartitionStockProduit, stockProduitSrc, stockSrcInitial, stockSrcFinal);
        repartitionStockProduitRepository.save(repartitionStockProduit);
        updateStockQuantity(stockProduitSrc, stockSrcFinal);
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
    private void setDestinationStockInfo(RepartitionStockProduit repartition, StockProduit stockProduit,
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
    private void setSourceStockInfo(RepartitionStockProduit repartition, StockProduit stockProduit, int initSock, int finalStock) {
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
    private RepartitionStockProduitDto mapProjectionToDto(RepartitionStockProduitProjection projection) {
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
        dto.setUserFullName(formatUserFullName(projection.getFirstName(), projection.getLastName()));

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
    private StockProduitDTO createStockProduitDTO(Integer id, Integer storageId, String storageName) {
        StockProduitDTO dto = new StockProduitDTO();
        dto.setId(id);
        dto.setStorageId(storageId);
        dto.setStorageName(storageName);
        return dto;
    }

    private StockProduit getStockRayon(Set<StockProduit> stockProduits, Storage storageRayon) {
        if (CollectionUtils.isEmpty(stockProduits)) {
            return null;
        }
        return stockProduits.stream()
            .filter(sp -> Objects.equals(sp.getStorage(), storageRayon))
            .findFirst()
            .orElse(null);
    }
}

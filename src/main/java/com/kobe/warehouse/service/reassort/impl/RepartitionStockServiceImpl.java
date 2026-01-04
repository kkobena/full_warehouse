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
            if(nonNull(stockProduitSrc)){
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
        String query = buildFetchQuery(searchQueryDto, false);
        String countQuery = buildFetchQuery(searchQueryDto, true);

        Page<Object[]> results = repartitionStockProduitRepository.findRepartitionStockProduitsDynamic(
            query,
            countQuery,
            searchQueryDto,
            pageable
        );

        return results.map(this::mapToRepartitionStockProduitDto);
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
        stockProduit.setQtyStock(newQty);
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
     * Builds dynamic SQL query based on provided search criteria
     *
     * @param searchQueryDto the search criteria
     * @param isCountQuery   whether to build a count query
     * @return the SQL query string
     */
    private String buildFetchQuery(RepartionSearchQueryDto searchQueryDto, boolean isCountQuery) {
        StringBuilder query = new StringBuilder();

        if (isCountQuery) {
            query.append("SELECT COUNT(r.id) ");
        } else {
            query.append("""
                SELECT r.id, r.created_at, r.qty_mvt, r.source_init_stock, r.source_final_stock,
                       r.dest_init_stock, r.dest_final_stock, r.type_repartition,
                       u.first_name, u.last_name,
                       p.libelle as produit_name, p.code_ean as produit_code, p.code_ean_fabricant,
                       src_sp.id as src_stock_id, src_sp.storage_id as src_storage_id,
                       src_s.name as src_storage_name,
                       dest_sp.id as dest_stock_id, dest_sp.storage_id as dest_storage_id,
                       dest_s.name as dest_storage_name
                """);
        }

        query.append("""
            FROM repartition_stock_produit r
            INNER JOIN app_user u ON r.user_id = u.id
            INNER JOIN stock_produit dest_sp ON r.stock_produit_destination_id = dest_sp.id
            INNER JOIN produit p ON dest_sp.produit_id = p.id
            INNER JOIN storage dest_s ON dest_sp.storage_id = dest_s.id
            LEFT JOIN stock_produit src_sp ON r.stock_produit_source_id = src_sp.id
            LEFT JOIN storage src_s ON src_sp.storage_id = src_s.id
            """);

        // Build WHERE clause dynamically
        StringBuilder whereClause = new StringBuilder(" WHERE 1=1 ");

        if (searchQueryDto.userId() != null) {
            whereClause.append(" AND r.user_id = :userId ");
        }

        if (searchQueryDto.typeRepartition() != null) {
            whereClause.append(" AND r.type_repartition = :typeRepartition ");
        }

        if (searchQueryDto.storageId() != null) {
            whereClause.append(" AND (dest_sp.storage_id = :storageId OR src_sp.storage_id = :storageId) ");
        }

        if (searchQueryDto.stockProduitId() != null) {
            whereClause.append(" AND (dest_sp.id = :stockProduitId OR src_sp.id = :stockProduitId) ");
        }

        if (searchQueryDto.searchTerm() != null && !searchQueryDto.searchTerm().isBlank()) {
            whereClause.append("""
                 AND (UPPER(p.libelle) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
                     OR UPPER(p.code_ean) LIKE UPPER(CONCAT('%', :searchTerm, '%'))
                     OR UPPER(p.code_ean_fabricant) LIKE UPPER(CONCAT('%', :searchTerm, '%')))
                """);
        }

        if (searchQueryDto.dateDebut() != null) {
            whereClause.append(" AND CAST(r.created_at AS date) >= :dateDebut ");
        }

        if (searchQueryDto.dateFin() != null) {
            whereClause.append(" AND CAST(r.created_at AS date) <= :dateFin ");
        }

        query.append(whereClause);

        if (!isCountQuery) {
            query.append(" ORDER BY r.created_at DESC ");
        }

        return query.toString();
    }

    /**
     * Maps Object array from native query to RepartitionStockProduitDto
     * Query result order:
     * 0: id, 1: created_at, 2: qty_mvt, 3: source_init_stock, 4: source_final_stock,
     * 5: dest_init_stock, 6: dest_final_stock, 7: type_repartition,
     * 8: first_name, 9: last_name,
     * 10: produit_name, 11: produit_code, 12: code_ean_fabricant,
     * 13: src_stock_id, 14: src_storage_id, 15: src_storage_name,
     * 16: dest_stock_id, 17: dest_storage_id, 18: dest_storage_name
     *
     * @param row the Object array from native query
     * @return mapped RepartitionStockProduitDto
     */
    private RepartitionStockProduitDto mapToRepartitionStockProduitDto(Object[] row) {
        RepartitionStockProduitDto dto = new RepartitionStockProduitDto();

        // Basic fields
        dto.setId(getIntegerValue(row, 0));
        dto.setCreated(getLocalDateTimeValue(row, 1));
        dto.setMvtQty(getIntegerValue(row, 2));
        dto.setSourceInitStock(getIntegerValue(row, 3));
        dto.setSourceFinalStock(getIntegerValue(row, 4));
        dto.setDestInitStock(getIntegerValue(row, 5));
        dto.setDestFinalStock(getIntegerValue(row, 6));

        // User full name
        String firstName = getStringValue(row, 8);
        String lastName = getStringValue(row, 9);
        dto.setUserFullName(formatUserFullName(firstName, lastName));

        // Product information
        dto.setProduitName(getStringValue(row, 10));
        dto.setProduitCode(getStringValue(row, 11));
        dto.setCodeEanFabricant(getStringValue(row, 12));

        // Source stock produit (optional)
        if (row[13] != null) {
            dto.setStockProduitSrc(createStockProduitDTO(
                getIntegerValue(row, 13),
                getIntegerValue(row, 14),
                getStringValue(row, 15)
            ));
        }

        // Destination stock produit (required)
        dto.setStockProduitDest(createStockProduitDTO(
            getIntegerValue(row, 16),
            getIntegerValue(row, 17),
            getStringValue(row, 18)
        ));

        return dto;
    }

    /**
     * Helper to safely extract Integer value from Object array
     */
    private Integer getIntegerValue(Object[] row, int index) {
        return row[index] != null ? ((Number) row[index]).intValue() : null;
    }

    /**
     * Helper to safely extract String value from Object array
     */
    private String getStringValue(Object[] row, int index) {
        return row[index] != null ? row[index].toString() : null;
    }

    /**
     * Helper to safely extract LocalDateTime value from Object array
     */
    private LocalDateTime getLocalDateTimeValue(Object[] row, int index) {
        if (row[index] == null) {
            return null;
        }
        if (row[index] instanceof LocalDateTime) {
            return (LocalDateTime) row[index];
        }
        // Handle java.sql.Timestamp if needed
        if (row[index] instanceof java.sql.Timestamp) {
            return ((java.sql.Timestamp) row[index]).toLocalDateTime();
        }
        return null;
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
}

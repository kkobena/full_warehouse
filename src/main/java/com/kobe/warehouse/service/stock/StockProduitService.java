package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import com.kobe.warehouse.service.stock.dto.StockProduitSearchDTO;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing StockProduit entities
 */
@Service
@Transactional
public class StockProduitService {

    private static final Logger LOG = LoggerFactory.getLogger(StockProduitService.class);

    private final StockProduitRepository stockProduitRepository;
    private final ProduitRepository produitRepository;
    private final StorageService storageService;
    private final RepartitionStockService repartitionStockService;


    public StockProduitService(
        StockProduitRepository stockProduitRepository,
        ProduitRepository produitRepository,
        StorageService storageService, RepartitionStockService repartitionStockService
    ) {
        this.stockProduitRepository = stockProduitRepository;
        this.produitRepository = produitRepository;
        this.storageService = storageService;

        this.repartitionStockService = repartitionStockService;
    }

    /**
     * Create a new StockProduit (reserve storage)
     */
    public StockProduitDTO createStockProduit(StockProduitDTO dto) {
        LOG.debug("Creating new StockProduit: {}", dto);

        Produit produit = produitRepository
            .findById(dto.getProduitId())
            .orElseThrow(() -> new EntityNotFoundException("Produit not found with id: " + dto.getProduitId()));

        // Find or create SAFETY_STOCK storage for this product
        Storage storageReserve = storageService.getDefaultConnectedUserReserveStorage();

        StockProduit stockProduit = new StockProduit();
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storageReserve);
        stockProduit.setQtyStock(dto.getQtyStock());
        stockProduit.setQtyVirtual(stockProduit.getQtyStock());
        stockProduit.setQtyUG(0);
        stockProduit.setSeuilMini(dto.getSeuilMini());
        stockProduit.setStockReassort(dto.getStockReassort());
        stockProduit.setCreatedAt(LocalDateTime.now());
        stockProduit.setUpdatedAt(stockProduit.getCreatedAt());
        stockProduit.setStockMaxi(dto.getStockMaxi());
        stockProduit = stockProduitRepository.save(stockProduit);
        LOG.debug("Created StockProduit with id: {}", stockProduit.getId());
        if (dto.isWithTransfer()) {
            repartitionStockService.transferStockBetweenStorages(stockProduit);
        }


        return new StockProduitDTO(stockProduit);
    }

    /**
     * Update an existing StockProduit
     */
    public StockProduitDTO updateStockProduit(StockProduitDTO dto) {
        LOG.debug("Updating StockProduit: {}", dto);

        StockProduit stockProduit = stockProduitRepository
            .findById(dto.getId())
            .orElseThrow(() -> new EntityNotFoundException("StockProduit not found with id: " + dto.getId()));

        // Only update the editable fields
        if (dto.getSeuilMini() != null) {
            stockProduit.setSeuilMini(dto.getSeuilMini());
        }
        if (dto.getStockReassort() != null) {
            stockProduit.setStockReassort(dto.getStockReassort());
        }
        if (dto.getStockMaxi() != null) {
            stockProduit.setStockMaxi(dto.getStockMaxi());
        }
        stockProduit.setUpdatedAt(LocalDateTime.now());

        stockProduit = stockProduitRepository.save(stockProduit);
        LOG.debug("Updated StockProduit with id: {}", stockProduit.getId());

        return new StockProduitDTO(stockProduit);
    }

    /**
     * Get a StockProduit by ID
     */
    @Transactional(readOnly = true)
    public StockProduitDTO getStockProduit(Integer id) {
        LOG.debug("Getting StockProduit with id: {}", id);
        StockProduit stockProduit = stockProduitRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("StockProduit not found with id: " + id));
        return new StockProduitDTO(stockProduit);
    }

    @Transactional(readOnly = true)
    public List<StockProduitSearchDTO> searchStockProduitsForRepartition(Integer storageId, String searchTerm) {
        Integer magasinId = storageService.getUser().getMagasin().getId();

        List<StockProduit> stockProduits = stockProduitRepository.searchStockProduitsForRepartition(
            storageId,
            magasinId,
            searchTerm
        );

        return stockProduits.stream()
            .map(this::mapToSearchDTO)
            .collect(Collectors.toList());
    }

    private StockProduitSearchDTO mapToSearchDTO(StockProduit stockProduit) {
        StockProduitSearchDTO dto = new StockProduitSearchDTO();
        dto.setId(stockProduit.getId());
        dto.setQtyStock(stockProduit.getQtyStock());
        dto.setSeuilMini(stockProduit.getSeuilMini());

        // Storage information
        Storage storage = stockProduit.getStorage();
        if (storage != null) {
            dto.setStorageId(storage.getId());
            dto.setStorageName(storage.getName());
            dto.setStorageType(storage.getStorageType() != null ? storage.getStorageType().name() : null);
        }

        // Product information
        Produit produit = stockProduit.getProduit();
        if (produit != null) {
            dto.setProduitId(produit.getId());
            dto.setProduitLibelle(produit.getLibelle());

            // Get codeCip from fournisseurProduitPrincipal
            if (produit.getFournisseurProduitPrincipal() != null) {
                dto.setProduitCodeCip(produit.getFournisseurProduitPrincipal().getCodeCip());
            }

            // Map all stocks for this product
            if (produit.getStockProduits() != null) {
                List<StockProduitDTO> allStocks = produit.getStockProduits().stream()
                    .map(this::fromStockProduit)
                    .collect(Collectors.toList());
                dto.setAllStocks(allStocks);
            } else {
                dto.setAllStocks(new ArrayList<>());
            }
        }

        return dto;
    }


    public StockProduitDTO fromStockProduit(StockProduit s) {
        Storage r = s.getStorage();
        Produit p = s.getProduit();

        return new StockProduitDTO()
            .setId(s.getId())
            .setQtyStock(s.getQtyStock())
            .setQtyVirtual(s.getQtyVirtual())
            .setQtyUG(s.getQtyUG())
            .setStorageId(r != null ? r.getId() : null)
            .setStorageName(r != null ? r.getName() : null)
            .setStorageType(r != null && r.getStorageType() != null ? r.getStorageType().getValue() : null)
            .setType(r != null ? r.getStorageType() : null)
            .setProduitId(p != null ? p.getId() : null)
            .setCreatedAt(p != null ? p.getCreatedAt() : null)
            .setUpdatedAt(p != null ? p.getUpdatedAt() : null)
            .setProduitLibelle(p != null ? p.getLibelle() : null)
            .setTotalStockQuantity(s.getTotalStockQuantity());
    }
}

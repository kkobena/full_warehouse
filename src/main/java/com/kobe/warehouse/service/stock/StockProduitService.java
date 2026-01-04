package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.reassort.RepartitionStockService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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

        stockProduit = stockProduitRepository.save(stockProduit);
        LOG.debug("Created StockProduit with id: {}", stockProduit.getId());

        //TODO: appel du service de reparation de stock pour mettre a jour les stocks en rayon si besoin

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


}

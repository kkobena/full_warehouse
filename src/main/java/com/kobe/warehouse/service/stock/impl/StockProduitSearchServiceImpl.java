package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.stock.StockProduitSearchService;
import com.kobe.warehouse.service.stock.dto.StockProduitSearchDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class StockProduitSearchServiceImpl implements StockProduitSearchService {

    private final StockProduitRepository stockProduitRepository;
    private final StorageService storageService;

    public StockProduitSearchServiceImpl(StockProduitRepository stockProduitRepository, StorageService storageService) {
        this.stockProduitRepository = stockProduitRepository;
        this.storageService = storageService;
    }

    @Override
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
                    .map(this::mapToStockProduitDTO)
                    .collect(Collectors.toList());
                dto.setAllStocks(allStocks);
            } else {
                dto.setAllStocks(new ArrayList<>());
            }
        }

        return dto;
    }

    private StockProduitDTO mapToStockProduitDTO(StockProduit sp) {
        StockProduitDTO dto = new StockProduitDTO();
        dto.setId(sp.getId());
        dto.setQtyStock(sp.getQtyStock());
        dto.setQtyUG(sp.getQtyUG());
        dto.setSeuilMini(sp.getSeuilMini());

        if (sp.getStorage() != null) {
            dto.setStorageId(sp.getStorage().getId());
            dto.setStorageName(sp.getStorage().getName());
            dto.setStorageType(sp.getStorage().getStorageType() != null ? sp.getStorage().getStorageType().name() : null);
        }

        if (sp.getProduit() != null) {
            dto.setProduitId(sp.getProduit().getId());
            dto.setProduitLibelle(sp.getProduit().getLibelle());
        }

        return dto;
    }
}

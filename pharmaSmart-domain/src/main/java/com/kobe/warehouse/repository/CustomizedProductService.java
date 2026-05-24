package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomizedProductService {
    List<ProduitDTO> findAll(ProduitCriteria produitCriteria) throws Exception;

    Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable) throws Exception;

    Optional<ProduitDTO> findOneById(Integer ProduitId);

    void save(StockProduitDTO dto) throws Exception;

    Optional<FournisseurProduit> getFournisseurProduitByCriteria(String criteteria, Integer fournisseurId);


    LocalDateTime lastSale(ProduitCriteria produitCriteria);

    LocalDateTime lastOrder(ProduitCriteria produitCriteria);

    List<Produit> find(ProduitCriteria produitCriteria);


    default StockProduit buildStockProduitFromStockProduitDTO(StockProduitDTO dto) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setCreatedAt(LocalDateTime.now());
        stockProduit.setQtyStock(dto.getQtyStock());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduit.setQtyVirtual(dto.getQtyVirtual());
        stockProduit.setQtyUG(dto.getQtyUG());
        stockProduit.setStorage(storageFromId(dto.getStorageId()));
        return stockProduit;
    }

    default Storage storageFromId(Integer id) {
        if (id == null) {
            return null;
        }
        Storage storage = new Storage();
        storage.setId(id);
        return storage;
    }


    List<ProduitDTO> lite(ProduitCriteria produitCriteria);

    List<ProduitDTO> productsLiteList(ProduitCriteria produitCriteria, Pageable pageable);
}

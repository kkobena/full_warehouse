package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomizedProductService {
    List<ProduitDTO> findAll(ProduitCriteria produitCriteria) throws Exception;

    Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable) throws Exception;

    Optional<ProduitDTO> findOneById(Integer ProduitId);

    void save(ProduitDTO dto, Rayon rayon) throws Exception;

    void save(Produit produit) throws Exception;

    void update(ProduitDTO dto) throws Exception;

    void save(StockProduitDTO dto) throws Exception;

    void update(StockProduitDTO dto) throws Exception;

    void save(FournisseurProduitDTO dto) throws Exception;

    void update(FournisseurProduitDTO dto) throws Exception;

    void removeFournisseurProduit(Integer id) throws Exception;

    Optional<FournisseurProduit> getFournisseurProduitByCriteria(String criteteria, Integer fournisseurId);

    int produitTotalStock(Produit produit);

    int produitTotalStockWithQantityUg(Produit produit);

    FournisseurProduit fournisseurProduitProduit(Produit produit, ProduitDTO dto);

    StockProduit stockProduitFromProduitDTO(ProduitDTO dto);

    LocalDateTime lastSale(ProduitCriteria produitCriteria);

    LocalDateTime lastOrder(ProduitCriteria produitCriteria);

    void updateDetail(ProduitDTO dto);

    StockProduit updateTotalStock(Produit produit, int stockIn, int stockUg);

    void updateFromCommande(ProduitDTO dto, Produit produit);

    List<Produit> find(ProduitCriteria produitCriteria);

    List<Produit> findByIds(Set<Integer> ids);

    default FournisseurProduit buildFournisseurProduitFromFournisseurProduitDTO(FournisseurProduitDTO dto) {
        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setCodeCip(dto.getCodeCip());
        fournisseurProduit.setPrixAchat(dto.getPrixAchat());
        fournisseurProduit.setPrixUni(dto.getPrixUni());
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        return fournisseurProduit;
    }

    default Fournisseur fournisseurFromId(Integer id) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(id);
        return fournisseur;
    }

    default FournisseurProduit buildFournisseurProduitFromFournisseurProduitDTO(
        FournisseurProduitDTO dto,
        FournisseurProduit fournisseurProduit
    ) {
        fournisseurProduit.setCodeCip(dto.getCodeCip());
        fournisseurProduit.setPrixAchat(dto.getPrixAchat());
        fournisseurProduit.setPrixUni(dto.getPrixUni());
        fournisseurProduit.setFournisseur(fournisseurFromId(dto.getFournisseurId()));
        return fournisseurProduit;
    }

    default StockProduit buildStockProduitFromStockProduitDTO(StockProduitDTO dto, StockProduit stockProduit) {
        stockProduit.setQtyStock(dto.getQtyStock());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduit.setQtyVirtual(dto.getQtyVirtual());
        stockProduit.setQtyUG(dto.getQtyUG());
        stockProduit.setStorage(storageFromId(dto.getStorageId()));
        return stockProduit;
    }

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

    default Rayon rayonFromId(Integer id) {
        if (id == null) {
            return null;
        }
        Rayon rayon = new Rayon();
        rayon.setId(id);
        return rayon;
    }

    default Tva tvaFromId(Integer id) {
        if (id == null) {
            return null;
        }
        Tva tva = new Tva();
        tva.setId(id);
        return tva;
    }

    default Produit buildDetailProduitFromProduitDTO(ProduitDTO produitDTO, Produit produit) {
        produit.setUpdatedAt(LocalDateTime.now());
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCostAmount(produitDTO.getCostAmount());
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        //   produit.addStockProduit(stockProduitFromProduitDTO(produitDTO));
        //  produit.addFournisseurProduit(fournisseurProduitFromDTO(produitDTO));
        return produit;
    }

    List<ProduitDTO> lite(ProduitCriteria produitCriteria);

    List<ProduitDTO> productsLiteList(ProduitCriteria produitCriteria, Pageable pageable);
}

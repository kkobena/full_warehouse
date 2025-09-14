package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.kobe.warehouse.domain.Produit}.
 */
public interface ProduitService {
    /**
     * Save a produit.
     *
     * @param produitDTO the entity to save.
     * @return the persisted entity.
     */
    void save(ProduitDTO produitDTO);

    /**
     * Get all the produits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<ProduitDTO> findAll(Pageable pageable);

    /**
     * Get the "id" produit.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<ProduitDTO> findOne(Long id);

    /**
     * Delete the "id" produit.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);

    Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable);

    LocalDateTime lastSale(ProduitCriteria produitCriteria);


    LocalDateTime lastOrder(ProduitCriteria produitCriteria);

    ProduitDTO findOne(ProduitCriteria produitCriteria);

    List<ProduitDTO> findWithCriteria(ProduitCriteria produitCriteria);

    void update(ProduitDTO produitDTO);


    List<ProduitDTO> productsLiteList(ProduitCriteria produitCriteria, Pageable pageable);

    void updateDetail(ProduitDTO produitDTO);

    int getProductTotalStock(Long productId);

    StockProduit updateTotalStock(Produit produit, int stockIn, int stockUg);

    void update(Produit produit);

    default int calculPrixMoyenPondereReception(int oldStock, int oldPrixAchat, int newStock, int newPrixAchat) {
        return ((oldStock * oldPrixAchat) + (newStock * newPrixAchat)) / (oldStock + newStock);
    }

    void updateFromCommande(ProduitDTO produitDTO, Produit produit);

    void updatePeremption(Long produitId, LocalDate peremptionDate);

    Produit findReferenceById(Long id);

    List<ProduitSearch> searchProducts(String search ,Long magasinId, Pageable pageable);
}

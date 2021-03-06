package com.kobe.warehouse.service;


import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.ResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.InputStream;
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

    SalesLine lastSale(ProduitCriteria produitCriteria);

    StoreInventoryLine lastInventory(ProduitCriteria produitCriteria);

    OrderLine lastOrder(ProduitCriteria produitCriteria);

    ProduitDTO findOne(ProduitCriteria produitCriteria);

    List<ProduitDTO> findWithCriteria(ProduitCriteria produitCriteria);

    void update(ProduitDTO produitDTO);

    List<ProduitDTO> lite(String query);

    void updateDetail(ProduitDTO produitDTO);
}

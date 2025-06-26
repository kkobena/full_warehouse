package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.service.ProduitService;
import com.kobe.warehouse.service.dto.*;
import com.kobe.warehouse.service.dto.builder.ProduitBuilder;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.kobe.warehouse.domain.Produit}.
 */
@Service
@Transactional
public class ProduitServiceImpl implements ProduitService {

    private final Logger log = LoggerFactory.getLogger(ProduitServiceImpl.class);
    private final MagasinRepository magasinRepository;
    private final ProduitRepository produitRepository;
    private final CustomizedProductService customizedProductService;
    private final RayonRepository rayonRepository;

    public ProduitServiceImpl(
        MagasinRepository magasinRepository,
        ProduitRepository produitRepository,
        CustomizedProductService customizedProductService,
        RayonRepository rayonRepository
    ) {
        this.magasinRepository = magasinRepository;
        this.produitRepository = produitRepository;
        this.customizedProductService = customizedProductService;
        this.rayonRepository = rayonRepository;
    }

    /**
     * Save a produit.
     *
     * @param produitDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public void save(ProduitDTO produitDTO) {
        log.debug("Request to save Produit : {}", produitDTO);
        try {
            customizedProductService.save(produitDTO, rayonRepository.getReferenceById(produitDTO.getRayonId()));
        } catch (Exception e) {
            log.error("Request to save Produit : {}", e);
        }
    }

    /**
     * Get all the produits.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProduitDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Produits");
        return produitRepository.findAll(pageable).map(ProduitBuilder::fromProduit);
    }

    /**
     * Get one produit by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<ProduitDTO> findOne(Long id) {
        log.debug("Request to get Produit : {}", id);
        return customizedProductService.findOneById(id);
    }

    /**
     * Delete the produit by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Long id) {
        log.debug("Request to delete Produit : {}", id);

        produitRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable) {
        log.debug("Request to get all Produits {} ", produitCriteria);
        try {
            return customizedProductService.findAll(produitCriteria, pageable);
        } catch (Exception e) {
            log.error("Request findAll  Produits : {}", e);
            return Page.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProduitDTO findOne(ProduitCriteria produitCriteria) {
        log.debug("Request to get Produit : {}", produitCriteria);
        Optional<ProduitDTO> produit = produitRepository.findById(produitCriteria.getId()).map(ProduitBuilder::fromProduit);
        ProduitDTO dto = null;
        if (produit.isPresent()) {
            dto = produit.get();
            dto.setLastDateOfSale(lastSale(produitCriteria));
            dto.setLastInventoryDate(lastInventory(produitCriteria));
            dto.setLastOrderDate(lastOrder(produitCriteria));
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime lastSale(ProduitCriteria produitCriteria) {
        return customizedProductService.lastSale(produitCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime lastInventory(ProduitCriteria produitCriteria) {
        return customizedProductService.lastInventory(produitCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDateTime lastOrder(ProduitCriteria produitCriteria) {
        return customizedProductService.lastOrder(produitCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitDTO> findWithCriteria(ProduitCriteria produitCriteria) {
        log.debug("Request to  findWithCriteria {} ", produitCriteria);
        try {
            return customizedProductService.findAll(produitCriteria);
        } catch (Exception e) {
            log.error("Request findWithCriteria  Produits : {}", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void update(ProduitDTO produitDTO) {
        log.debug("Request to update Produit : {}", produitDTO);
        try {
            customizedProductService.update(produitDTO);
        } catch (Exception e) {
            log.error("Request to update Produit : {}", e);
        }
    }



    @Override
    @Transactional(readOnly = true)
    public List<ProduitDTO> productsLiteList(ProduitCriteria produitCriteria, Pageable pageable) {
        return customizedProductService.productsLiteList(produitCriteria, pageable);
    }

    @Override
    public void updateDetail(ProduitDTO produitDTO) {
        log.debug("Request to updateDetail Produit : {}", produitDTO);
        try {
            customizedProductService.updateDetail(produitDTO);
        } catch (Exception e) {
            log.error("Request to update Produit : {}", e);
        }
    }

    @Override
    public int getProductTotalStock(Long productId) {
        return customizedProductService.produitTotalStockWithQantityUg(produitRepository.getReferenceById(productId));
    }

    @Override
    public StockProduit updateTotalStock(Produit produit, int stockIn, int stockUg) {
        return customizedProductService.updateTotalStock(produit, stockIn, stockUg);
    }

    @Override
    public void update(Produit produit) {
        produitRepository.save(produit);
    }

    @Override
    public void updateFromCommande(ProduitDTO produitDTO, Produit produit) {
        this.customizedProductService.updateFromCommande(produitDTO, produit);
    }

    private Storage getPointOfSale() {
        return magasinRepository.getReferenceById(1L).getPointOfSale();
    }
}

package com.kobe.warehouse.service.impl;


import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.service.ProduitService;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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


    public ProduitServiceImpl(MagasinRepository magasinRepository, ProduitRepository produitRepository, CustomizedProductService customizedProductService, RayonRepository rayonRepository) {
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

            customizedProductService.save(produitDTO,rayonRepository.getOne(produitDTO.getRayonId()));
        } catch (Exception e) {
            log.debug("Request to save Produit : {}", e);

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
        return produitRepository.findAll(pageable).map(ProduitDTO::new);
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
            log.debug("Request findAll  Produits : {}", e);
            return Page.empty();
        }

    }

    @Override
    @Transactional(readOnly = true)
    public ProduitDTO findOne(ProduitCriteria produitCriteria) {
        log.debug("Request to get Produit : {}", produitCriteria);
        Optional<ProduitDTO> produit = produitRepository.findById(produitCriteria.getId()).map(ProduitDTO::new);
        ProduitDTO dto = null;
        if (produit.isPresent()) {
            dto = produit.get();
            SalesLine lignesVente = lastSale(produitCriteria);
            if (lignesVente != null) {
                dto.setLastDateOfSale(lignesVente.getUpdatedAt());
            }
            StoreInventoryLine detailsInventaire = lastInventory(produitCriteria);
            if (detailsInventaire != null) {
                dto.setLastInventoryDate(detailsInventaire.getStoreInventory().getUpdatedAt());
            }
            OrderLine commandeItem = lastOrder(produitCriteria);
            if (commandeItem != null) {
                dto.setLastOrderDate(commandeItem.getUpdatedAt());
            }
        }

        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public SalesLine lastSale(ProduitCriteria produitCriteria) {
        return customizedProductService.lastSale(produitCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreInventoryLine lastInventory(ProduitCriteria produitCriteria) {
        return customizedProductService.lastInventory(produitCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderLine lastOrder(ProduitCriteria produitCriteria) {
        return customizedProductService.lastOrder(produitCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitDTO> findWithCriteria(ProduitCriteria produitCriteria) {
        log.debug("Request to  findWithCriteria {} ", produitCriteria);
        try {
            return customizedProductService.findAll(produitCriteria);
        } catch (Exception e) {
            log.debug("Request findWithCriteria  Produits : {}", e);
            return Collections.emptyList();
        }

    }

    @Override
    public void update(ProduitDTO produitDTO) {
        log.debug("Request to update Produit : {}", produitDTO);
        try {
            customizedProductService.update(produitDTO);
        } catch (Exception e) {
            log.debug("Request to update Produit : {}", e);

        }

    }

    @Override
    public List<ProduitDTO> lite(String query) {
        return null;
    }

    private Storage getPointOfSale() {
        return magasinRepository.getOne(1l).getPointOfSale();
    }

    @Override
    public void updateDetail(ProduitDTO produitDTO) {
        log.debug("Request to updateDetail Produit : {}", produitDTO);
        try {
            customizedProductService.updateDetail(produitDTO);
        } catch (Exception e) {
            log.debug("Request to update Produit : {}", e);

        }
    }
}

package com.kobe.warehouse.service.stock.impl;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.builder.ProduitBuilder;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;

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

    private static final Logger LOG = LoggerFactory.getLogger(ProduitServiceImpl.class);
    private final ProduitRepository produitRepository;
    private final CustomizedProductService customizedProductService;
    private final RayonRepository rayonRepository;
    private final JsonMapper objectMapper;
    private final AppConfigurationService appConfigurationService;

    public ProduitServiceImpl(
        ProduitRepository produitRepository,
        CustomizedProductService customizedProductService,
        RayonRepository rayonRepository,
        JsonMapper objectMapper,
        AppConfigurationService appConfigurationService
    ) {

        this.produitRepository = produitRepository;
        this.customizedProductService = customizedProductService;
        this.rayonRepository = rayonRepository;
        this.objectMapper = objectMapper;
        this.appConfigurationService = appConfigurationService;
    }

    /**
     * Save a produit.
     *
     * @param produitDTO the entity to save.
     * @return the persisted entity.
     */
    @Override
    public void save(ProduitDTO produitDTO) {
        LOG.debug("Request to save Produit : {}", produitDTO);
        try {
            customizedProductService.save(produitDTO, rayonRepository.getReferenceById(produitDTO.getRayonId()));
        } catch (Exception e) {
            LOG.error("Request to save Produit : {}", e);
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
        LOG.debug("Request to get all Produits");
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
    public Optional<ProduitDTO> findOne(Integer id) {
        LOG.debug("Request to get Produit : {}", id);
        return customizedProductService.findOneById(id);
    }

    /**
     * Delete the produit by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Integer id) {
        LOG.debug("Request to delete Produit : {}", id);

        produitRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProduitDTO> findAll(ProduitCriteria produitCriteria, Pageable pageable) {
        LOG.debug("Request to get all Produits  ", produitCriteria);
        try {
            return customizedProductService.findAll(produitCriteria, pageable);
        } catch (Exception e) {
            LOG.error("Request findAll  Produits : ", e);
            return Page.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProduitDTO findOne(ProduitCriteria produitCriteria) {
        LOG.debug("Request to get Produit : {}", produitCriteria);
        Optional<ProduitDTO> produit = produitRepository.findById(produitCriteria.getId()).map(ProduitBuilder::fromProduit);
        ProduitDTO dto = null;
        if (produit.isPresent()) {
            dto = produit.get();
            dto.setLastDateOfSale(lastSale(produitCriteria));
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
    public LocalDateTime lastOrder(ProduitCriteria produitCriteria) {
        return customizedProductService.lastOrder(produitCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitDTO> findWithCriteria(ProduitCriteria produitCriteria) {
        LOG.debug("Request to  findWithCriteria {} ", produitCriteria);
        try {
            return customizedProductService.findAll(produitCriteria);
        } catch (Exception e) {
            LOG.error("Request findWithCriteria  Produits : {}", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void update(ProduitDTO produitDTO) {
        LOG.debug("Request to update Produit : {}", produitDTO);
        try {
            customizedProductService.update(produitDTO);
        } catch (Exception e) {
            LOG.error("Request to update Produit : {}", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitDTO> productsLiteList(ProduitCriteria produitCriteria, Pageable pageable) {
        return customizedProductService.productsLiteList(produitCriteria, pageable);
    }

    @Override
    public void updateDetail(ProduitDTO produitDTO) {
        LOG.debug("Request to updateDetail Produit : {}", produitDTO);
        try {
            customizedProductService.updateDetail(produitDTO);
        } catch (Exception e) {
            LOG.error("Request to update Produit : {}", e);
        }
    }

    @Override
    public int getProductTotalStock(Integer productId) {
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

    @Override
    public Produit findReferenceById(Integer id) {
        return produitRepository.getReferenceById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitSearch> searchProducts(String search, Integer magasinId, Pageable pageable) {
        if (isNull(magasinId)) {
            magasinId = appConfigurationService.getMagasin().getId();
        }

        String jsonResult = produitRepository.searchProduitsJson(search, magasinId, pageable.getPageSize());

        try {
            return objectMapper.readValue(jsonResult, new TypeReference<>() {});
        } catch (Exception e) {
            LOG.error(null, e);
            return List.of();
        }
    }
}

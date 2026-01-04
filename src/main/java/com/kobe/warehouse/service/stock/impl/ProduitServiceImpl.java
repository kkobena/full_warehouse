package com.kobe.warehouse.service.stock.impl;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.repository.CustomizedProductService;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RayonProduitRepository;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.ProduitCriteria;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.dto.builder.ProduitBuilder;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.reassort.SuggestionReassortService;
import com.kobe.warehouse.service.stock.ProduitService;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final LogsService logsService;
    private final StockProduitRepository stockProduitRepository;
    private final RayonProduitRepository rayonProduitRepository;
    private final SuggestionReassortService suggestionReassortService;

    public ProduitServiceImpl(
        ProduitRepository produitRepository,
        CustomizedProductService customizedProductService,
        RayonRepository rayonRepository,
        ObjectMapper objectMapper, StorageService storageService, LogsService logsService, StockProduitRepository stockProduitRepository, RayonProduitRepository rayonProduitRepository, SuggestionReassortService suggestionReassortService
    ) {

        this.produitRepository = produitRepository;
        this.customizedProductService = customizedProductService;
        this.rayonRepository = rayonRepository;
        this.objectMapper = objectMapper;
        this.storageService = storageService;
        this.logsService = logsService;
        this.stockProduitRepository = stockProduitRepository;
        this.rayonProduitRepository = rayonProduitRepository;
        this.suggestionReassortService = suggestionReassortService;
    }

    /**
     * Save a produit.
     *
     * @param produitDTO the entity to save.
     */
    @Override
    public void save(ProduitDTO produitDTO) {
        LOG.debug("Request to save Produit : {}", produitDTO);
        if (nonNull(produitDTO.getTypeProduit()) && produitDTO.getTypeProduit() == TypeProduit.DETAIL) {
            saveDetail(produitDTO);
        } else {
            Storage reserveStorage = storageService.getDefaultConnectedUserReserveStorage();
            Produit produit = ProduitBuilder.fromDTO(produitDTO, rayonRepository.getReferenceById(produitDTO.getRayonId()), reserveStorage);
            save(produit);
        }

    }


    private void save(Produit produit) {
        produit = produitRepository.save(produit);
        stockProduitRepository.saveAll(produit.getStockProduits());
        logsService.create(
            TransactionType.CREATE_PRODUCT,
            String.format("Création du produit %s", produit.getLibelle()),
            produit.getId().toString()
        );
    }

    @Override
    public void saveDetail(ProduitDTO dto) {
        Produit parentProduit = produitRepository.getReferenceById(dto.getProduitId());
        Produit produit = ProduitBuilder.buildDetailFromDTO(dto, parentProduit);
        updateProduitItemQty(parentProduit, dto);
        save(produit);
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
        Magasin magasin = storageService.getConnectedUserMagasin();
        Storage userStorage = storageService.getDefaultConnectedUserMainStorage();
        return findProduitById(id)
            .map(p ->
                ProduitBuilder.buildFromProduit(
                    p,
                    magasin,
                    p.getStockProduits().stream().filter(s -> s.getStorage().equals(userStorage)).findFirst().orElse(null)
                )
            );
    }

    /**
     * Delete the produit by id.
     *
     * @param id the id of the entity.
     */
    @Override
    public void delete(Integer id) {
        LOG.debug("Request to delete Produit : {}", id);

        deleteProduit(id);
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
        Optional<ProduitDTO> produit = findProduitById(produitCriteria.getId()).map(ProduitBuilder::fromProduit);
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

        Storage reserveStorage = storageService.getDefaultConnectedUserReserveStorage();
        Produit produitToUpdate = produitRepository.getReferenceById(produitDTO.getId());
        Set<RayonProduit> rayonProduits = rayonProduitRepository.findAllByProduitId(produitToUpdate.getId());
        boolean mustUpdateRayon = false;
        if (rayonProduits.isEmpty()) {
            mustUpdateRayon = true;
        } else {
            Optional<RayonProduit> optionalRayonProduit = rayonProduits
                .stream()
                .filter(rayonProduit ->
                    rayonProduit.getRayon().getStorage().getStorageType().name().equalsIgnoreCase(StorageType.PRINCIPAL.getValue())
                )
                .findFirst();
            if (optionalRayonProduit.isPresent()) {
                RayonProduit rayonProduit = optionalRayonProduit.get();
                if (!rayonProduit.getRayon().getId().equals(produitDTO.getRayonId())) {
                    mustUpdateRayon = true;
                }
            }
        }
        if (mustUpdateRayon) {
            updateProduitDetails(produitToUpdate.getProduits(), produitDTO);
        }
        Produit produit = ProduitBuilder.buildProduitFromProduitDTO(produitDTO, produitToUpdate);
        buildRayonProduits(produit, produitDTO, rayonProduits);
        updateStockInfo(reserveStorage, produit, produitDTO);
        updateProduit(produit);
        logsService.create(
            TransactionType.UPDATE_PRODUCT,
            String.format("Modification du produit %s", produit.getLibelle()),
            produit.getId().toString()
        );

    }

    @Override
    public void save(StockProduitDTO dto) {
        StockProduit stockProduit = buildStockProduitFromStockProduitDTO(dto);
        Produit produit = findProduitById(dto.getProduitId()).orElseThrow(() -> new GenericError("Produit not found with id " + dto.getProduitId()));
        stockProduit.setProduit(produit);
        stockProduitRepository.save(stockProduit);

    }
    @Override
    @Transactional(readOnly = true)
    public Optional<FournisseurProduit> getFournisseurProduitByCriteria(String criteteria, Integer fournisseurId){
        return customizedProductService.getFournisseurProduitByCriteria(criteteria, fournisseurId);
    }

    @Override
    public List<Produit> find(ProduitCriteria produitCriteria) {
        return customizedProductService.find(produitCriteria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitDTO> productsLiteList(ProduitCriteria produitCriteria, Pageable pageable) {
        return customizedProductService.productsLiteList(produitCriteria, pageable);
    }
    @Override
    public int produitTotalStock(Produit produit) {
        return produit.getStockProduits().stream().map(StockProduit::getQtyStock).reduce(0, Integer::sum);
    }

    @Override
    public void updateDetail(ProduitDTO produitDTO) {
        LOG.debug("Request to updateDetail Produit : {}", produitDTO);

        Produit produit = buildDetailProduitFromProduitDTO(produitDTO, produitRepository.getReferenceById(produitDTO.getId()));
        updateProduitItemQty(produit.getParent(), produitDTO);
        produitRepository.save(produit);
        logsService.create(
            TransactionType.UPDATE_PRODUCT,
            String.format("Modification du produit %s", produit.getLibelle()),
            produit.getId().toString()
        );

    }

    @Override
    public int getProductTotalStock(Integer productId) {
        return findReferenceById(productId).getStockProduits().stream().map(StockProduit::getTotalStockQuantity).reduce(0, Integer::sum);
    }

    @Override
    public List<Produit> findByIds(Set<Integer> ids) {
        return this.produitRepository.findAllById(ids);
    }

    @Override
    public StockProduit updateTotalStock(Produit produit, int stockIn, int stockUg) {
        Magasin magasin = storageService.getConnectedUserMagasin();
        Set<StockProduit> stockProduits = produit.getStockProduits();
        StockProduit stockProduit = null;
        StockProduit stockReserve = null;
        if (stockProduits.size() == 1) {
            stockProduit = stockProduits.iterator().next();

        } else {
            for (StockProduit sp : stockProduits) {
                Storage storage = sp.getStorage();
                StorageType storageType = storage.getStorageType();
                if (storage.getMagasin().getId().equals(magasin.getId()) && storageType == StorageType.PRINCIPAL) {
                    stockProduit = sp;
                }
                if (storage.getMagasin().getId().equals(magasin.getId()) && storageType == StorageType.SAFETY_STOCK) {
                    stockReserve = sp;

                }
            }

        }
        assert stockProduit != null;
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduit.setQtyStock(stockProduit.getQtyStock() + (stockIn + stockUg));
        stockProduit.setQtyUG(stockProduit.getQtyUG() + stockUg);
        stockProduit.setQtyVirtual(stockProduit.getQtyStock());
        stockProduit = stockProduitRepository.save(stockProduit);
        if (nonNull(stockReserve)) {
            suggestionReassortService.createSuggestionReassort(stockReserve);
        }
        return stockProduit;
    }
    @Override
    public void update(Produit produit) {
        updateProduit( produit);
    }


    @Override
    public void updateFromCommande(ProduitDTO dto, Produit produit) {
        produit.setTva(tvaFromId(dto.getTvaId()));
        if (StringUtils.hasLength(dto.getCodeEanLaboratoire())) {
            produit.setCodeEanLaboratoire(dto.getCodeEanLaboratoire());
        }

        produit.setUpdatedAt(LocalDateTime.now());
        Set<RayonProduit> rayonProduits = rayonProduitRepository.findAllByProduitId(produit.getId());
        buildRayonProduits(produit, dto, rayonProduits);
        updateProduit(produit);
        logsService.create(
            TransactionType.UPDATE_PRODUCT,
            String.format("Modification du produit %s", produit.getLibelle()),
            produit.getId().toString()
        );
    }
    @Override
    public Produit findReferenceById(Integer id) {
        return produitRepository.getReferenceById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitSearch> searchProducts(String search, Integer magasinId, Pageable pageable) {
        if (isNull(magasinId)) {
            magasinId = storageService.getConnectedUserMagasin().getId();
        }

        String jsonResult = produitRepository.searchProduitsJson(search, magasinId, pageable.getPageSize());

        try {
            return objectMapper.readValue(jsonResult, new TypeReference<>() {
            });
        } catch (Exception e) {
            LOG.error(null, e);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProduitSearch> searchProductsByStorage(@NotNull Integer storageId, String search, Pageable pageable) {
        if (isNull(storageId)) {
            return List.of();
        }

        String jsonResult = produitRepository.searchProductsByStorage(search, storageId, pageable.getPageSize());

        try {
            return objectMapper.readValue(jsonResult, new TypeReference<>() {
            });
        } catch (Exception e) {
            LOG.error(null, e);
            return List.of();
        }
    }


    private void updateProduitItemQty(Produit produitParent, ProduitDTO dto) {
        if (nonNull(dto.getItemQty()) && dto.getItemQty() > 0 && !Objects.equals(dto.getItemQty(), produitParent.getItemQty())) {
            produitParent.setItemQty(dto.getItemQty());
            produitParent.setUpdatedAt(LocalDateTime.now());
            updateProduit(produitParent);
        }
    }

    private Produit buildDetailProduitFromProduitDTO(ProduitDTO produitDTO, Produit produit) {
        produit.setUpdatedAt(LocalDateTime.now());
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setNetUnitPrice(produitDTO.getRegularUnitPrice());
        produit.setCostAmount(produitDTO.getCostAmount());
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        return produit;
    }

    @Cacheable(cacheNames = "produits", key = "#id")
    public Optional<Produit> findProduitById(Integer id) {
        return produitRepository.findById(id);
    }


    @CachePut(cacheNames = "produits", key = "#produit.id")
    public Produit updateProduit(Produit produit) {
        return produitRepository.save(produit);
    }


    @CacheEvict(cacheNames = "produits", key = "#id")
    public void deleteProduit(Integer id) {
        produitRepository.deleteById(id);
    }

    private StockProduit buildStockProduitFromStockProduitDTO(StockProduitDTO dto, StockProduit stockProduit) {
        stockProduit.setQtyStock(dto.getQtyStock());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduit.setQtyVirtual(dto.getQtyVirtual());
        stockProduit.setQtyUG(dto.getQtyUG());
        stockProduit.setStorage(storageFromId(dto.getStorageId()));
        return stockProduit;
    }

    private Storage storageFromId(Integer id) {
        if (id == null) {
            return null;
        }
        Storage storage = new Storage();
        storage.setId(id);
        return storage;
    }

    private Rayon rayonFromId(Integer id) {
        if (id == null) {
            return null;
        }
        Rayon rayon = new Rayon();
        rayon.setId(id);
        return rayon;
    }

    private Tva tvaFromId(Integer id) {
        return ProduitBuilder.tvaFromId(id);
    }

    private StockProduit buildStockProduitFromStockProduitDTO(StockProduitDTO dto) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setCreatedAt(LocalDateTime.now());
        stockProduit.setQtyStock(dto.getQtyStock());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduit.setQtyVirtual(dto.getQtyVirtual());
        stockProduit.setQtyUG(dto.getQtyUG());
        stockProduit.setStorage(storageFromId(dto.getStorageId()));
        return stockProduit;
    }

    private void updateProduitDetails(List<Produit> produits, ProduitDTO produitDTO) {
        for (Produit p : produits) {
            Set<RayonProduit> rayonProduits = rayonProduitRepository.findAllByProduitId(p.getId());
            buildRayonProduits(p, produitDTO, rayonProduits);
            produitRepository.save(p);
        }
    }

    private void buildRayonProduits(Produit produitToUpdate, ProduitDTO
        produitDTO, Set<RayonProduit> rayonProduits) {
        Set<RayonProduit> newSet = new HashSet<>();
        if (!rayonProduits.isEmpty()) {
            for (RayonProduit r : rayonProduits) {
                if (r.getRayon().getStorage().getStorageType() == StorageType.PRINCIPAL) {
                    newSet.add(r.setRayon(rayonFromId(produitDTO.getRayonId())));
                } else {
                    newSet.add(r);
                }
            }
        } else {
            newSet.add(new RayonProduit().setProduit(produitToUpdate).setRayon(rayonFromId(produitDTO.getRayonId())));
        }
        produitToUpdate.setRayonProduits(newSet);
    }

    private void updateStockInfo(Storage reserveStorage, Produit produit, ProduitDTO dto) {
        boolean hasStockInfo = dto.getStockReassort() != null || dto.getQtySeuilMini() != null || dto.getSeuilMini() != null;
        if (!hasStockInfo) {
            return;
        }
        boolean hasReserveStorage = false;
        for (StockProduit sp : produit.getStockProduits()) {
            if (sp.getStorage().getStorageType() == StorageType.PRINCIPAL) {
                sp.setStockReassort(dto.getStockReassort());
                sp.setSeuilMini(dto.getQtySeuilMini());
                stockProduitRepository.save(sp);
            } else {
                hasReserveStorage = true;
                sp.setSeuilMini(dto.getSeuilMini());
                stockProduitRepository.save(sp);
            }

            if (!hasReserveStorage && nonNull(dto.getSeuilMini())) {
                createReserve(reserveStorage, produit, dto);
            }
        }
    }

    private void createReserve(Storage storage, Produit produit, ProduitDTO produitDTO) {
        if (isNull(produitDTO.getSeuilMini())) {
            return;
        }
        StockProduit stockProduit = ProduitBuilder.createReserve(storage, produitDTO);
        stockProduit.setProduit(produit);
        produit.getStockProduits().add(stockProduit);
        stockProduitRepository.save(stockProduit);
    }
}

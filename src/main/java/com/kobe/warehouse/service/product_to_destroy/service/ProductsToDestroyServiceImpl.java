package com.kobe.warehouse.service.product_to_destroy.service;

import static com.kobe.warehouse.service.utils.ServiceUtil.buildPeremptionStatut;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.hasText;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.ProductsToDestroy;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.ProductsToDestroyRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.records.Keys;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyFilter;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyPayload;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroySumDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductsToDestroyPayload;
import com.kobe.warehouse.service.utils.DateUtil;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional(readOnly = true)
public class ProductsToDestroyServiceImpl implements ProductsToDestroyService {

    private final ProductsToDestroyRepository productsToDestroyRepository;
    private final LotRepository lotRepository;
    private final FournisseurProduitRepository fournisseurRepository;
    private final UserService userService;
    private final StorageService storageService;
    private final MagasinRepository magasinRepository;
    private final StockProduitRepository stockProduitRepository;
    private final ProduitRepository produitRepository;

    public ProductsToDestroyServiceImpl(
        ProductsToDestroyRepository productsToDestroyRepository,
        LotRepository lotRepository,
        FournisseurProduitRepository fournisseurRepository,
        UserService userService,
        StorageService storageService,
        MagasinRepository magasinRepository,
        StockProduitRepository stockProduitRepository,
        ProduitRepository produitRepository
    ) {
        this.productsToDestroyRepository = productsToDestroyRepository;
        this.lotRepository = lotRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.userService = userService;
        this.storageService = storageService;
        this.magasinRepository = magasinRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.produitRepository = produitRepository;
    }

    @Override
    @Transactional
    public void addLotQuantities(ProductsToDestroyPayload productToDestroyPayload) {
        Magasin magasin;
        if (isNull(productToDestroyPayload.magasinId())) {
            magasin = storageService.getConnectedUserMagasin();
        } else {
            magasin = magasinRepository.getReferenceById(productToDestroyPayload.magasinId());
        }
        for (ProductToDestroyPayload productToDestroyPayload1 : productToDestroyPayload.products()) {
            ProductsToDestroy productsToDestroy = addProductQuantity(productToDestroyPayload1, null, magasin);
            updateStock(productsToDestroy);
            updateLot(productsToDestroy);
        }
    }

    private ProductsToDestroy addProductQuantity(ProductToDestroyPayload productToDestroyPayload, Lot lot, Magasin magasin) {
        FournisseurProduit fournisseurProduit = nonNull(lot) ? lot.getOrderLine().getFournisseurProduit() : null;
        // Lot lot = null;
        if (isNull(lot) && nonNull(productToDestroyPayload.lotId())) {
            lot = this.lotRepository.findById(productToDestroyPayload.lotId()).orElse(null);
            if (nonNull(lot)) {
                fournisseurProduit = lot.getOrderLine().getFournisseurProduit();
            }
        } else {
            if (isNull(fournisseurProduit) && nonNull(productToDestroyPayload.fournisseurId())) {
                fournisseurProduit = this.fournisseurRepository.findOneByProduitIdAndFournisseurId(
                        productToDestroyPayload.produitId(),
                        productToDestroyPayload.fournisseurId()
                    ).orElse(null);
            }
        }
        if (isNull(fournisseurProduit)) {
            Produit produit = this.produitRepository.getReferenceById(productToDestroyPayload.produitId());
            fournisseurProduit = produit.getFournisseurProduitPrincipal();
            if (isNull(fournisseurProduit)) {
                throw new GenericError("Le produit n'est pas rattaché au fournisseur ");
            }
        }
        ProductsToDestroy productsToDestroy = new ProductsToDestroy();
        productsToDestroy.setCreated(LocalDateTime.now());
        productsToDestroy.setUpdated(productsToDestroy.getCreated());
        productsToDestroy.setDestroyed(false);
        productsToDestroy.setUser(userService.getUser());
        productsToDestroy.setFournisseurProduit(fournisseurProduit);
        productsToDestroy.setQuantity(productToDestroyPayload.quantity());

        if (nonNull(lot)) {
            productsToDestroy.setPrixAchat(lot.getPrixAchat());
            productsToDestroy.setPrixUnit(lot.getPrixUnit());
            productsToDestroy.setNumLot(lot.getNumLot());
            productsToDestroy.setDatePeremption(lot.getExpiryDate());
        } else {
            Produit product = fournisseurProduit.getProduit();
            productsToDestroy.setDatePeremption(product.getPerimeAt());
            productsToDestroy.setPrixAchat(fournisseurProduit.getPrixAchat());
            productsToDestroy.setPrixUnit(fournisseurProduit.getPrixUni());
            //a revoir
            if (!productToDestroyPayload.editing()) {
                product.setPerimeAt(null);
                this.produitRepository.save(product);
            }
        }
        productsToDestroy.setMagasin(magasin);

        return this.productsToDestroyRepository.save(productsToDestroy);
    }

    @Override
    public Page<ProductToDestroyDTO> findAll(ProductToDestroyFilter produidToDestroyFilter, Pageable pageable) {
        Sort sort = Sort.by(Direction.DESC, "created");
        if (pageable.isPaged()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        }
        return this.productsToDestroyRepository.findAll(
                this.productsToDestroyRepository.buildCombinedSpecification(produidToDestroyFilter),
                pageable
            ).map(productToDestroy -> {
                ProductToDestroyDTO productToDestroyDTO = new ProductToDestroyDTO();
                productToDestroyDTO.setId(productToDestroy.getId());
                productToDestroyDTO.setNumLot(productToDestroy.getNumLot());
                productToDestroyDTO.setPrixAchat(productToDestroy.getPrixAchat());
                productToDestroyDTO.setPrixUni(productToDestroy.getPrixUnit());
                FournisseurProduit fournisseurProduit = productToDestroy.getFournisseurProduit();
                productToDestroyDTO.setProduitCodeCip(fournisseurProduit.getCodeCip());
                productToDestroyDTO.setProduitName(fournisseurProduit.getProduit().getLibelle());
                productToDestroyDTO.setQuantity(productToDestroy.getQuantity());
                productToDestroyDTO.setDatePeremption(DateUtil.format(productToDestroy.getDatePeremption()));
                productToDestroyDTO.setDateDestruction(DateUtil.format(productToDestroy.getDateDestuction()));
                productToDestroyDTO.setFournisseur(fournisseurProduit.getFournisseur().getLibelle());
                productToDestroyDTO.setUpdatedDate(DateUtil.format(productToDestroy.getUpdated()));
                productToDestroyDTO.setCreatedDate(DateUtil.format(productToDestroy.getCreated()));
                productToDestroyDTO.setPeremptionStatut(buildPeremptionStatut(productToDestroy.getDatePeremption()));
                User user = productToDestroy.getUser();
                productToDestroyDTO.setUser(user.getFirstName().charAt(0) + ".".toUpperCase() + user.getLastName());
                return productToDestroyDTO;
            });
    }

    @Override
    public ProductToDestroySumDTO getSum(ProductToDestroyFilter produidToDestroyFilter) {
        return this.productsToDestroyRepository.getSum(this.productsToDestroyRepository.buildCombinedSpecification(produidToDestroyFilter)); // todo to implement
    }

    @Override
    @Transactional
    public void destroy(Keys keys) {
        List<ProductsToDestroy> productsToDestroys = this.productsToDestroyRepository.findAllById(keys.ids());
        for (ProductsToDestroy productsToDestroy : productsToDestroys) {
            productsToDestroy.setDestroyed(true);
            productsToDestroy.setDateDestuction(LocalDate.now());
            productsToDestroy.setUpdated(LocalDateTime.now());
            productsToDestroy.setUser(this.userService.getUser());
            this.productsToDestroyRepository.save(productsToDestroy);
        }
    }

    @Override
    @Transactional
    public void addProductQuantity(ProductToDestroyPayload productToDestroyPayload) {
        Lot lot = this.lotRepository.findByNumLot(productToDestroyPayload.numLot()).orElse(null);
        prevaliderLot(lot, productToDestroyPayload.quantity());
        Magasin magasin;
        if (isNull(productToDestroyPayload.magasinId())) {
            magasin = storageService.getConnectedUserMagasin();
        } else {
            magasin = magasinRepository.getReferenceById(productToDestroyPayload.magasinId());
        }
        addProductQuantity(productToDestroyPayload, lot, magasin);
    }

    @Override
    public void closeLastEdition() {
        this.productsToDestroyRepository.findAllByEditingTrueAndCreatedEquals(true, LocalDate.now()).forEach(productsToDestroy -> {
                updateStock(productsToDestroy);
                productsToDestroy.setEditing(false);
                Produit produit = productsToDestroy.getFournisseurProduit().getProduit();
                produit.setPerimeAt(null);
                this.produitRepository.save(produit);
            });
    }

    @Override
    @Transactional
    public void modifyProductQuantity(ProductToDestroyPayload productToDestroyPayload) {
        ProductsToDestroy productsToDestroy = this.productsToDestroyRepository.getReferenceById(productToDestroyPayload.id());
        Lot lot = this.lotRepository.findByNumLot(productToDestroyPayload.numLot()).orElse(null);
        prevaliderLot(lot, productToDestroyPayload.quantity());
        productsToDestroy.setQuantity(productToDestroyPayload.quantity());
        productsToDestroy.setUpdated(LocalDateTime.now());
        this.productsToDestroyRepository.save(productsToDestroy);
    }

    private void updateStock(ProductsToDestroy productsToDestroy) {
        int quantity = productsToDestroy.getQuantity();
        List<StockProduit> stockProduits =
            this.stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(
                    productsToDestroy.getMagasin().getId(),
                    productsToDestroy.getFournisseurProduit().getId()
                );
        if (!stockProduits.isEmpty()) {
            Map<Boolean, List<StockProduit>> partiton = stockProduits
                .stream()
                .collect(Collectors.partitioningBy(s -> s.getStorage().getStorageType() == StorageType.POINT_DE_VENTE));
            List<StockProduit> stockOfPointOfSale = partiton.get(true);
            if (!CollectionUtils.isEmpty(stockOfPointOfSale)) {
                StockProduit stockProduit = stockOfPointOfSale.getFirst();
                if (stockProduit.getQtyStock() >= quantity) {
                    stockProduit.setQtyStock(stockProduit.getQtyStock() - quantity);
                    quantity -= stockProduit.getQtyStock();
                } else {
                    if (stockProduit.getQtyStock() > 0) {
                        stockProduit.setQtyStock(0);
                        quantity -= stockProduit.getQtyStock();
                    }
                }
                stockProduit.setUpdatedAt(LocalDateTime.now());
                this.stockProduitRepository.save(stockProduit);
            }
            if (quantity > 0) {
                List<StockProduit> others = partiton.get(false);
                if (!CollectionUtils.isEmpty(others)) {
                    for (StockProduit stockProduit : others) {
                        if (quantity <= 0) {
                            return;
                        }
                        if (stockProduit.getQtyStock() >= quantity) {
                            stockProduit.setQtyStock(stockProduit.getQtyStock() - quantity);
                            quantity -= stockProduit.getQtyStock();
                        } else {
                            if (stockProduit.getQtyStock() > 0) {
                                stockProduit.setQtyStock(0);
                                quantity -= stockProduit.getQtyStock();
                            }
                        }
                        stockProduit.setUpdatedAt(LocalDateTime.now());
                        this.stockProduitRepository.save(stockProduit);
                    }
                }
            }
        }
    }

    private void updateLot(ProductsToDestroy productsToDestroy) {
        if (hasText(productsToDestroy.getNumLot())) {
            this.lotRepository.findByNumLot(productsToDestroy.getNumLot()).ifPresent(lot -> {
                    lot.setQuantity(lot.getQuantity() - productsToDestroy.getQuantity());
                    lot.setUpdated(LocalDateTime.now());
                    this.lotRepository.save(lot);
                });
        }
    }

    private void prevaliderLot(Lot lot, int quantity) {
        if (nonNull(lot)) {
            if (lot.getQuantity() <= 0) {
                throw new GenericError("Le lot n'a plus de stock en machine");
            }
            if (lot.getExpiryDate().isAfter(LocalDate.now().plusMonths(6))) {
                throw new GenericError("Le lot expire dans plus de 6 mois");
            }
            if (lot.getQuantity() < quantity) {
                throw new GenericError("La quantité saisie est supérieure à la quantité disponible ");
            }
        }
    }
}

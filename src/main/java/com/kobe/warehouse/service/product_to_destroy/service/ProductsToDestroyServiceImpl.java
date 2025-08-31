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
import com.kobe.warehouse.domain.AppUser;
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
import com.kobe.warehouse.service.excel.ExcelExportUtil;
import com.kobe.warehouse.service.excel.model.ExportFormat;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyFilter;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroyPayload;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroySumDTO;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductsToDestroyPayload;
import com.kobe.warehouse.service.utils.DateUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
import org.springframework.http.ResponseEntity;
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
    private final InventoryTransactionService inventoryTransactionService;
    private final ProductToDestroyReportService productToDestroyReportService;

    public ProductsToDestroyServiceImpl(
        ProductsToDestroyRepository productsToDestroyRepository,
        LotRepository lotRepository,
        FournisseurProduitRepository fournisseurRepository,
        UserService userService,
        StorageService storageService,
        MagasinRepository magasinRepository,
        StockProduitRepository stockProduitRepository,
        ProduitRepository produitRepository,
        InventoryTransactionService inventoryTransactionService,
        ProductToDestroyReportService productToDestroyReportService
    ) {
        this.productsToDestroyRepository = productsToDestroyRepository;
        this.lotRepository = lotRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.userService = userService;
        this.storageService = storageService;
        this.magasinRepository = magasinRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.produitRepository = produitRepository;
        this.inventoryTransactionService = inventoryTransactionService;
        this.productToDestroyReportService = productToDestroyReportService;
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
        productsToDestroy.setStockInitial(productToDestroyPayload.stockInitial());
        productsToDestroy.setEditing(productToDestroyPayload.editing());
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
                List<StockProduit> stockProduits = findStockProduits(
                    magasin.getId(),
                    productsToDestroy.getFournisseurProduit().getProduit().getId()
                );
                productsToDestroy.setStockInitial(stockProduits.stream().mapToInt(StockProduit::getQtyStock).sum());
                /* product.setPerimeAt(null);
                this.produitRepository.save(product);*/
            } else {
                productsToDestroy.setDatePeremption(productToDestroyPayload.datePeremption());
                productsToDestroy.setNumLot(productToDestroyPayload.numLot());
                productsToDestroy.setStockInitial(productToDestroyPayload.stockInitial());
            }
        }
        productsToDestroy.setMagasin(magasin);

        return this.productsToDestroyRepository.save(productsToDestroy);
    }

    @Override
    public Page<ProductToDestroyDTO> findAll(ProductToDestroyFilter produidToDestroyFilter, Pageable pageable) {
        Sort sort = Sort.by(Direction.DESC, "created");
        if (nonNull(pageable) && pageable.isPaged()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        } else {
            pageable = Pageable.unpaged(sort);
        }
        return this.productsToDestroyRepository.findAll(
                this.productsToDestroyRepository.buildCombinedSpecification(produidToDestroyFilter),
                pageable
            ).map(productToDestroy -> {
                ProductToDestroyDTO productToDestroyDTO = buildProductToDestroy(productToDestroy);
                AppUser user = productToDestroy.getUser();
                productToDestroyDTO.setUser(user.getFirstName().charAt(0) + ".".toUpperCase() + user.getLastName());
                return productToDestroyDTO;
            });
    }

    @Override
    public ProductToDestroySumDTO getSum(ProductToDestroyFilter produidToDestroyFilter) {
        return this.productsToDestroyRepository.getSum(this.productsToDestroyRepository.buildCombinedSpecification(produidToDestroyFilter));
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
    public void remove(Keys keys) {
        this.productsToDestroyRepository.deleteAllById(keys.ids());
    }

    @Override
    @Transactional
    public void addProductQuantity(ProductToDestroyPayload productToDestroyPayload) {
        Lot lot = this.lotRepository.findByNumLot(productToDestroyPayload.numLot()).orElse(null);
        this.productsToDestroyRepository.findByNumLotAndFournisseurProduitProduitId(
                productToDestroyPayload.numLot(),
                productToDestroyPayload.produitId()
            ).ifPresentOrElse(
                productsToDestroy -> {
                    update(productsToDestroy, productToDestroyPayload, lot);
                },
                () -> {
                    prevaliderLot(lot, productToDestroyPayload.quantity());
                    Magasin magasin;
                    if (isNull(productToDestroyPayload.magasinId())) {
                        magasin = storageService.getConnectedUserMagasin();
                    } else {
                        magasin = magasinRepository.getReferenceById(productToDestroyPayload.magasinId());
                    }
                    addProductQuantity(productToDestroyPayload, lot, magasin);
                }
            );
    }

    private void update(ProductsToDestroy productsToDestroy, ProductToDestroyPayload productToDestroyPayload, Lot lot) {
        prevaliderLot(lot, productToDestroyPayload.quantity() + productsToDestroy.getQuantity());
        productsToDestroy.setQuantity(productToDestroyPayload.quantity() + productsToDestroy.getQuantity());
        productsToDestroy.setUpdated(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void closeLastEdition() {
        this.productsToDestroyRepository.findAllByEditingTrueAndCreatedEquals(LocalDate.now(), this.userService.getUser().getId()).forEach(
                productsToDestroy -> {
                    updateStock(productsToDestroy);
                    productsToDestroy.setEditing(false);
                    productsToDestroy.setUpdated(LocalDateTime.now());
                    inventoryTransactionService.save(productsToDestroy);
                    this.productsToDestroyRepository.save(productsToDestroy);
                }
            );
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

    @Override
    public Page<ProductToDestroyDTO> findEditing(ProductToDestroyFilter produidToDestroyFilter, Pageable pageable) {
        Sort sort = Sort.by(Direction.DESC, "updated");
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return this.productsToDestroyRepository.findAll(
                this.productsToDestroyRepository.buildEditing(this.userService.getUser().getId(), produidToDestroyFilter.searchTerm()),
                pageable
            ).map(this::buildProductToDestroy);
    }

    @Override
    public void export(HttpServletResponse response, ExportFormat type, ProductToDestroyFilter produidToDestroyFilter) throws IOException {
        ExcelExportUtil.writeToResponse(
            response,
            type,
            "produits_a_detruire",
            "Liste des produits à détruire",
            ProductToDestroyDTO.class,
            this.findAll(produidToDestroyFilter, Pageable.unpaged()).getContent()
        );
    }

    @Override
    public ResponseEntity<byte[]> generatePdf(ProductToDestroyFilter produidToDestroyFilter) {
        return this.productToDestroyReportService.generatePdf(
                this.findAll(produidToDestroyFilter, Pageable.unpaged()).getContent(),
                this.getSum(produidToDestroyFilter),
                produidToDestroyFilter.fromDate(),
                produidToDestroyFilter.toDate()
            );
    }

    private List<StockProduit> findStockProduits(Long magasinId, Long produitId) {
        return this.stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(magasinId, produitId);
    }

    private void updateStock(ProductsToDestroy productsToDestroy) {
        int quantity = productsToDestroy.getQuantity();
        List<StockProduit> stockProduits = findStockProduits(
            productsToDestroy.getMagasin().getId(),
            productsToDestroy.getFournisseurProduit().getProduit().getId()
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

    private ProductToDestroyDTO buildProductToDestroy(ProductsToDestroy productToDestroy) {
        ProductToDestroyDTO productToDestroyDTO = new ProductToDestroyDTO();
        productToDestroyDTO.setId(productToDestroy.getId());
        productToDestroyDTO.setNumLot(productToDestroy.getNumLot());
        productToDestroyDTO.setPrixAchat(productToDestroy.getPrixAchat());
        productToDestroyDTO.setPrixUni(productToDestroy.getPrixUnit());
        FournisseurProduit fournisseurProduit = productToDestroy.getFournisseurProduit();
        productToDestroyDTO.setProduitCodeCip(fournisseurProduit.getCodeCip());
        productToDestroyDTO.setProduitName(fournisseurProduit.getProduit().getLibelle());
        productToDestroyDTO.setQuantity(productToDestroy.getQuantity());
        productToDestroyDTO.setDatePeremption(DateUtil.formatFr(productToDestroy.getDatePeremption()));
        productToDestroyDTO.setDateDestruction(DateUtil.formatFr(productToDestroy.getDateDestuction()));
        productToDestroyDTO.setFournisseur(fournisseurProduit.getFournisseur().getLibelle());
        productToDestroyDTO.setUpdatedDate(DateUtil.format(productToDestroy.getUpdated()));
        productToDestroyDTO.setCreatedDate(DateUtil.format(productToDestroy.getCreated()));
        productToDestroyDTO.setPeremptionStatut(buildPeremptionStatut(productToDestroy.getDatePeremption()));
        productToDestroyDTO.setDestroyed(productToDestroy.isDestroyed());
        return productToDestroyDTO;
    }
}

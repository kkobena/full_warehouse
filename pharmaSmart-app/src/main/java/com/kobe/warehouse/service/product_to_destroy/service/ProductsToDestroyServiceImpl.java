package com.kobe.warehouse.service.product_to_destroy.service;

import static com.kobe.warehouse.service.utils.ServiceUtil.buildPeremptionStatut;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.hasText;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AppUserNames;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotStockLocation;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.ProductsToDestroy;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.FournisseurProduitRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.LotStockLocationRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.ProductsToDestroyRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.records.Keys;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
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
import java.util.Objects;
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
    private final LotStockLocationRepository lotStockLocationRepository;
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
        LotStockLocationRepository lotStockLocationRepository,
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
        this.lotStockLocationRepository = lotStockLocationRepository;
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
            ProductsToDestroy productsToDestroy = addProductQuantity(productToDestroyPayload1, null,
                magasin);
            updateStock(productsToDestroy, productToDestroyPayload1.storageId());
            updateLot(productsToDestroy, productToDestroyPayload1.storageId());
        }
    }

    /**
     * Stock de départ à consigner sur une mise au rebut de lot.
     *
     * <p>Trois sources, de la plus précise à la plus large : ce que l'appelant a fourni, la
     * quantité du lot avant retrait, puis le stock total du produit — un lot dont la quantité
     * n'est pas tenue (reprise, saisie manuelle) reste ainsi traçable.
     *
     * @throws GenericError si aucune ne donne un stock positif : retirer d'un stock vide n'a
     *                      pas de sens, et le dire vaut mieux qu'un échec technique.
     */
    private int stockInitialDuLot(ProductToDestroyPayload payload, Lot lot, Magasin magasin) {
        int stock = Objects.requireNonNullElse(payload.stockInitial(), 0);
        if (stock < 1) {
            stock = Objects.requireNonNullElse(lot.getQuantity(), 0);
        }
        if (stock < 1) {
            stock = findStockProduits(magasin.getId(), lot.getProduit().getId())
                .stream()
                .mapToInt(StockProduit::getQtyStock)
                .sum();
        }
        if (stock < 1) {
            throw new GenericError(
                "Le lot " + lot.getNumLot() + " n'a plus de stock : il n'y a rien à retirer.");
        }
        return stock;
    }

    /**
     * Le couple produit/fournisseur d'origine du lot, ou {@code null} s'il n'a pas de réception.
     */
    private FournisseurProduit fournisseurProduitDuLot(Lot lot) {
        if (isNull(lot) || isNull(lot.getOrderLine())) {
            return null;
        }
        return lot.getOrderLine().getFournisseurProduit();
    }

    private ProductsToDestroy addProductQuantity(ProductToDestroyPayload productToDestroyPayload,
        Lot lot, Magasin magasin) {
        // `lot.orderLine` est NULLABLE : un lot peut exister sans réception d'origine — saisi
        // à la main sur un stock déjà présent, ou repris d'une migration. Le déréférencer
        // sans précaution faisait échouer la mise au rebut en HTTP 500, précisément sur les
        // lots les plus anciens.
        FournisseurProduit fournisseurProduit = fournisseurProduitDuLot(lot);
        if (isNull(lot) && nonNull(productToDestroyPayload.lotId())) {
            lot = this.lotRepository.findById(productToDestroyPayload.lotId()).orElse(null);
            if (nonNull(lot)) {
                fournisseurProduit = fournisseurProduitDuLot(lot);
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
            Produit produit = this.produitRepository.getReferenceById(
                productToDestroyPayload.produitId());
            fournisseurProduit = produit.getFournisseurProduitPrincipal();
            if (isNull(fournisseurProduit)) {
                throw new GenericError("Le produit n'est pas rattaché au fournisseur ");
            }
        }
        ProductsToDestroy productsToDestroy = new ProductsToDestroy();

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
            // Le stock d'AVANT le retrait, que la ligne de mise au rebut a vocation à
            // consigner : sans lui, une destruction ne dit plus sur quel stock elle a porté.
            //
            // Il n'était jamais renseigné sur cette branche — l'écran ne l'envoie pas, un lot
            // le portant déjà — et restait donc à zéro, que la contrainte @Min(1) de l'entité
            // refuse. Le retrait d'un lot périmé échouait ainsi en HTTP 500, sans que la
            // moindre trace n'en dise la raison.
            productsToDestroy.setStockInitial(
                stockInitialDuLot(productToDestroyPayload, lot, magasin));
        } else {
            productsToDestroy.setStockInitial(
                Objects.requireNonNullElse(productToDestroyPayload.stockInitial(), 0));
            // Produit product = fournisseurProduit.getProduit();
            // productsToDestroy.setDatePeremption(product.getPerimeAt());
            productsToDestroy.setPrixAchat(fournisseurProduit.getPrixAchat());
            productsToDestroy.setPrixUnit(fournisseurProduit.getPrixUni());
            //a revoir
            if (!productToDestroyPayload.editing()) {
                List<StockProduit> stockProduits = findStockProduits(
                    magasin.getId(),
                    productsToDestroy.getFournisseurProduit().getProduit().getId()
                );
                productsToDestroy.setStockInitial(
                    stockProduits.stream().mapToInt(StockProduit::getQtyStock).sum());
                /* product.setPerimeAt(null);
                this.produitRepository.save(product);*/
            } else {
                productsToDestroy.setDatePeremption(productToDestroyPayload.datePeremption());
                productsToDestroy.setNumLot(productToDestroyPayload.numLot());
                // Même déballage risqué qu'à la construction : `stockInitial` est un Integer
                // NULLABLE dans la charge utile — l'écran ne l'envoie pas quand la ligne
                // vient d'une saisie manuelle — et l'affecter à un `int` levait un NPE, rendu
                // en HTTP 500 sans le moindre message exploitable.
                productsToDestroy.setStockInitial(
                    Objects.requireNonNullElse(productToDestroyPayload.stockInitial(), 0));
            }
        }
        productsToDestroy.setMagasin(magasin);

        return this.productsToDestroyRepository.save(productsToDestroy);
    }

    @Override
    public Page<ProductToDestroyDTO> findAll(ProductToDestroyFilter produidToDestroyFilter,
        Pageable pageable) {
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
            productToDestroyDTO.setUser(
                AppUserNames.shortName(user));
            return productToDestroyDTO;
        });
    }

    @Override
    public ProductToDestroySumDTO getSum(ProductToDestroyFilter produidToDestroyFilter) {
        return this.productsToDestroyRepository.getSum(
            this.productsToDestroyRepository.buildCombinedSpecification(produidToDestroyFilter));
    }

    @Override
    @Transactional
    public void destroy(Keys keys) {
        List<ProductsToDestroy> productsToDestroys = this.productsToDestroyRepository.findAllById(
            keys.ids());
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
        // Numéro + produit : la vraie clé d'un lot (voir updateLot).
        Lot lot = this.lotRepository.findByNumLotAndProduitId(
            productToDestroyPayload.numLot(),
            productToDestroyPayload.produitId()
        ).orElse(null);
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
                    magasin = magasinRepository.getReferenceById(
                        productToDestroyPayload.magasinId());
                }
                addProductQuantity(productToDestroyPayload, lot, magasin);
            }
        );
    }

    private void update(ProductsToDestroy productsToDestroy,
        ProductToDestroyPayload productToDestroyPayload, Lot lot) {
        prevaliderLot(lot, productToDestroyPayload.quantity() + productsToDestroy.getQuantity());
        productsToDestroy.setQuantity(
            productToDestroyPayload.quantity() + productsToDestroy.getQuantity());
        productsToDestroy.setUpdated(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void closeLastEdition() {
        this.productsToDestroyRepository.findAllByEditingTrueAndCreatedEquals(LocalDate.now(),
            this.userService.getUser().getId()).forEach(
            productsToDestroy -> {
                // Pour la clôture manuelle (editing=true), on n'a pas de storageId connu
                // → cascade automatique sur toutes les localisations du lot
                updateStock(productsToDestroy, null);
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
        ProductsToDestroy productsToDestroy = this.productsToDestroyRepository.getReferenceById(
            productToDestroyPayload.id());
        Lot lot = this.lotRepository.findByNumLotAndProduitId(
            productToDestroyPayload.numLot(),
            productsToDestroy.getFournisseurProduit().getProduit().getId()
        ).orElse(null);
        prevaliderLot(lot, productToDestroyPayload.quantity());
        productsToDestroy.setQuantity(productToDestroyPayload.quantity());
        productsToDestroy.setUpdated(LocalDateTime.now());
        this.productsToDestroyRepository.save(productsToDestroy);
    }

    @Override
    public Page<ProductToDestroyDTO> findEditing(ProductToDestroyFilter produidToDestroyFilter,
        Pageable pageable) {
        Sort sort = Sort.by(Direction.DESC, "updated");
        pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        return this.productsToDestroyRepository.findAll(
            this.productsToDestroyRepository.buildEditing(this.userService.getUser().getId(),
                produidToDestroyFilter.searchTerm()),
            pageable
        ).map(this::buildProductToDestroy);
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

    private List<StockProduit> findStockProduits(Integer magasinId, Integer produitId) {
        return this.stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(magasinId,
            produitId);
    }

    /**
     * Met à jour StockProduit (stock niveau produit par emplacement). Si storageId fourni → cible
     * uniquement ce storage. Sinon → déduit d'abord du PRINCIPAL, puis des autres (comportement
     * historique).
     */
    private void updateStock(ProductsToDestroy productsToDestroy, Integer storageId) {
        int quantity = productsToDestroy.getQuantity();
        List<StockProduit> stockProduits = findStockProduits(
            productsToDestroy.getMagasin().getId(),
            productsToDestroy.getFournisseurProduit().getProduit().getId()
        );
        if (stockProduits.isEmpty()) {
            return;
        }

        if (nonNull(storageId)) {
            // Cibler uniquement le StockProduit du storage sélectionné
            stockProduits.stream()
                .filter(sp -> sp.getStorage().getId().equals(storageId))
                .findFirst()
                .ifPresent(sp -> {
                    int newQty = Math.max(sp.getQtyStock() - quantity, 0);
                    sp.setQtyStock(newQty);
                    sp.setUpdatedAt(LocalDateTime.now());
                    stockProduitRepository.save(sp);
                });
        } else {
            // Comportement historique : PRINCIPAL d'abord, puis les autres
            Map<Boolean, List<StockProduit>> partition = stockProduits
                .stream()
                .collect(Collectors.partitioningBy(
                    s -> s.getStorage().getStorageType() == StorageType.PRINCIPAL));

            int remaining = quantity;
            List<StockProduit> principal = partition.get(true);
            if (!CollectionUtils.isEmpty(principal)) {
                StockProduit sp = principal.getFirst();
                int toDeduct = Math.min(sp.getQtyStock(), remaining);
                sp.setQtyStock(sp.getQtyStock() - toDeduct);
                remaining -= toDeduct;
                sp.setUpdatedAt(LocalDateTime.now());
                stockProduitRepository.save(sp);
            }
            if (remaining > 0) {
                List<StockProduit> others = partition.get(false);
                if (!CollectionUtils.isEmpty(others)) {
                    for (StockProduit sp : others) {
                        if (remaining <= 0) {
                            break;
                        }
                        int toDeduct = Math.min(sp.getQtyStock(), remaining);
                        if (toDeduct <= 0) {
                            continue;
                        }
                        sp.setQtyStock(sp.getQtyStock() - toDeduct);
                        remaining -= toDeduct;
                        sp.setUpdatedAt(LocalDateTime.now());
                        stockProduitRepository.save(sp);
                    }
                }
            }
        }
    }

    /**
     * Met à jour Lot.quantity ET LotStockLocation.qty pour la bonne localisation.
     *
     * @param storageId si fourni → met à jour uniquement ce LotStockLocation ; sinon → cascade sur
     *                  toutes les localisations du lot (PRINCIPAL en premier).
     */
    private void updateLot(ProductsToDestroy productsToDestroy, Integer storageId) {
        if (!hasText(productsToDestroy.getNumLot())) {
            return;
        }
        // Le numéro de lot n'est unique QUE PAR PRODUIT : deux fournisseurs livrent
        // couramment un « LOT-2026-014 » chacun. Chercher sur le seul numéro rendait donc
        // plusieurs lignes là où le type promet au plus une, et le retrait groupé échouait en
        // HTTP 500 dès que deux produits partageaient un numéro — ce que le retrait d'un lot
        // isolé ne rencontrait presque jamais.
        this.lotRepository.findByNumLotAndProduitId(
            productsToDestroy.getNumLot(),
            productsToDestroy.getFournisseurProduit().getProduit().getId()
        ).ifPresent(lot -> {
            int qty = productsToDestroy.getQuantity();
            // 1. Mettre à jour le total du lot
            int newLotQty = Math.max(lot.getQuantity() - qty, 0);
            lot.setQuantity(newLotQty);
            lot.setUpdated(LocalDateTime.now());
            this.lotRepository.save(lot);

            // 2. Mettre à jour LotStockLocation
            updateLotStockLocations(lot, qty, storageId);
        });
    }

    /**
     * Décrémente les entrées LotStockLocation pour un lot donné. Si storageId fourni → cible cette
     * localisation précise. Sinon → cascade PRINCIPAL en premier, puis les autres par qty
     * décroissante.
     */
    private void updateLotStockLocations(Lot lot, int quantity, Integer storageId) {
        if (nonNull(storageId)) {
            // Cibler la localisation exacte
            this.lotStockLocationRepository.findByLotIdAndStorageId(lot.getId(), storageId)
                .ifPresent(lsl -> {
                    int newQty = Math.max(lsl.getQty() - quantity, 0);
                    lsl.setQty(newQty);
                    this.lotStockLocationRepository.save(lsl);
                });
        } else {
            // Cascade sur toutes les localisations disponibles (PRINCIPAL d'abord)
            List<LotStockLocation> locations = this.lotStockLocationRepository.findAvailableByLotId(
                lot.getId());
            int remaining = quantity;
            for (LotStockLocation lsl : locations) {
                if (remaining <= 0) {
                    break;
                }
                int toDeduct = Math.min(lsl.getQty(), remaining);
                if (toDeduct <= 0) {
                    continue;
                }
                lsl.setQty(lsl.getQty() - toDeduct);
                remaining -= toDeduct;
                this.lotStockLocationRepository.save(lsl);
            }
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
                throw new GenericError(
                    "La quantité saisie est supérieure à la quantité disponible ");
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
        productToDestroyDTO.setProduitId(fournisseurProduit.getProduit().getId());
        productToDestroyDTO.setQuantity(productToDestroy.getQuantity());
        productToDestroyDTO.setDatePeremption(
            DateUtil.formatFr(productToDestroy.getDatePeremption()));
        productToDestroyDTO.setDateDestruction(
            DateUtil.formatFr(productToDestroy.getDateDestuction()));
        productToDestroyDTO.setFournisseur(fournisseurProduit.getFournisseur().getLibelle());
        productToDestroyDTO.setUpdatedDate(DateUtil.format(productToDestroy.getUpdated()));
        productToDestroyDTO.setCreatedDate(DateUtil.format(productToDestroy.getCreated()));
        productToDestroyDTO.setPeremptionStatut(
            buildPeremptionStatut(productToDestroy.getDatePeremption()));
        productToDestroyDTO.setDestroyed(productToDestroy.isDestroyed());
        return productToDestroyDTO;
    }
}

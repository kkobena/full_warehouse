package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Decondition;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import com.kobe.warehouse.repository.DeconditionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.dto.DeconditionDTO;
import com.kobe.warehouse.service.errors.StockException;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Decondition}.
 */
@Service
@Transactional
public class DeconditionService {

    private final Logger log = LoggerFactory.getLogger(DeconditionService.class);
    private final DeconditionRepository deconditionRepository;
    private final ProduitRepository produitRepository;
    private final LogsService logsService;
    private final StockProduitRepository stockProduitRepository;
    private final StorageService storageService;
    private final InventoryTransactionService inventoryTransactionService;

    public DeconditionService(
        DeconditionRepository deconditionRepository,
        ProduitRepository produitRepository,
        LogsService logsService,
        StockProduitRepository stockProduitRepository,
        StorageService storageService,
        InventoryTransactionService inventoryTransactionService
    ) {
        this.deconditionRepository = deconditionRepository;
        this.produitRepository = produitRepository;
        this.logsService = logsService;
        this.stockProduitRepository = stockProduitRepository;
        this.storageService = storageService;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    private Decondition createDecondition(
        Produit produit,
        int beforeStock,
        int afterStock,
        int mvtQty,
        AppUser user,
        TypeDeconditionnement typeDeconditionnement
    ) {
        Decondition decondition = new Decondition();
        decondition.setProduit(produit);
        decondition.setDateMtv(LocalDateTime.now());
        decondition.setQtyMvt(mvtQty);
        decondition.setStockBefore(beforeStock);
        decondition.setStockAfter(afterStock);
        decondition.setUser(user);
        decondition.setTypeDeconditionnement(typeDeconditionnement);
        decondition = deconditionRepository.save(decondition);
        inventoryTransactionService.save(decondition);
        return decondition;
    }

    //TODO a revoir pour optimisation des ug
    public void save(DeconditionDTO deconditionDTO) throws StockException {
        Produit parent = produitRepository.getReferenceById(deconditionDTO.getProduitId());
        StockProduit stockProduit = stockProduitRepository.findOneByProduitIdAndStockageId(
            deconditionDTO.getProduitId(),
            storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
        );
        int stock = stockProduit.getQtyStock();
        AppUser user = storageService.getUser();
        if (stock >= deconditionDTO.getQtyMvt()) {
            stockProduit.setQtyStock(stock - deconditionDTO.getQtyMvt());
            stockProduit.setQtyVirtual(stockProduit.getQtyStock());
            stockProduit.setUpdatedAt(LocalDateTime.now());
            Produit detail = parent.getProduits().getFirst();
            StockProduit stockDetail = stockProduitRepository.findOneByProduitIdAndStockageId(
                detail.getId(),
                storageService.getDefaultConnectedUserPointOfSaleStorage().getId()
            );
            int stockDetailInit = stockDetail.getQtyStock();
            int stockDetailFinal = (deconditionDTO.getQtyMvt() * parent.getItemQty()) + stockDetailInit;
            stockDetail.setQtyStock(stockDetailFinal);
            stockDetail.setQtyVirtual(stockProduit.getQtyStock());
            stockDetail.setUpdatedAt(LocalDateTime.now());
            stockProduitRepository.save(stockDetail);
            stockProduitRepository.save(stockProduit);

            logsService.create(TransactionType.DECONDTION_OUT, TransactionType.FORCE_STOCK.getValue(), parent.getId().toString());
            Decondition deconditionParent = createDecondition(
                parent,
                stock,
                stockProduit.getQtyStock(),
                deconditionDTO.getQtyMvt(),
                user,
                TypeDeconditionnement.DECONDTION_OUT
            );
            Decondition decondition = createDecondition(
                detail,
                stockDetailInit,
                stockDetailFinal,
                (deconditionDTO.getQtyMvt() * parent.getItemQty()),
                user,
                TypeDeconditionnement.DECONDTION_IN
            );
            FournisseurProduit fournisseurProduitPrincipal = parent.getFournisseurProduitPrincipal();
            if (fournisseurProduitPrincipal != null) {
                String desc = String.format(
                    "Déconditionnement du produit %s %s quantité initiale %d quantité déconditionnée %d quantité finale %d",
                    fournisseurProduitPrincipal.getCodeCip(),
                    parent.getLibelle(),
                    stock,
                    deconditionDTO.getQtyMvt(),
                    stockProduit.getQtyStock()
                );
                logsService.create(TransactionType.DECONDTION_OUT, desc, deconditionParent.getId().toString());
            }
            FournisseurProduit fournisseurProduitPrincipaldetail = detail.getFournisseurProduitPrincipal();
            if (fournisseurProduitPrincipal != null) {
                String desc = String.format(
                    "Déconditionnement du produit %s %s quantité initiale %d quantité ajoutée %d quantité finale %d",
                    fournisseurProduitPrincipaldetail != null ? fournisseurProduitPrincipaldetail.getCodeCip() : "",
                    detail.getLibelle(),
                    stockDetailInit,
                    stockDetailFinal,
                    stockDetail.getQtyStock()
                );
                logsService.create(TransactionType.DECONDTION_IN, desc, decondition.getId().toString());
            }
        } else {
            throw new StockException();
        }
    }

    /**
     * Get all the deconditions.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<DeconditionDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Deconditions");
        return deconditionRepository.findAll(pageable).map(DeconditionDTO::new);
    }

    /**
     * Get one decondition by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Decondition> findOne(Long id) {
        log.debug("Request to get Decondition : {}", id);
        return deconditionRepository.findById(id);
    }

    /**
     * Delete the decondition by id.
     *
     * @param id the id of the entity.
     */
    public void delete(Long id) {
        log.debug("Request to delete Decondition : {}", id);
        deconditionRepository.deleteById(id);
    }
}

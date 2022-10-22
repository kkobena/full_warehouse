package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeDeconditionnement;
import com.kobe.warehouse.repository.*;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.DeconditionDTO;
import com.kobe.warehouse.web.rest.errors.StockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;


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
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final StockProduitRepository stockProduitRepository;
    private final StorageService storageService;

    public DeconditionService(DeconditionRepository deconditionRepository, ProduitRepository produitRepository, LogsService logsService, InventoryTransactionRepository inventoryTransactionRepository, StockProduitRepository stockProduitRepository, StorageService storageService) {
        this.deconditionRepository = deconditionRepository;
        this.produitRepository = produitRepository;
        this.logsService = logsService;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.stockProduitRepository = stockProduitRepository;
        this.storageService = storageService;
    }

    private void createDecondition(Produit produit, int beforeStock, int afterStock, int mvtQty, User user, TransactionType transactionType, TypeDeconditionnement typeDeconditionnement){
        Decondition decondition = new Decondition();
        decondition.setProduit(produit);
        decondition.setDateMtv(Instant.now());
        decondition.setQtyMvt(mvtQty);
        decondition.setStockBefore(beforeStock);
        decondition.setStockAfter(afterStock);
        decondition.setDateDimension(Constants.DateDimension(LocalDate.now()));
        decondition.setUser(user);
        decondition.setTypeDeconditionnement(typeDeconditionnement);
        deconditionRepository.save(decondition);
        createInventory(decondition,
            transactionType,
            beforeStock,afterStock, user);
    }
//TODO a revoir pour optimisation des ug
    public void save(DeconditionDTO deconditionDTO) throws StockException {
        Produit parent = produitRepository.getOne(deconditionDTO.getProduitId());
        StockProduit stockProduit = this.stockProduitRepository.findOneByProduitIdAndStockageId(deconditionDTO.getProduitId(), storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
        int stock = stockProduit.getQtyStock();
        User user = this.storageService.getUser();
        if (stock >= deconditionDTO.getQtyMvt()) {
            stockProduit.setQtyStock(stock - deconditionDTO.getQtyMvt());
            stockProduit.setQtyVirtual(stockProduit.getQtyStock());
            stockProduit.setUpdatedAt(Instant.now());
            Produit detail = parent.getProduits().get(0);
            StockProduit stockDetail = this.stockProduitRepository.findOneByProduitIdAndStockageId(detail.getId(), storageService.getDefaultConnectedUserPointOfSaleStorage().getId());
            int stockDetailInit = stockDetail.getQtyStock();
            int stockDetailFinal = (deconditionDTO.getQtyMvt() * parent.getItemQty()) + stockDetailInit;
            stockDetail.setQtyStock(stockDetailFinal);
            stockDetail.setQtyVirtual(stockProduit.getQtyStock());
            stockDetail.setUpdatedAt(Instant.now());
            stockProduitRepository.save(stockDetail);
            stockProduitRepository.save(stockProduit);

            this.logsService.create(TransactionType.DECONDTION_OUT, TransactionType.FORCE_STOCK.getValue(), parent.getId().toString());
            createDecondition(parent,stock,stockProduit.getQtyStock(),deconditionDTO.getQtyMvt(),user,TransactionType.DECONDTION_OUT,TypeDeconditionnement.DECONDTION_OUT);
            createDecondition(detail,stockDetailInit,stockDetailFinal, (deconditionDTO.getQtyMvt() * parent.getItemQty()),user,TransactionType.DECONDTION_IN,TypeDeconditionnement.DECONDTION_IN);
            FournisseurProduit fournisseurProduitPrincipal = parent.getFournisseurProduitPrincipal();
            if (fournisseurProduitPrincipal != null ) {
                String desc = String.format("Déconditionnement du produit %s %s quantité initiale %d quantité déconditionnée %d quantité finale %d", fournisseurProduitPrincipal != null ? fournisseurProduitPrincipal.getCodeCip() : "",parent.getLibelle(), stock, deconditionDTO.getQtyMvt(), stockProduit.getQtyStock());
                this.logsService.create(TransactionType.DECONDTION_OUT, desc, parent.getId().toString());
            }
            FournisseurProduit fournisseurProduitPrincipaldetail = detail.getFournisseurProduitPrincipal();
            if (fournisseurProduitPrincipal != null ) {
                String desc = String.format("Déconditionnement du produit %s %s quantité initiale %d quantité ajoutée %d quantité finale %d", fournisseurProduitPrincipaldetail != null ? fournisseurProduitPrincipaldetail.getCodeCip() : "",detail.getLibelle(), stockDetailInit, stockDetailFinal, stockDetail.getQtyStock());
                this.logsService.create(TransactionType.DECONDTION_IN, desc, parent.getId().toString());
            }
        } else {
            throw new StockException();
        }

    }

    private void createInventory(Decondition decondition, TransactionType transactionType, int quantityBefor, int quantityFinal, User user) {
        DateDimension dateD = Constants.DateDimension(LocalDate.now());
        Produit p = decondition.getProduit();
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setDecondition(decondition);
        inventoryTransaction.setCreatedAt(Instant.now());
        inventoryTransaction.setProduit(p);
        inventoryTransaction.setUser(user);
        inventoryTransaction.setMagasin(user.getMagasin());
        inventoryTransaction.setQuantity(decondition.getQtyMvt());
        inventoryTransaction.setTransactionType(transactionType);
        inventoryTransaction.setDateDimension(dateD);
        inventoryTransaction.setQuantityBefor(quantityBefor);
         inventoryTransaction.setQuantityAfter(quantityFinal);
        inventoryTransaction.setRegularUnitPrice(p.getRegularUnitPrice());
        inventoryTransaction.setCostAmount(p.getCostAmount());
        inventoryTransactionRepository.save(inventoryTransaction);
    }

    /**
     * Get all the deconditions.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<Decondition> findAll(Pageable pageable) {
        log.debug("Request to get all Deconditions");
        return deconditionRepository.findAll(pageable);
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

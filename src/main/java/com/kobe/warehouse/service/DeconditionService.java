package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.DeconditionRepository;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public DeconditionService(UserRepository userRepository, InventoryTransactionRepository inventoryTransactionRepository, DeconditionRepository deconditionRepository, ProduitRepository produitRepository) {
        this.deconditionRepository = deconditionRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    private User getUser() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> userRepository.findOneByLogin(login));
        return user.orElseGet(null);
    }

    /**
     * Save a decondition.
     *
     * @param deconditionDTO the entity to save.
     * @return the persisted entity.
     */
    public void save(DeconditionDTO deconditionDTO) throws Exception {
        Produit parent = produitRepository.getOne(deconditionDTO.getProduitId());
        int stock = parent.getQuantity();
        User user = getUser();
        if (stock >= deconditionDTO.getQtyMvt()) {
            parent.setQuantity(stock - deconditionDTO.getQtyMvt());
            //  Produit detail = produitRepository.findFirstByParentId(parent.getId());
            Produit detail = parent.getProduits().get(0);
            int stockBefore = detail.getQuantity();
            detail.setQuantity((deconditionDTO.getQtyMvt() * parent.getItemQty()) + detail.getQuantity());
            produitRepository.save(detail);
            produitRepository.save(parent);

            Decondition decondition = new Decondition();
            decondition.setProduit(parent);
            decondition.setDateMtv(Instant.now());
            decondition.setQtyMvt(deconditionDTO.getQtyMvt());
            decondition.setStockBefore(stock);
            decondition.setStockAfter(parent.getQuantity());
            decondition.setDateDimension(Constants.DateDimension(LocalDate.now()));
            decondition.setUser(user);
            deconditionRepository.save(decondition);
            createInventory(decondition,
                TransactionType.DECONDTION_OUT,
                stock, user);
            decondition = new Decondition();
            decondition.setProduit(detail);
            decondition.setDateMtv(Instant.now());
            decondition.setQtyMvt(deconditionDTO.getQtyMvt()*parent.getItemQty());
            decondition.setStockBefore(stockBefore);
            decondition.setStockAfter(detail.getQuantity());
            decondition.setDateDimension(Constants.DateDimension(LocalDate.now()));
            decondition.setUser(user);
            deconditionRepository.save(decondition);
            createInventory(decondition,
                TransactionType.DECONDTION_IN,
                stockBefore, user);
        }else{
            throw new Exception("Stock insuffisant");
        }


    }

    private void createInventory(Decondition decondition, TransactionType transactionType, int quantityBefor, User user) {

        DateDimension dateD = Constants.DateDimension(LocalDate.now());
        Produit p = decondition.getProduit();
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setCreatedAt(Instant.now());
        inventoryTransaction.setUpdatedAt(inventoryTransaction.getCreatedAt());
        inventoryTransaction.setProduit(p);
        inventoryTransaction.setUser(user);
        inventoryTransaction.setAmount(decondition.getQtyMvt() * p.getCostAmount());
        inventoryTransaction.setQuantity(decondition.getQtyMvt());
        inventoryTransaction.setTransactionType(transactionType);
        inventoryTransaction.setDateDimension(dateD);
        inventoryTransaction.setQuantityBefor(quantityBefor);
        inventoryTransaction.setQuantityAfter(p.getQuantity());
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

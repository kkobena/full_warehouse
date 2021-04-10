package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.*;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.AjustementDTO;
import com.kobe.warehouse.service.dto.DeconditionDTO;
import com.kobe.warehouse.web.rest.errors.StockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing {@link Ajustement}.
 */
@Service
@Transactional
public class AjustementService {

    private final Logger log = LoggerFactory.getLogger(AjustementService.class);

    private final AjustementRepository ajustementRepository;
    private final ProduitRepository produitRepository;
    private final UserRepository userRepository;
    private final AjustRepository ajustRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    private User getUser() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> userRepository.findOneByLogin(login));
        return user.orElseGet(null);
    }

    public AjustementService(AjustRepository ajustRepository, UserRepository userRepository, InventoryTransactionRepository inventoryTransactionRepository, AjustementRepository ajustementRepository, ProduitRepository produitRepository) {
        this.ajustementRepository = ajustementRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.ajustRepository = ajustRepository;
    }

    private Ajust createAjsut(Long id) {

        if (id == null) {
            Ajust  ajust = new Ajust();
            ajust.setDateDimension(Constants.DateDimension(LocalDate.now()));
            ajust.setUser(getUser());
            ajust.setDateMtv(Instant.now());
            return ajustRepository.save(ajust);
        }
        return ajustRepository.getOne(id);
    }

    public AjustementDTO save(AjustementDTO ajustementDTO) {
        Ajust ajust = createAjsut(ajustementDTO.getAjustId());
        Produit produit = produitRepository.getOne(ajustementDTO.getProduitId());
        int stock = produit.getQuantity();
        Ajustement ajustement = new Ajustement();
        ajustement.setAjust(ajust);
        ajustement.setProduit(produit);
        ajustement.setDateMtv(Instant.now());
        ajustement.setQtyMvt(ajustementDTO.getQtyMvt());
        ajustement.setStockBefore(stock);
        ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
    return new AjustementDTO(ajustementRepository.save(ajustement));

    }
    public void save(Long idAjsut) {
        Ajust ajust=ajustRepository.getOne(idAjsut);
        List<Ajustement> ajustements=ajustementRepository.findAllByAjustId(idAjsut);
        save(ajustements);
        ajust.setStatut(SalesStatut.CLOSE);

    }
    private void save(List<Ajustement> ajustements) {
        User user = getUser();
        for (Ajustement ajustement : ajustements) {
            Produit produit = ajustement.getProduit();
            int stock = produit.getQuantity();
            TransactionType transactionType = TransactionType.AJUSTEMENT_OUT;
            if (ajustement.getQtyMvt() > 0) {
                transactionType = TransactionType.AJUSTEMENT_IN;
            }
            produit.setQuantity(stock + ajustement.getQtyMvt());
            produitRepository.save(produit);
            createInventory(ajustement,
                transactionType,
                stock, user);

        }


    }

    public AjustementDTO update(AjustementDTO ajustementDTO) {
        Ajustement ajustement = ajustementRepository.getOne(ajustementDTO.getId());
        Produit produit = ajustement.getProduit();
        int stock = produit.getQuantity();
        ajustement.setDateMtv(Instant.now());
        ajustement.setQtyMvt(ajustementDTO.getQtyMvt());
        ajustement.setStockBefore(stock);
        ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
     return  new AjustementDTO(ajustementRepository.save(ajustement));

    }

    private void createInventory(Ajustement ajustement, TransactionType transactionType, int quantityBefor, User user) {

        DateDimension dateD = Constants.DateDimension(LocalDate.now());
        Produit p = ajustement.getProduit();
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setCreatedAt(Instant.now());
        inventoryTransaction.setUpdatedAt(inventoryTransaction.getCreatedAt());
        inventoryTransaction.setProduit(p);
        inventoryTransaction.setUser(user);
        inventoryTransaction.setAmount(ajustement.getQtyMvt() * p.getCostAmount());
        inventoryTransaction.setQuantity(ajustement.getQtyMvt());
        inventoryTransaction.setTransactionType(transactionType);
        inventoryTransaction.setDateDimension(dateD);
        inventoryTransaction.setQuantityBefor(quantityBefor);
        inventoryTransaction.setQuantityAfter(p.getQuantity());
        inventoryTransaction.setRegularUnitPrice(p.getRegularUnitPrice());
        inventoryTransaction.setCostAmount(p.getCostAmount());
        inventoryTransactionRepository.save(inventoryTransaction);


    }

    @Transactional(readOnly = true)
    public List<Ajustement> findAll(Long id) {
        log.debug("Request to get all Ajustements");
        return ajustementRepository.findAllByAjustId(id);
    }
    @Transactional(readOnly = true)
    public List<Ajustement> findAll() {
        log.debug("Request to get all Ajustements");
        return ajustementRepository.findAll();
    }
    @Transactional(readOnly = true)
    public List<Ajustement> findAllSaved() {
        log.debug("Request to get all Ajustements");
        return ajustementRepository.findAllByAjustStatut(SalesStatut.CLOSE, Sort.by(Sort.Direction.DESC,"dateMtv"));
    }



}

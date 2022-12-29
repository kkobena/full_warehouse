package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.DateDimension;
import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.AjustRepository;
import com.kobe.warehouse.repository.AjustementRepository;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.dto.AjustementDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final StorageService storageService;
    private final StockProduitRepository stockProduitRepository;

    public AjustementService(AjustementRepository ajustementRepository, ProduitRepository produitRepository, UserRepository userRepository, AjustRepository ajustRepository, InventoryTransactionRepository inventoryTransactionRepository, StorageService storageService, StockProduitRepository stockProduitRepository) {
        this.ajustementRepository = ajustementRepository;
        this.produitRepository = produitRepository;
        this.userRepository = userRepository;
        this.ajustRepository = ajustRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.storageService = storageService;
        this.stockProduitRepository = stockProduitRepository;
    }

    private User getUser() {
        Optional<User> user = SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> userRepository.findOneByLogin(login));
        return user.orElseGet(null);
    }

    private Ajust createAjsut(Long id, String comment, Long storageId) {

        if (id == null) {
            Ajust ajust = new Ajust();
            ajust.setCommentaire(comment);
            ajust.setDateDimension(Constants.DateDimension(LocalDate.now()));
            ajust.setUser(getUser());
            ajust.setDateMtv(Instant.now());
            if (storageId != null) {
                ajust.setStorage(storageService.getOne(storageId));

            } else {
                ajust.setStorage(storageService.getDefaultConnectedUserMainStorage());
            }
            return ajustRepository.save(ajust);
        }
        return ajustRepository.getReferenceById(id);
    }

    public AjustementDTO save(AjustementDTO ajustementDTO) {
        Ajust ajust = createAjsut(ajustementDTO.getAjustId(), ajustementDTO.getCommentaire(), ajustementDTO.getStorageId());
        Produit produit = produitRepository.getReferenceById(ajustementDTO.getProduitId());
        int stock = stockProduitRepository.findStockProduitByStorageIdAndProduitId(ajust.getStorage().getId(), ajustementDTO.getProduitId()).get().getQtyStock();
        Ajustement ajustement;
        if (ajustementDTO.getAjustId() == null) {
            ajustement = create(ajustementDTO, ajust, produit, stock);
        } else {
            ajustement = createOrUpdate(ajustementDTO, ajust, produit, stock);
        }

        return new AjustementDTO(ajustementRepository.save(ajustement));

    }

    private Ajustement create(AjustementDTO ajustementDTO, Ajust ajust, Produit produit, int stock) {
        Ajustement ajustement = new Ajustement();
        ajustement.setAjust(ajust);
        ajustement.setMotifAjustement(fromMotifId(ajustementDTO.getMotifAjustementId()));
        ajustement.setProduit(produit);
        ajustement.setDateMtv(Instant.now());
        ajustement.setQtyMvt(ajustementDTO.getQtyMvt());
        ajustement.setStockBefore(stock);
        ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
        return ajustement;
    }

    private Ajustement createOrUpdate(AjustementDTO ajustementDTO, Ajust ajust, Produit produit, int stock) {
        Optional<Ajustement> optionalAjustement = ajustementRepository.findFirstByAjustIdAndProduitId(ajustementDTO.getAjustId(), ajustementDTO.getProduitId());
        if (optionalAjustement.isEmpty()) return create(ajustementDTO, ajust, produit, stock);
        Ajustement ajustement = optionalAjustement.get();
        ajustement.setDateMtv(Instant.now());
        ajustement.setQtyMvt(ajustement.getQtyMvt() + ajustementDTO.getQtyMvt());
        ajustement.setStockBefore(stock);
        ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
        return ajustement;
    }

    public void saveAjust(AjustementDTO ajustementDTO) {
        Ajust ajust = ajustRepository.getReferenceById(ajustementDTO.getAjustId());
        List<Ajustement> ajustements = ajustementRepository.findAllByAjustId(ajust.getId());
        save(ajustements);
        ajust.setCommentaire(ajustementDTO.getCommentaire());
        ajust.setStatut(SalesStatut.CLOSED);

    }

    private void save(List<Ajustement> ajustements) {
        User user = getUser();
        for (Ajustement ajustement : ajustements) {
            StockProduit p = stockProduitRepository.findStockProduitByStorageIdAndProduitId(ajustement.getAjust().getStorage().getId(), ajustement.getProduit().getId()).get();
            TransactionType transactionType = TransactionType.AJUSTEMENT_OUT;
            if (ajustement.getQtyMvt() > 0) {
                transactionType = TransactionType.AJUSTEMENT_IN;
            }
            p.setQtyStock(ajustement.getQtyMvt());
            p.setQtyVirtual(p.getQtyStock());
            stockProduitRepository.save(p);
            createInventory(ajustement,
                transactionType, user);

        }


    }

    public AjustementDTO update(AjustementDTO ajustementDTO) {
        Ajustement ajustement = ajustementRepository.getReferenceById(ajustementDTO.getId());
        int stock = 0;
        Optional<StockProduit> stockProduit = stockProduitRepository.findStockProduitByStorageIdAndProduitId(ajustementDTO.getStorageId(), ajustementDTO.getProduitId());
        if (stockProduit.isPresent()) {
            stock = stockProduit.get().getQtyStock();
        }
        ajustement.setDateMtv(Instant.now());
        ajustement.setQtyMvt(ajustementDTO.getQtyMvt());
        ajustement.setStockBefore(stock);
        ajustement.setStockAfter(stock + ajustementDTO.getQtyMvt());
        return new AjustementDTO(ajustementRepository.save(ajustement));

    }

    private MotifAjustement fromMotifId(Long motifId) {
        if (motifId == null) return null;
        MotifAjustement motifAjustement = new MotifAjustement();
        motifAjustement.setId(motifId);
        return motifAjustement;
    }

    private void createInventory(Ajustement ajustement, TransactionType transactionType, User user) {

        DateDimension dateD = Constants.DateDimension(LocalDate.now());
        Produit p = ajustement.getProduit();
        InventoryTransaction inventoryTransaction = new InventoryTransaction();
        inventoryTransaction.setCreatedAt(Instant.now());
        inventoryTransaction.setProduit(p);
        inventoryTransaction.setUser(user);
        inventoryTransaction.setQuantity(ajustement.getQtyMvt());
        inventoryTransaction.setTransactionType(transactionType);
        inventoryTransaction.setDateDimension(dateD);
        inventoryTransaction.setQuantityBefor(ajustement.getStockBefore());
        inventoryTransaction.setQuantityAfter(ajustement.getStockAfter());
        inventoryTransaction.setRegularUnitPrice(p.getRegularUnitPrice());
        inventoryTransaction.setCostAmount(p.getCostAmount());
        inventoryTransaction.setAjustement(ajustement);
        inventoryTransaction.setMagasin(ajustement.getAjust().getUser().getMagasin());//TODO a optimiser pour la gestion du stock multisite
        inventoryTransactionRepository.save(inventoryTransaction);


    }

    @Transactional
    public List<Ajustement> findAll(Long id) {
        log.debug("Request to get all Ajustements");
        return ajustementRepository.findAllByAjustId(id);
    }

    @Transactional
    public List<Ajustement> findAll() {
        log.debug("Request to get all Ajustements");
        return ajustementRepository.findAll();
    }

    public void delete(Long id) {
        ajustementRepository.deleteById(id);
    }

}

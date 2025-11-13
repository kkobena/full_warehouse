package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.MotifRetourProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.OrderLineId;
import com.kobe.warehouse.domain.ReponseRetourBon;
import com.kobe.warehouse.domain.ReponseRetourBonItem;
import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.RetourBonItem;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.repository.OrderLineRepository;
import com.kobe.warehouse.repository.ReponseRetourBonItemRepository;
import com.kobe.warehouse.repository.ReponseRetourBonRepository;
import com.kobe.warehouse.repository.RetourBonItemRepository;
import com.kobe.warehouse.repository.RetourBonRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.ReponseRetourBonDTO;
import com.kobe.warehouse.service.dto.ReponseRetourBonItemDTO;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.stock.RetourBonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service Implementation for managing {@link RetourBon}.
 */
@Service
@Transactional
public class RetourBonServiceImpl implements RetourBonService {

    private final Logger log = LoggerFactory.getLogger(RetourBonServiceImpl.class);

    private final RetourBonRepository retourBonRepository;
    private final RetourBonItemRepository retourBonItemRepository;
    private final CommandeRepository commandeRepository;
    private final OrderLineRepository orderLineRepository;
    private final UserService userService;
    private final StockProduitRepository stockProduitRepository;
    private final InventoryTransactionService inventoryTransactionService;
    private final ReponseRetourBonRepository reponseRetourBonRepository;
    private final ReponseRetourBonItemRepository reponseRetourBonItemRepository;
    private final LotRepository lotRepository;

    public RetourBonServiceImpl(RetourBonRepository retourBonRepository, RetourBonItemRepository retourBonItemRepository, CommandeRepository commandeRepository, OrderLineRepository orderLineRepository, UserService userService, StockProduitRepository stockProduitRepository, InventoryTransactionService inventoryTransactionService, ReponseRetourBonRepository reponseRetourBonRepository, ReponseRetourBonItemRepository reponseRetourBonItemRepository, LotRepository lotRepository) {
        this.retourBonRepository = retourBonRepository;
        this.retourBonItemRepository = retourBonItemRepository;
        this.commandeRepository = commandeRepository;
        this.orderLineRepository = orderLineRepository;
        this.userService = userService;
        this.stockProduitRepository = stockProduitRepository;
        this.inventoryTransactionService = inventoryTransactionService;
        this.reponseRetourBonRepository = reponseRetourBonRepository;
        this.reponseRetourBonItemRepository = reponseRetourBonItemRepository;
        this.lotRepository = lotRepository;
    }

    @Override
    public RetourBonDTO create(RetourBonDTO retourBonDTO) {
        log.debug("Request to create RetourBon : {}", retourBonDTO);

        RetourBon retourBon = new RetourBon();
        retourBon.setDateMtv(LocalDateTime.now());
        retourBon.setStatut(RetourStatut.VALIDATED);
        retourBon.setCommentaire(retourBonDTO.getCommentaire());
        AppUser currentUser = userService.getUser();
        Magasin magasin = currentUser.getMagasin();
        int magasinId = magasin.getId();
        retourBon.setUser(currentUser);
        CommandeId commandeId = new CommandeId(retourBonDTO.getCommandeId(), retourBonDTO.getCommandeOrderDate());
        Commande commande = commandeRepository.findById(commandeId).orElseThrow(() -> new RuntimeException("Commande not found"));
        retourBon.setCommande(commande);
        retourBon = retourBonRepository.save(retourBon);

        if (retourBonDTO.getRetourBonItems() != null && !retourBonDTO.getRetourBonItems().isEmpty()) {
            RetourBon finalRetourBon = retourBon;
            retourBonDTO.getRetourBonItems().forEach(itemDTO -> createRetourBonItem(itemDTO, finalRetourBon, magasinId));
        }

        return new RetourBonDTO(retourBon);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<RetourBonDTO> findAll(Pageable pageable) {
        log.debug("Request to get all RetourBons");
        return retourBonRepository.findAllByOrderByDateMtvDesc(pageable).map(RetourBonDTO::new);
    }


    @Override
    @Transactional(readOnly = true)
    public List<RetourBonDTO> findAllByCommande(Integer commandeId, LocalDate orderDate) {
        log.debug("Request to get all RetourBons by commande : {}, {}", commandeId, orderDate);
        return retourBonRepository.findAllByCommandeId(commandeId).stream().map(RetourBonDTO::new).toList();
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<RetourBonDTO> findOne(Integer id) {
        log.debug("Request to get RetourBon : {}", id);
        return retourBonRepository.findById(id).map(RetourBonDTO::new);
    }


    @Override
    public ReponseRetourBonDTO createSupplierResponse(ReponseRetourBonDTO reponseRetourBonDTO) {
        log.debug("Request to create supplier response for RetourBon : {}", reponseRetourBonDTO.getRetourBonId());

        // Get the current user
        AppUser currentUser = userService.getUser();

        // Find the RetourBon
        RetourBon retourBon = retourBonRepository.findById(reponseRetourBonDTO.getRetourBonId()).orElseThrow(() -> new GenericError("RetourBon not found"));

        // Validate that the retour bon is in VALIDATED status
        if (retourBon.getStatut() != RetourStatut.VALIDATED) {
            throw new GenericError("Ce retour est déjà traité ");
        }

        // Create the ReponseRetourBon
        ReponseRetourBon reponseRetourBon = new ReponseRetourBon();
        reponseRetourBon.setDateMtv(LocalDateTime.now());
        reponseRetourBon.setUser(currentUser);
        reponseRetourBon.setRetourBon(retourBon);
        reponseRetourBon = reponseRetourBonRepository.save(reponseRetourBon);
        boolean allItemsAccepted = true;

        // Create the response items
        if (reponseRetourBonDTO.getReponseRetourBonItems() != null && !reponseRetourBonDTO.getReponseRetourBonItems().isEmpty()) {
            ReponseRetourBon finalReponseRetourBon = reponseRetourBon;
            for (ReponseRetourBonItemDTO itemDTO : reponseRetourBonDTO.getReponseRetourBonItems()) {
                var allAccepted = createReponseRetourBonItem(itemDTO, finalReponseRetourBon);
                if (!allAccepted) {
                    allItemsAccepted = false;
                }
            }
        }
        if (allItemsAccepted) {
            retourBon.setStatut(RetourStatut.CLOSED);
        }
        retourBonRepository.save(retourBon);

        return new ReponseRetourBonDTO(reponseRetourBon);
    }

    private boolean createReponseRetourBonItem(ReponseRetourBonItemDTO itemDTO, ReponseRetourBon reponseRetourBon) {
        // Find the RetourBonItem
        RetourBonItem retourBonItem = retourBonItemRepository.findById(itemDTO.getRetourBonItemId()).orElseThrow(() -> new GenericError("RetourBonItem not found"));
        if (retourBonItem.getAcceptedQty() != null && retourBonItem.getAcceptedQty().compareTo(retourBonItem.getQtyMvt()) == 0) {
            return true;
        }
        // Validate quantity
        if (itemDTO.getQtyMvt() < 0) {
            throw new GenericError("Accepted quantity cannot be negative");
        }

        if (itemDTO.getQtyMvt() > retourBonItem.getQtyMvt()) {
            throw new GenericError("Accepted quantity cannot exceed requested quantity");
        }

        // Create the response item

        ReponseRetourBonItem responseItem = new ReponseRetourBonItem();
        responseItem.setDateMtv(LocalDateTime.now());
        responseItem.setReponseRetourBon(reponseRetourBon);
        responseItem.setRetourBonItem(retourBonItem);
        responseItem.setQtyMvt(itemDTO.getQtyMvt());
        responseItem.setPrixAchat(retourBonItem.getPrixAchat());
        reponseRetourBonItemRepository.save(responseItem);
        retourBonItem.setAcceptedQty(Objects.requireNonNullElse(retourBonItem.getAcceptedQty(),0) + itemDTO.getQtyMvt());
        retourBonItemRepository.save(retourBonItem);
        return itemDTO.getQtyMvt().compareTo(retourBonItem.getQtyMvt()) == 0;

    }

    private void createRetourBonItem(RetourBonItemDTO itemDTO, RetourBon retourBon, int magasinId) {
        List<StockProduit> stockProduits = stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(magasinId, itemDTO.getProduitId());
        StockProduit stockProduit = stockProduits.stream().filter(st -> st.getStorage().getStorageType() == StorageType.PRINCIPAL).findFirst().orElse(stockProduits.getFirst());
        int initStock = stockProduits.stream().mapToInt(StockProduit::getTotalStockQuantity).sum();
        if (itemDTO.getQtyMvt() > initStock) {
            throw new GenericError("Stock insuffisant pour le produit: " + itemDTO.getProduitCip());
        }
        int finalAfterStock = initStock - itemDTO.getQtyMvt();
        RetourBonItem item = new RetourBonItem();
        item.setDateMtv(LocalDateTime.now());
        item.setRetourBon(retourBon);
        item.setQtyMvt(itemDTO.getQtyMvt());
        item.setInitStock(initStock);
        item.setAfterStock(finalAfterStock);
        var motifRetourProduit = new MotifRetourProduit();
        motifRetourProduit.setId(itemDTO.getMotifRetourId());
        item.setMotifRetour(motifRetourProduit);


        // Set order line
        if (itemDTO.getOrderLineId() != null && itemDTO.getOrderLineOrderDate() != null) {
            OrderLineId orderLineId = new OrderLineId(itemDTO.getOrderLineId(), itemDTO.getOrderLineOrderDate());
            OrderLine orderLine = orderLineRepository.findById(orderLineId).orElseThrow(() -> new RuntimeException("ligne de commande introuvable"));
            item.setOrderLine(orderLine);
            item.setPrixAchat(orderLine.getOrderCostAmount());
        }
        if (itemDTO.getLotId() != null) {
            var lot = lotRepository.findById(itemDTO.getLotId()).orElseThrow(() -> new GenericError("Le lot n'existe pas"));
            item.setLot(lot);

            if (itemDTO.getQtyMvt() > lot.getQuantity()) {
                throw new GenericError("Quantité insuffisante dans le lot: " + lot.getNumLot());
            }
            lot.setQuantity(lot.getQuantity() - itemDTO.getQtyMvt());
            lotRepository.save(lot);
        }


        item = retourBonItemRepository.save(item);
        stockProduit.setQtyStock(finalAfterStock);
        stockProduitRepository.save(stockProduit);
        inventoryTransactionService.save(item);

    }
}

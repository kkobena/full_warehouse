package com.kobe.warehouse.service;

import com.kobe.warehouse.config.Constants;
import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class InventaireService {
    private final Logger LOG = LoggerFactory.getLogger(InventaireService.class);
    @Autowired
    private ProduitRepository produitRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private InventoryTransactionService inventoryTransactionService;
    @Autowired
    private StoreInventoryRepository storeInventoryRepository;
    @Autowired
    private StoreInventoryLineRepository storeInventoryLineRepository;
    private static final Comparator<StoreInventoryDTO> COMPARATOR = Comparator.comparing(StoreInventoryDTO::getUpdatedAt,Comparator.reverseOrder());
    private static final Comparator<StoreInventoryLineDTO> COMPARATOR_LINE = Comparator.comparing(
        StoreInventoryLineDTO::getProduitLibelle);

    public void init() throws Exception {
        long inventoryValueCostBegin = 0, inventoryAmountBegin = 0;
        StoreInventory storeInventory = new StoreInventory();
        storeInventory.setCreatedAt(Instant.now());
        storeInventory.setUpdatedAt(storeInventory.getCreatedAt());
        storeInventory.setUser(userService.getUser());
        storeInventory.setInventoryAmountAfter(0l);
        storeInventory.setInventoryValueCostAfter(0l);
        storeInventory.setDateDimension(Constants.DateDimension(LocalDate.now()));
        List<StoreInventoryLine> storeInventoryLines = intitLines(storeInventory);
        for (StoreInventoryLine line : storeInventoryLines) {
            inventoryValueCostBegin += (line.getInventoryValueCost() * line.getQuantityInit());
            inventoryAmountBegin += (line.getInventoryValueLatestSellingPrice() * line.getQuantityInit());
        }
        storeInventory.setInventoryAmountBegin(inventoryAmountBegin);
        storeInventory.setInventoryValueCostBegin(inventoryValueCostBegin);
        storeInventoryRepository.save(storeInventory);
        storeInventoryLineRepository.saveAll(storeInventoryLines);
    }

    public void close(Long id) throws Exception {
        long inventoryValueCostAfter = 0, inventoryAmountAfter = 0;
        StoreInventory storeInventory = storeInventoryRepository.getOne(id);
        DateDimension dateDimension = storeInventory.getDateDimension();
        storeInventory.setStatut(SalesStatut.CLOSE);
        storeInventory.setUpdatedAt(Instant.now());
        List<StoreInventoryLine> storeInventoryLines = storeInventoryLineRepository.findAllByStoreInventoryId(id);
        for (StoreInventoryLine line : storeInventoryLines) {
            inventoryValueCostAfter += (line.getInventoryValueCost() * line.getQuantityOnHand());
            inventoryAmountAfter += (line.getInventoryValueLatestSellingPrice() * line.getQuantityOnHand());
            inventoryTransactionService.buildInventoryTransaction(line, dateDimension, storeInventory.getUpdatedAt(), userService.getUser());
            Produit produit = line.getProduit();
            produit.setQuantity(line.getUpdated() ? line.getQuantityOnHand() : line.getQuantityInit());
            produitRepository.save(produit);
        }
        storeInventory.setInventoryValueCostAfter(inventoryValueCostAfter);
        storeInventory.setInventoryAmountAfter(inventoryAmountAfter);
        storeInventoryRepository.save(storeInventory);
        storeInventoryLineRepository.saveAll(storeInventoryLines);
    }

    private StoreInventoryLine createStoreInventoryLine(Produit produit, int quantitySold, StoreInventory storeInventory) {
        StoreInventoryLine storeInventoryLine = new StoreInventoryLine();
        storeInventoryLine.setProduit(produit);
        storeInventoryLine.setQuantitySold(quantitySold);
        storeInventoryLine.setStoreInventory(storeInventory);
        storeInventoryLine.setQuantityInit(produit.getQuantity());
        storeInventoryLine.setQuantityOnHand(0);
        storeInventoryLine.setUpdated(false);
        storeInventoryLine.setInventoryValueCost(produit.getCostAmount());
        storeInventoryLine.setInventoryValueLatestSellingPrice(produit.getRegularUnitPrice());
        return storeInventoryLine;
    }

    private List<StoreInventoryLine> intitLines(StoreInventory storeInventory) {
        List<StoreInventoryLine> storeInventoryLines = new ArrayList<>();
        List<Produit> produits = produitRepository.findAllByParentIdIsNull();
        for (Produit produit : produits) {
            long quantitySold = inventoryTransactionService.quantitySoldIncludeChildQuantity(produit.getId());
            storeInventoryLines.add(createStoreInventoryLine(produit, (int) quantitySold, storeInventory));
            if (!produit.getProduits().isEmpty()) {
                Produit detail = produit.getProduits().get(0);
                quantitySold = inventoryTransactionService.quantitySold(detail.getId());
                storeInventoryLines.add(createStoreInventoryLine(detail, (int) quantitySold, storeInventory));
            }
        }
        return storeInventoryLines;
    }

    @Transactional(readOnly = true)
    public List<StoreInventoryLineDTO> storeInventoryList(Long storeInventoryId) {
        return storeInventoryLineRepository.findAllByStoreInventoryId(storeInventoryId).stream().map(StoreInventoryLineDTO::new)
            .sorted(COMPARATOR_LINE).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreInventoryDTO> storeInventoryList() {
        return storeInventoryRepository.findAll().stream()
            .map(e -> new StoreInventoryDTO(e,
                storeInventoryList(e.getId()))).sorted(COMPARATOR).collect(Collectors.toList());
    }

    public void remove(Long id) {
        storeInventoryRepository.deleteById(id);
    }

    public void updateQuantityOnHand(StoreInventoryLineDTO storeInventoryLineDTO) {
        StoreInventoryLine storeInventoryLine = storeInventoryLineRepository.getOne(storeInventoryLineDTO.getId());
        storeInventoryLine.setQuantityOnHand(storeInventoryLineDTO.getQuantityOnHand());
        storeInventoryLine.setUpdated(true);
        storeInventoryLineRepository.save(storeInventoryLine);

    }

    @Transactional(readOnly = true)
    public Optional<StoreInventoryDTO> getStoreInventory(Long id) {
        return storeInventoryRepository.findById(id).
            map(e -> new StoreInventoryDTO(
                e, storeInventoryList(e.getId())));

    }


}

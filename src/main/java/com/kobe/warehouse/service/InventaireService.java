package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.enumeration.InventoryStatut;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.StoreInventoryLineRepository;
import com.kobe.warehouse.repository.StoreInventoryRepository;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InventaireService {
    private static final Comparator<StoreInventoryDTO> COMPARATOR = Comparator.comparing(StoreInventoryDTO::getUpdatedAt, Comparator.reverseOrder());
    private static final Comparator<StoreInventoryLineDTO> COMPARATOR_LINE = Comparator.comparing(
        StoreInventoryLineDTO::getProduitLibelle);
    private final Logger LOG = LoggerFactory.getLogger(InventaireService.class);

    private final ProduitRepository produitRepository;

    private final UserService userService;

    private final InventoryTransactionService inventoryTransactionService;

    private final StoreInventoryRepository storeInventoryRepository;

    private final StoreInventoryLineRepository storeInventoryLineRepository;

    public InventaireService(ProduitRepository produitRepository, UserService userService, InventoryTransactionService inventoryTransactionService, StoreInventoryRepository storeInventoryRepository, StoreInventoryLineRepository storeInventoryLineRepository) {
        this.produitRepository = produitRepository;
        this.userService = userService;
        this.inventoryTransactionService = inventoryTransactionService;
        this.storeInventoryRepository = storeInventoryRepository;
        this.storeInventoryLineRepository = storeInventoryLineRepository;
    }

    public void init() throws Exception {
        long inventoryValueCostBegin = 0, inventoryAmountBegin = 0;
        StoreInventory storeInventory = new StoreInventory();
        storeInventory.setCreatedAt(LocalDateTime.now());
        storeInventory.setUpdatedAt(storeInventory.getCreatedAt());
        storeInventory.setUser(userService.getUser());
        storeInventory.setInventoryAmountAfter(0L);
        storeInventory.setInventoryValueCostAfter(0L);

        List<StoreInventoryLine> storeInventoryLines = intitLines(storeInventory);
        for (StoreInventoryLine line : storeInventoryLines) {
            inventoryValueCostBegin += ((long) line.getInventoryValueCost() * line.getQuantityInit());
            inventoryAmountBegin += ((long) line.getInventoryValueLatestSellingPrice() * line.getQuantityInit());
        }
        storeInventory.setInventoryAmountBegin(inventoryAmountBegin);
        storeInventory.setInventoryValueCostBegin(inventoryValueCostBegin);
        storeInventoryRepository.save(storeInventory);
        storeInventoryLineRepository.saveAll(storeInventoryLines);
    }

    public void close(Long id) throws Exception {
        long inventoryValueCostAfter = 0, inventoryAmountAfter = 0;
        StoreInventory storeInventory = storeInventoryRepository.getReferenceById(id);

        storeInventory.setStatut(InventoryStatut.CLOSED);
        storeInventory.setUpdatedAt(LocalDateTime.now());
        List<StoreInventoryLine> storeInventoryLines = storeInventoryLineRepository.findAllByStoreInventoryId(id);
        for (StoreInventoryLine line : storeInventoryLines) {
            inventoryValueCostAfter += ((long) line.getInventoryValueCost() * line.getQuantityOnHand());
            inventoryAmountAfter += ((long) line.getInventoryValueLatestSellingPrice() * line.getQuantityOnHand());
            inventoryTransactionService.buildInventoryTransaction(line, storeInventory.getUpdatedAt(), userService.getUser());
            Produit produit = line.getProduit();
            // produit.setQuantity(line.getUpdated() ? line.getQuantityOnHand() : line.getQuantityInit());
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
        //  storeInventoryLine.setQuantityInit(produit.getQuantity());
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
        StoreInventoryLine storeInventoryLine = storeInventoryLineRepository.getReferenceById(storeInventoryLineDTO.getId());
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

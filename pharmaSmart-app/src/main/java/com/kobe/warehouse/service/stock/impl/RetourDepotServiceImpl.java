package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourDepot;
import com.kobe.warehouse.domain.RetourDepotItem;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RetourDepotItemRepository;
import com.kobe.warehouse.repository.RetourDepotRepository;
import com.kobe.warehouse.repository.StockProduitRepository;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.dto.RetourDepotDTO;
import com.kobe.warehouse.service.dto.RetourDepotItemDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.mvt_produit.service.InventoryTransactionService;
import com.kobe.warehouse.service.stock.RetourDepotService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link RetourDepot}.
 */
@Service
@Transactional
public class RetourDepotServiceImpl implements RetourDepotService {

    private final Logger log = LoggerFactory.getLogger(RetourDepotServiceImpl.class);

    private final RetourDepotRepository retourDepotRepository;
    private final RetourDepotItemRepository retourDepotItemRepository;
    private final MagasinRepository magasinRepository;
    private final ProduitRepository produitRepository;
    private final UserService userService;
    private final StockProduitRepository stockProduitRepository;
    private final InventoryTransactionService inventoryTransactionService;

    public RetourDepotServiceImpl(
        RetourDepotRepository retourDepotRepository,
        RetourDepotItemRepository retourDepotItemRepository,
        MagasinRepository magasinRepository,
        ProduitRepository produitRepository,
        UserService userService,
        StockProduitRepository stockProduitRepository,
        InventoryTransactionService inventoryTransactionService
    ) {
        this.retourDepotRepository = retourDepotRepository;
        this.retourDepotItemRepository = retourDepotItemRepository;
        this.magasinRepository = magasinRepository;
        this.produitRepository = produitRepository;
        this.userService = userService;
        this.stockProduitRepository = stockProduitRepository;
        this.inventoryTransactionService = inventoryTransactionService;
    }

    @Override
    public RetourDepotDTO create(RetourDepotDTO retourDepotDTO) {
        log.debug("Request to create RetourDepot : {}", retourDepotDTO);

        RetourDepot retourDepot = new RetourDepot();
        retourDepot.setDateMtv(LocalDateTime.now());
        AppUser currentUser = userService.getUser();
        retourDepot.setUser(currentUser);

        // Find Depot (Magasin)
        Magasin depot = magasinRepository.findById(retourDepotDTO.getDepotId()).orElseThrow(() -> new GenericError("Dépôt non trouvé"));
        retourDepot.setDepot(depot);

        retourDepot = retourDepotRepository.save(retourDepot);

        if (retourDepotDTO.getRetourDepotItems() != null && !retourDepotDTO.getRetourDepotItems().isEmpty()) {
            RetourDepot finalRetourDepot = retourDepot;
            retourDepotDTO.getRetourDepotItems().forEach(itemDTO -> createRetourDepotItem(itemDTO, finalRetourDepot, depot.getId()));
        }

        return toDTO(retourDepot);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RetourDepotDTO> findAllByDateRange(Integer depotId, LocalDate dtStart, LocalDate dtEnd, Pageable pageable) {
        log.debug("Request to get all RetourDepots by date range : {} - {}", dtStart, dtEnd);
        LocalDateTime start = dtStart.atStartOfDay();
        LocalDateTime end = dtEnd.atTime(23, 59, 59);
        return retourDepotRepository
            .findAll(retourDepotRepository.filterByDateRangeAndDepot(depotId, start, end), pageable)
            .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RetourDepotDTO> findOne(Integer id) {
        log.debug("Request to get RetourDepot : {}", id);
        return retourDepotRepository.findOneWithItems(id).map(this::toDTO);
    }

    private void createRetourDepotItem(RetourDepotItemDTO itemDTO, RetourDepot retourDepot, Integer depotId) {
        // Find the product
        Produit produit = produitRepository.findById(itemDTO.getProduitId()).orElseThrow(() -> new GenericError("Produit non trouvé"));
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        // Get stock for the depot
        List<StockProduit> stockProduits = stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(
            depotId,
            itemDTO.getProduitId()
        );

        StockProduit stockProduit = stockProduits
            .stream()
            .filter(st -> st.getStorage().getStorageType() == StorageType.PRINCIPAL)
            .findFirst()
            .orElse(stockProduits.isEmpty() ? null : stockProduits.getFirst());

        if (stockProduit == null) {
            throw new GenericError("Stock non trouvé pour le produit: " + itemDTO.getProduitCip());
        }
        Magasin officine = retourDepot.getUser().getMagasin();
        List<StockProduit> officineStocks = stockProduitRepository.findStockProduitByStorageMagasinIdAndProduitId(
            officine.getId(),
            itemDTO.getProduitId()
        );
        int officineInitStock = 0;
        StockProduit officineStock = null;
        for (StockProduit sp : officineStocks) {
            if (sp.getStorage().getStorageType() == StorageType.PRINCIPAL) {
                officineStock = sp;
            }
            officineInitStock += sp.getTotalStockQuantity();
        }
        int officineAfterStock = officineInitStock + itemDTO.getQtyMvt();
        int initStock = stockProduit.getTotalStockQuantity();

        // Create RetourDepotItem
        RetourDepotItem item = new RetourDepotItem();
        item.setRetourDepot(retourDepot);
        item.setProduit(produit);
        item.setQtyMvt(itemDTO.getQtyMvt());
        item.setRegularUnitPrice(itemDTO.getRegularUnitPrice());
        item.setInitStock(initStock);
        item.setAfterStock(initStock - itemDTO.getQtyMvt());
        item.setOfficineInitStock(officineInitStock);
        item.setOfficineFinalStock(officineAfterStock);
        item.setRegularUnitPrice(fournisseurProduit.getPrixUni());
        item.setPrixAchat(fournisseurProduit.getPrixAchat());

        // Update stock in depot
        stockProduit.setQtyStock(stockProduit.getQtyStock() - itemDTO.getQtyMvt());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(stockProduit);
        // Update stock in officine
        officineStock.setQtyStock(officineStock.getQtyStock() + itemDTO.getQtyMvt());
        officineStock.setUpdatedAt(LocalDateTime.now());
        stockProduitRepository.save(officineStock);

        retourDepotItemRepository.save(item);
        inventoryTransactionService.save(item);
    }

    private RetourDepotDTO toDTO(RetourDepot retourDepot) {
        RetourDepotDTO dto = new RetourDepotDTO();
        dto.setId(retourDepot.getId());
        dto.setDateMtv(retourDepot.getDateMtv());
        AppUser user = retourDepot.getUser();
        dto.setUserFullName(user.getFirstName() + " " + user.getLastName());
        Magasin depot = retourDepot.getDepot();
        dto.setDepotId(depot.getId());
        dto.setDepotName(depot.getFullName());

        if (retourDepot.getRetourDepotItems() != null) {
            List<RetourDepotItemDTO> itemDTOs = retourDepot.getRetourDepotItems().stream().map(this::toItemDTO).toList();
            dto.setRetourDepotItems(itemDTOs);
        }

        return dto;
    }

    private RetourDepotItemDTO toItemDTO(RetourDepotItem item) {
        RetourDepotItemDTO dto = new RetourDepotItemDTO();
        dto.setId(item.getId());
        dto.setQtyMvt(item.getQtyMvt());
        dto.setRegularUnitPrice(item.getRegularUnitPrice());
        dto.setInitStock(item.getInitStock());
        dto.setAfterStock(item.getAfterStock());
        Produit produit = item.getProduit();
        FournisseurProduit fournisseurProduit = produit.getFournisseurProduitPrincipal();
        dto.setProduitId(produit.getId());
        dto.setProduitLibelle(produit.getLibelle());
        dto.setProduitCip(fournisseurProduit.getCodeCip());

        return dto;
    }
}

package com.kobe.warehouse.service.mvt_produit.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.criteria.InventoryTransactionSpec;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import com.kobe.warehouse.service.dto.filter.InventoryTransactionFilterDTO;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingSum;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import com.kobe.warehouse.service.id_generator.MvtProduitIdGeneratorService;
import com.kobe.warehouse.service.mvt_produit.builder.InventoryTransactionBuilder;
import com.kobe.warehouse.service.sale.dto.VenteDepotTransactionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.nonNull;

@Service
@Transactional
public class InventoryTransactionServiceIml implements InventoryTransactionService {
    private final Logger log = LoggerFactory.getLogger(InventoryTransactionServiceIml.class);
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryTransactionSpec inventoryTransactionSpec;
    private final MvtProduitIdGeneratorService mvtProduitIdGeneratorService;
    private final JsonMapper objectMapper;
    private final StorageService storageService;

    public InventoryTransactionServiceIml(
        InventoryTransactionRepository inventoryTransactionRepository,
        InventoryTransactionSpec inventoryTransactionSpec,
        MvtProduitIdGeneratorService mvtProduitIdGeneratorService, JsonMapper objectMapper, StorageService storageService
    ) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryTransactionSpec = inventoryTransactionSpec;
        this.mvtProduitIdGeneratorService = mvtProduitIdGeneratorService;

        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    @Override
    public void save(Object entity) {
        inventoryTransactionRepository.save(new InventoryTransactionBuilder(entity).build().setId(mvtProduitIdGeneratorService.nextId()));
    }

    @Transactional(readOnly = true)
    public long quantitySold(Integer produitId) {
        Long aLong = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produitId);
        return (aLong != null ? aLong : 0);
    }

    @Transactional(readOnly = true)
    public Optional<InventoryTransaction> findById(Long id) {
        return inventoryTransactionRepository.findInventoryTransactionById(id);
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransactionDTO> getAllInventoryTransactions(
        Pageable pageable,
        Integer produitId,
        String startDate,
        String endDate,
        Integer type
    ) {
        this.inventoryTransactionSpec.setInventoryTransactionFilter(
            new InventoryTransactionFilterDTO().setEndDate(endDate).setProduitId(produitId).setStartDate(startDate).setType(type)
        );

        return inventoryTransactionRepository.findAll(this.inventoryTransactionSpec, pageable).map(InventoryTransactionDTO::new);
    }

    @Override
    public LocalDateTime fetchLastDateByTypeAndProduitId(MouvementProduit type, Integer produitId) {
        return inventoryTransactionRepository
            .fetchLastDateByTypeAndProduitId(type, produitId)
            .map(LastDateProjection::getUpdatedAt)
            .orElse(null);
    }

    @Override
    public List<ProduitAuditingState> fetchProduitDailyTransaction(ProduitAuditingParam produitAuditingParam) {
        var startDate = nonNull(produitAuditingParam.fromDate()) ? produitAuditingParam.fromDate() : LocalDate.now();
        var endDate = nonNull(produitAuditingParam.toDate()) ? produitAuditingParam.toDate() : LocalDate.now();
        Integer magasinId = produitAuditingParam.magasinId();
        magasinId = magasinId == null ? storageService.getConnectedUserMagasin().getId() : magasinId;
        return fetchMouvementProduit(produitAuditingParam.produitId(), magasinId, startDate, endDate);

    }

    private List<ProduitAuditingState> fetchMouvementProduit(Integer produitId, Integer magasinId, LocalDate startDate, LocalDate endDate) {
        List<ProduitAuditingState> produitAuditingStates = new ArrayList<>();
        record ProductMouvement(LocalDate mvtDate, int initStock, int afterStock,
                                Map<MouvementProduit, Integer> mouvements) {
        }

        try {

            List<ProductMouvement> productMouvements = objectMapper.readValue(inventoryTransactionRepository.fetchMouvementProduit(produitId, magasinId, startDate, endDate), new TypeReference<>() {
            });
            productMouvements.forEach(productMouvement -> {
                ProduitAuditingState produitAuditingState = new ProduitAuditingState();
                produitAuditingState.setMvtDate(productMouvement.mvtDate());
                produitAuditingState.setInitStock(productMouvement.initStock());
                produitAuditingState.setAfterStock(productMouvement.afterStock());
                Map<MouvementProduit, Integer> mvts = productMouvement.mouvements();
                if (!CollectionUtils.isEmpty(mvts)) {
                    mvts.forEach((mouvementProduit, quantity) -> {
                        switch (mouvementProduit) {
                            case SALE -> produitAuditingState.setSaleQuantity(quantity);
                            case COMMANDE -> produitAuditingState.setDeleveryQuantity(quantity);
                            case RETOUR_FOURNISSEUR -> produitAuditingState.setRetourFournisseurQuantity(quantity);
                            case RETRAIT_PERIME -> produitAuditingState.setPerimeQuantity(quantity);
                            case AJUSTEMENT_IN -> produitAuditingState.setAjustementPositifQuantity(quantity);
                            case AJUSTEMENT_OUT -> produitAuditingState.setAjustementNegatifQuantity(quantity);
                            case DECONDTION_IN -> produitAuditingState.setDeconPositifQuantity(quantity);
                            case DECONDTION_OUT -> produitAuditingState.setDeconNegatifQuantity(quantity);
                            case CANCEL_SALE -> produitAuditingState.setCanceledQuantity(quantity);
                            case RETOUR_DEPOT -> produitAuditingState.setRetourDepot(quantity);
                            case INVENTAIRE -> {
                                produitAuditingState.setStoreInventoryQuantity(quantity);
                                produitAuditingState.setInventoryGap(productMouvement.initStock() - quantity);

                            }

                        }
                    });


                }

                produitAuditingStates.add(produitAuditingState);
            });


        } catch (Exception e) {
            log.info(e.getMessage());

        }
        return produitAuditingStates;
    }

    @Override
    public List<ProduitAuditingSum> fetchProduitDailyTransactionSum(ProduitAuditingParam produitAuditingParam) {
        var startDate = nonNull(produitAuditingParam.fromDate()) ? produitAuditingParam.fromDate() : null;
        var endDate = nonNull(produitAuditingParam.toDate()) ? produitAuditingParam.toDate() : null;
        return this.inventoryTransactionRepository.fetchProduitDailyTransactionSum(
            inventoryTransactionRepository.combineSpecifications(produitAuditingParam.produitId(), startDate, endDate)
        );
    }

    @Override
    public void saveAll(List<OrderLine> entities) {
        if (CollectionUtils.isEmpty(entities)) {
            entities.forEach(this::save);
        }
    }

    @Override
    public void saveVenteDepotExtensionInventoryTransactions(Magasin depot, List<VenteDepotTransactionRecord> venteDepotTransactionRecords) {
        if (CollectionUtils.isEmpty(venteDepotTransactionRecords)) {
            return;
        }
        venteDepotTransactionRecords.forEach(record -> {
            SalesLine salesLine = record.salesLine();
            InventoryTransaction inventoryTransaction = new InventoryTransaction()
                .setCreatedAt(salesLine.getUpdatedAt())
                .setProduit(salesLine.getProduit())
                .setMouvementType(MouvementProduit.ENTREE_STOCK)
                .setQuantity(salesLine.getQuantityRequested())
                .setQuantityBefor(record.quantityBefore())
                .setQuantityAfter(record.quantityAfter())
                .setCostAmount(salesLine.getCostAmount())
                .setEntityId(salesLine.getId().getId())
                .setUser(salesLine.getSales().getUser())
                .setMagasin(depot)
                .setRegularUnitPrice(salesLine.getRegularUnitPrice());
            inventoryTransaction.setId(mvtProduitIdGeneratorService.nextId());
            inventoryTransactionRepository.save(inventoryTransaction);
        });

    }
}

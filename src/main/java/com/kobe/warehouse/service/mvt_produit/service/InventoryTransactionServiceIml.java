package com.kobe.warehouse.service.mvt_produit.service;

import com.kobe.warehouse.domain.InventoryTransaction;
import com.kobe.warehouse.domain.enumeration.MouvementProduit;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.InventoryTransactionRepository;
import com.kobe.warehouse.service.criteria.InventoryTransactionSpec;
import com.kobe.warehouse.service.dto.InventoryTransactionDTO;
import com.kobe.warehouse.service.dto.filter.InventoryTransactionFilterDTO;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingParam;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingState;
import com.kobe.warehouse.service.dto.produit.ProduitAuditingSum;
import com.kobe.warehouse.service.dto.projection.LastDateProjection;
import com.kobe.warehouse.service.mvt_produit.builder.InventoryTransactionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;

@Service
@Transactional
public class InventoryTransactionServiceIml implements InventoryTransactionService {
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final InventoryTransactionSpec inventoryTransactionSpec;


    public InventoryTransactionServiceIml(InventoryTransactionRepository inventoryTransactionRepository, InventoryTransactionSpec inventoryTransactionSpec) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.inventoryTransactionSpec = inventoryTransactionSpec;
    }


    @Override
    public void save(Object entity) {
        inventoryTransactionRepository.save(new InventoryTransactionBuilder(entity).build());

    }

    @Transactional(readOnly = true)
    public long quantitySold(Long produitId) {
        Long aLong = inventoryTransactionRepository.quantitySold(TransactionType.SALE, produitId);
        return (aLong != null ? aLong : 0);
    }

    @Transactional(readOnly = true)
    public Optional<InventoryTransaction> findById(Long id) {
        return inventoryTransactionRepository.findById(id);
    }


    @Transactional(readOnly = true)
    public Page<InventoryTransactionDTO> getAllInventoryTransactions(
        Pageable pageable,
        Long produitId,
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
    public LocalDateTime fetchLastDateByTypeAndProduitId(MouvementProduit type, Long produitId) {
        return inventoryTransactionRepository.fetchLastDateByTypeAndProduitId(type,
            produitId).map(
            LastDateProjection::getUpdatedAt).orElse(null);
    }

    @Override
    public Page<ProduitAuditingState> fetchProduitDailyTransaction(ProduitAuditingParam produitAuditingParam, Pageable pageable) {

        var startDate = nonNull(produitAuditingParam.fromDate()) ? produitAuditingParam.fromDate().atStartOfDay() : null;
        var endDate = nonNull(produitAuditingParam.toDate()) ? produitAuditingParam.toDate().atTime(23, 59, 59) : null;
        return this.inventoryTransactionRepository.fetchProduitDailyTransaction(inventoryTransactionRepository.combineSpecifications(produitAuditingParam.produitId(), startDate, endDate), pageable);
    }

    @Override
    public List<ProduitAuditingSum> fetchProduitDailyTransactionSum(ProduitAuditingParam produitAuditingParam) {
        var startDate = nonNull(produitAuditingParam.fromDate()) ? produitAuditingParam.fromDate().atStartOfDay() : null;
        var endDate = nonNull(produitAuditingParam.toDate()) ? produitAuditingParam.toDate().atTime(23, 59, 59) : null;
        return this.inventoryTransactionRepository.fetchProduitDailyTransactionSum(inventoryTransactionRepository.combineSpecifications(produitAuditingParam.produitId(), startDate, endDate));
    }
}

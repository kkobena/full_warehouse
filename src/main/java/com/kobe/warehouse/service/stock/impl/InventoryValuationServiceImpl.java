package com.kobe.warehouse.service.stock.impl;

import com.kobe.warehouse.service.dto.builder.StoreInventoryLineFilterBuilder;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryByGroupRecord;
import com.kobe.warehouse.service.dto.records.StoreInventorySummaryRecord;
import com.kobe.warehouse.service.stock.InventoryValuationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryValuationServiceImpl implements InventoryValuationService {

    private final EntityManager em;

    public InventoryValuationServiceImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    public StoreInventorySummaryRecord getGlobalSummary(Long inventoryId) {
        Tuple result = (Tuple) em.createNativeQuery(
                StoreInventoryLineFilterBuilder.SUMMARY_SQL, Tuple.class)
            .setParameter(1, inventoryId)
            .getSingleResult();
        return StoreInventoryLineFilterBuilder.buildSammary(result);
    }

    @Override
    public List<StoreInventorySummaryByGroupRecord> getSummaryByGroup(
        Long inventoryId, String groupBy
    ) {
        String sql = switch (groupBy == null ? "" : groupBy.toUpperCase()) {
            case "FAMILLE" -> StoreInventoryLineFilterBuilder.VALUATION_BY_FAMILLE_SQL;
            case "RAYON"   -> StoreInventoryLineFilterBuilder.VALUATION_BY_RAYON_SQL;
            default        -> StoreInventoryLineFilterBuilder.VALUATION_BY_STORAGE_SQL;
        };

        @SuppressWarnings("unchecked")
        List<Tuple> rows = em.createNativeQuery(sql, Tuple.class)
            .setParameter(1, inventoryId)
            .getResultList();

        return rows.stream()
            .map(StoreInventoryLineFilterBuilder::buildGroupRow)
            .toList();
    }
}
